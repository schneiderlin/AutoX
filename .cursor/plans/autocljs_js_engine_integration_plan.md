---
name: AutoCLJS JavaScript Engine Integration Plan
overview: "Plan to integrate JavaScript engine (Rhino) and expose automator API to JavaScript, enabling compiled ClojureScript to call automation functions."
todos:
  - id: js_engine_integration
    content: "Integrate Rhino JavaScript engine into ClojureScriptEngine to actually execute JavaScript code"
    status: pending
  - id: expose_console_api
    content: "Expose Console API (console.log, console.error, etc.) to JavaScript runtime"
    status: pending
    dependencies:
      - js_engine_integration
  - id: expose_automator_api
    content: "Expose automator API (auto.click, auto.longClick, etc.) to JavaScript runtime"
    status: pending
    dependencies:
      - js_engine_integration
  - id: update_test_activity
    content: "Update TestActivity to execute JavaScript code instead of calling Kotlin APIs directly"
    status: pending
    dependencies:
      - expose_console_api
      - expose_automator_api
  - id: test_js_execution
    content: "Test executing JavaScript code that calls console.log and auto.click()"
    status: pending
    dependencies:
      - update_test_activity
---

# AutoCLJS JavaScript Engine Integration Plan

## Problem Statement

Currently:
1. **ClojureScriptEngine** is just a placeholder that logs - it doesn't actually execute JavaScript
2. **TestActivity** calls Kotlin APIs directly (`runtime.getAutomator().click()`) instead of executing JavaScript
3. The automator API is not exposed to JavaScript runtime
4. No JavaScript code is being executed

**Goal**: Enable compiled ClojureScript (JavaScript) to actually execute and call automation APIs.

## Architecture

```
ClojureScript (compiled to JS) 
    ↓
ClojureScriptEngine (Rhino)
    ↓
ScriptRuntime (exposes APIs)
    ↓
SimpleActionAutomator (performs clicks)
```

## Phase 1: Integrate Rhino JavaScript Engine

### Goal
Replace the placeholder ClojureScriptEngine with a working Rhino-based engine that can execute JavaScript code.

### Steps

1. **Add Rhino dependency** to `app1/build.gradle.kts`
   ```kotlin
   dependencies {
       implementation("org.mozilla:rhino:1.7.14")
   }
   ```

2. **Implement Rhino engine** in `ClojureScriptEngine.kt`:
   - Create Rhino Context and Scope
   - Implement `execute()` to actually run JavaScript code
   - Use `Context.evaluateString()` or compile script first
   - Handle errors properly

3. **Initialize Rhino context**:
   - Create Context using `Context.enter()`
   - Create Scriptable scope (TopLevelScope or ImporterTopLevel)
   - Initialize standard objects

### Key Files to Study
- `autojs/src/main/java/com/stardust/autojs/engine/RhinoJavaScriptEngine.kt` - Reference implementation
- `autojs/src/main/java/com/stardust/autojs/rhino/AndroidContextFactory.kt` - Context factory
- `autojs/src/main/java/com/stardust/autojs/rhino/TopLevelScope.kt` - Scope implementation

### Implementation Notes
- Use Rhino's `Context.evaluateString()` for simple scripts
- For better performance, compile scripts first with `Context.compileString()`
- Handle Context lifecycle properly (enter/exit)
- Thread safety: Each engine instance should have its own Context

## Phase 2: Expose Console API to JavaScript

### Goal
Make `console.log()`, `console.error()`, etc. available in JavaScript code.

### Steps

1. **Create JavaScript Console wrapper**:
   - Create a Java/Kotlin class that implements console methods
   - Use Rhino's `@JSFunction` annotation or implement `Scriptable` interface
   - Or use `ScriptableObject.putProperty()` to expose a console object

2. **Expose console in engine initialization**:
   - In `ClojureScriptEngine.init()`, create console object
   - Use `engine.put("console", consoleObject)` to expose it
   - Or use `ScriptableObject.putProperty(scope, "console", consoleObject)`

3. **Bridge to ScriptRuntime.console**:
   - The JavaScript console should call `ScriptRuntime.console.log()` etc.
   - Use a wrapper class that delegates to the runtime's console

### Example Implementation Pattern

```kotlin
class JsConsole(private val runtime: ScriptRuntime) : ScriptableObject() {
    init {
        defineFunctionProperties(arrayOf("log", "error", "warn", "info"), 
            JsConsole::class.java, ScriptableObject.READONLY)
    }
    
    @JSFunction
    fun log(vararg args: Any?) {
        runtime.console.log(args.joinToString(" "))
    }
    
    // Similar for error, warn, info
}
```

### Key Files to Study
- `autojs/src/main/java/com/stardust/autojs/runtime/api/Console.java` - Console interface
- How AutoX.js exposes console to Rhino (check ScriptRuntime initialization)

## Phase 3: Expose Automator API to JavaScript

### Goal
Make `auto.click(x, y)`, `auto.longClick(x, y)`, etc. available in JavaScript code.

