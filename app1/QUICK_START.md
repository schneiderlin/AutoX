# Quick Start: Build and Test Phase 1 & 2

## Prerequisites

- Android Studio (or command line tools)
- Android device/emulator (API 24+ for gestures)
- Gradle configured

## Build Steps

### 1. Build the Project

```bash
# From project root
./gradlew :app1test:assembleDebug
```

Or in Android Studio:
- Open project
- Wait for Gradle sync
- Select `app1test` from run configurations
- Click Run

### 2. Install on Device

```bash
./gradlew :app1test:installDebug
```

Or use Android Studio's Run button.

## Testing

### Before Testing Phase 2

**IMPORTANT**: You must enable the accessibility service first!

1. Install the app
2. Go to **Android Settings → Accessibility**
3. Find **"AutoCLJS automation service"**
4. **Enable it**

### Test Phase 1 (Console)

1. Launch "AutoCLJS Test" app
2. Tap **"Test Phase 1 (Console)"**
3. Check logcat:
   ```bash
   adb logcat | grep "AutoCLJS"
   ```
4. You should see console output:
   - `[AutoCLJS] Hello from Phase 1!`
   - `[AutoCLJS] This is an info message`
   - etc.

### Test Phase 2 (Automation)

1. **Make sure accessibility service is enabled** (see above)
2. Launch "AutoCLJS Test" app
3. Tap **"Test Phase 2 (Click at 500,500)"**
4. The app should click at screen coordinates (500, 500)
5. Check the log view in the app for results

### Test Multiple Clicks

1. Tap **"Test Phase 2 (Multiple Clicks)"**
2. The app will perform multiple clicks at different coordinates
3. Watch the screen to see the clicks

## Troubleshooting

### "Accessibility service not enabled"

**Solution**: Enable it in Settings → Accessibility

### "Automator is null"

**Solution**: Make sure you call `initAutomation()` before `getAutomator()`

### Build fails

**Solution**: 
- Check that `app1test` is in `settings.gradle.kts`
- Sync Gradle in Android Studio
- Clean and rebuild: `./gradlew clean :app1test:assembleDebug`

### Service not found in Accessibility settings

**Solution**: 
- Uninstall and reinstall the app
- Make sure the service is declared in AndroidManifest.xml
- Check that the service config XML exists

## What to Verify

### Phase 1 ✓
- [ ] Console.log() outputs to logcat
- [ ] Console.info() works
- [ ] Console.warn() works
- [ ] Console.error() works

### Phase 2 ✓
- [ ] Accessibility service can be enabled
- [ ] Click at coordinates works
- [ ] Long click works
- [ ] Multiple clicks work
- [ ] Error handling works (disable service, test error message)

## Next Steps

After successful testing:
1. Test on different screen sizes
2. Test error scenarios
3. Proceed to Phase 3: Element Inspection

