# Phase 1 Reading Guide



## Key Questions to Answer While Reading

### 1. **ScriptSource**: What represents the script code?
**Answer**: `ScriptSource` is an abstract base class that represents a source of script code. The library receives **compiled JavaScript code** (ClojureScript is compiled to JS before reaching this library). `StringScriptSource` is a concrete implementation that holds JavaScript code as a string. The `engineName` is "javascript" because the library executes JavaScript, not ClojureScript.

### 2. **Console**: How do scripts output messages?
**Answer**: The `Console` interface provides logging methods (`log`, `info`, `warn`, `error`). `SimpleConsole` implements this by writing to Android's Log system. Scripts running in the JavaScript engine can call these console methods, which are exposed to the JS runtime through the `ScriptRuntime`.

### 3. **ScriptRuntime**: What environment do scripts run in?
**Answer**: `ScriptRuntime` provides the execution environment for scripts. It holds:
- A `Console` instance for output
- Application context (for Android APIs)
- Future APIs will be added here (automation, file I/O, etc.)

The runtime is created when the engine initializes and provides the bridge between JavaScript code and Android/Java APIs.

### 4. **ScriptEngine**: How is script code actually executed?
**Answer**: `ScriptEngine` is the interface for executing scripts. The current implementation (`ClojureScriptEngine` - though it should be named `JavaScriptEngine`) will:
- Initialize a `ScriptRuntime` when `init()` is called
- Execute JavaScript code via `execute(scriptSource)`
- Expose the runtime's APIs (like `console`) to the JavaScript environment
- Handle errors and lifecycle (destroy, forceStop)

**Important**: The engine receives **compiled JavaScript**, not ClojureScript source. ClojureScript → JavaScript compilation happens outside this library.

### 5. **ScriptEngineService**: What's the public API for running scripts?
**Answer**: `ScriptEngineService` is the high-level API for script execution. It:
- Manages a JavaScript engine (currently singleton pattern)
- Provides `execute(source)` to run scripts
- Returns `ScriptExecution` with results/errors
- Handles engine lifecycle

**Architecture Note**: ClojureScript code is compiled to JavaScript elsewhere, then passed to this service as JavaScript strings.

## Files Summary

| File | Lines | Complexity | Purpose |
|------|-------|------------|---------|
| ScriptSource.kt | 13 | ⭐ Simple | Base class for script sources |
| StringScriptSource.kt | 10 | ⭐ Simple | String-based script source |
| Console.kt | 14 | ⭐ Simple | Console interface |
| SimpleConsole.kt | 40 | ⭐⭐ Easy | Android Log console |
| ScriptRuntime.kt | 28 | ⭐⭐ Easy | Runtime environment |
| ScriptEngine.kt | 76 | ⭐⭐⭐ Medium | Engine interface & base class |
| ClojureScriptEngine.kt | 67 | ⭐⭐⭐ Medium | JavaScript engine (executes compiled JS from ClojureScript) |
| ScriptEngineService.kt | 85 | ⭐⭐⭐ Medium | High-level service API (uses singleton engine) |
| Phase1Example.kt | 60 | ⭐⭐ Easy | Usage examples |

## Next Steps After Reading

1. **Understand the flow**: Trace through `Phase1Example.executeHelloWorld()` step by step
2. **Identify TODOs**: Find where JavaScript engine integration needs to happen (the engine needs to actually execute JS, not just log)
3. **Think about extensions**: How would you add more APIs to ScriptRuntime?
4. **Consider Phase 2**: How would automation APIs fit into this structure?

## Architecture Clarification

**Important**: ClojureScript is compiled to JavaScript **before** it reaches this library. From the library's perspective:
- Input: Compiled JavaScript code (as strings)
- Engine: JavaScript engine (Rhino, V8, or similar)
- Output: Execution results, console logs, etc.

The library doesn't need to know about ClojureScript - it just executes JavaScript and provides Android APIs to that JavaScript runtime.

## Common Patterns to Notice

1. **Builder Pattern**: `ScriptEngineService.Builder`
2. **Abstract Base Class**: `AbstractScriptEngine` provides common functionality
3. **Interface Segregation**: `Console` interface allows different implementations
4. **Singleton Pattern**: Service uses a lazy singleton engine for simplicity
5. **Error Handling**: Exceptions are caught and returned in `ScriptExecution`

---

**Start with ScriptSource.kt and work your way up!** 🚀