### Steps

1. **Create JavaScript Auto wrapper**:
   - Create a Java/Kotlin class that wraps `SimpleActionAutomator`
   - Expose `click(x, y)`, `longClick(x, y)`, `press()`, `swipe()` methods
   - Use `@JSFunction` annotation or implement `Scriptable` interface

2. **Expose auto object in engine initialization**:
   - In `ClojureScriptEngine.init()`, create auto object
   - Use `engine.put("auto", autoObject)` to expose it
   - The auto object should get automator from `ScriptRuntime.getAutomator()`

3. **Handle thread safety**:
   - Gesture operations need proper Handler/Looper
   - Ensure automator is initialized before exposing

### Example Implementation Pattern

```kotlin
class JsAuto(private val runtime: ScriptRuntime) : ScriptableObject() {
    init {
        defineFunctionProperties(arrayOf("click", "longClick", "press", "swipe"), 
            JsAuto::class.java, ScriptableObject.READONLY)
    }
    
    @JSFunction
    fun click(x: Double, y: Double): Boolean {
        val automator = runtime.getAutomator()
            ?: throw IllegalStateException("Automator not initialized. Call initAutomation() first.")
        return automator.click(x.toInt(), y.toInt())
    }
    
    // Similar for longClick, press, swipe
}
```

### Key Files to Study
- `autojs/src/main/java/com/aiselp/autox/api/JsAccessibility.kt` - How AutoX.js exposes automator
- `autojs/src/main/js/v6-api/src/inline_modules/automator.ts` - JavaScript-side API

## Phase 4: Update TestActivity

### Goal
Change TestActivity to execute JavaScript code instead of calling Kotlin APIs directly.

### Steps

1. **Create test JavaScript code**:
   - Simple test: `console.log("Hello from JavaScript!");`
   - Automation test: `auto.click(500, 500);`

2. **Update test methods**:
   - Use `ScriptEngineService.execute()` with `StringScriptSource`
   - Remove direct Kotlin API calls
   - Keep accessibility bridge initialization (needed for automator)

3. **Test flow**:
   ```kotlin
   // Initialize automation (still needed)
   runtime.initAutomation(bridge)
   
   // Execute JavaScript
   val jsCode = """
       console.log("Testing automation...");
       auto.click(500, 500);
       console.log("Click executed!");
   """
   val source = StringScriptSource("test.js", jsCode)
   scriptEngineService.execute(source)
   ```

### Example Test Code

**Phase 1 Test (Console)**:
```javascript
console.log("Hello from JavaScript!");
console.info("This is an info message");
console.warn("This is a warning");
console.error("This is an error");
```

**Phase 2 Test (Automation)**:
```javascript
console.log("Testing automation...");
auto.click(500, 500);
console.log("Click at (500, 500) executed");
```

## Implementation Order

1. ✅ **Phase 1**: Integrate Rhino engine (execute basic JavaScript)
2. ✅ **Phase 2**: Expose Console API (test console.log in JS)
3. ✅ **Phase 3**: Expose Automator API (test auto.click in JS)
4. ✅ **Phase 4**: Update TestActivity (end-to-end test)

## Dependencies

### Required Libraries
- **Rhino**: `org.mozilla:rhino:1.7.14` (or latest version)
- Android dependencies already present

### Build Configuration
- Update `app1/build.gradle.kts` to include Rhino
- May need ProGuard rules if minification is enabled

## Testing Strategy

1. **Unit Tests** (if applicable):
   - Test Rhino engine execution
   - Test API exposure

2. **Integration Tests**:
   - TestActivity button tests
   - Execute JavaScript and verify:
     - Console output appears in logcat
     - Clicks actually happen on screen

3. **Manual Testing**:
   - Run app, tap test buttons
   - Verify JavaScript executes
   - Verify automation works

## Key Implementation Notes

1. **Context Lifecycle**: 
   - Enter Context before execution
   - Exit Context after execution
   - Handle errors properly

2. **Thread Safety**:
   - Each engine instance should have its own Context
   - Context is thread-local in Rhino

3. **API Exposure**:
   - Use `ScriptableObject.putProperty()` for simple objects
   - Use `@JSFunction` annotation for methods
   - Or implement `Scriptable` interface for complex objects

4. **Error Handling**:
   - Catch Rhino exceptions and convert to meaningful errors
   - Log JavaScript errors properly

5. **Automation Initialization**:
   - Automator must be initialized before JavaScript can use it
   - TestActivity should initialize automation before executing JS

## Success Criteria

✅ JavaScript code executes successfully  
✅ `console.log()` works and outputs to logcat  
✅ `auto.click(x, y)` works and performs actual clicks  
✅ TestActivity executes JavaScript instead of calling Kotlin directly  
✅ End-to-end flow: ClojureScript → JavaScript → Engine → Runtime → Automator → Click

## Next Steps After Completion

- Add more automation methods (swipe, scroll, etc.)
- Add error handling and better logging
- Optimize script execution performance
- Add script file loading capability
- Add script debugging support

