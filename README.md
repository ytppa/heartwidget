# Native Heart Widget

Native Android home-screen widget prototype for a small paired-heart experience.

## Current Features

- Android app with one `AppWidgetProvider`.
- Normal launcher settings Activity in the same APK.
- Transparent `2x2` widget surface.
- Centered warm red PNG heart with a glassy convex highlight.
- 10-frame heartbeat pulse by switching PNG frames through `RemoteViews`.
- Local offline sent/received beat counters stored in `SharedPreferences`.
- Local-only identity and pairing state stored in `SharedPreferences`.
- Backend-independent repository boundary around beat and pairing operations.
- Widget badge shows received unread beats.
- Widget tap records a sent beat, clears received unread beats, and plays the heartbeat animation.
- Compact counter badge formatting: `1.2K`, `10K`, `999K`, `1.2M`, `99M+`.
- Settings app debug controls can send a local test beat and simulate incoming beats.
- Minimum supported Android version: Android 6.0 / API 23.

The heartbeat is implemented with pre-rendered `drawable-nodpi` PNG frames because Android home-screen widgets are not normal live Android views.

## Project Files

- `app/src/main/java/com/aga/nothingheart/HeartWidgetProvider.java`
- `app/src/main/java/com/aga/nothingheart/HeartSettingsActivity.java`
- `app/src/main/java/com/aga/nothingheart/HeartStateStore.java`
- `app/src/main/java/com/aga/nothingheart/HeartRepository.java`
- `app/src/main/java/com/aga/nothingheart/LocalHeartRepository.java`
- `tools/generate-heart-frames.ps1`
- `DEVELOPMENT.md`
- `ROADMAP.md`

## Build

Requirements:

- JDK 17
- Android SDK Platform 35 or newer
- Android SDK Build Tools

Build with the Gradle Wrapper:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is produced at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Or use the local helper script:

```powershell
.\build-debug.ps1
```

The helper expects local SDK/JDK/Gradle folders in the project root. Those folders are machine-specific and are ignored by Git.

## Regenerate Heart Frames

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\generate-heart-frames.ps1
```

## Git Notes

Do not commit the local toolchain:

- `.tools`
- `android-sdk`
- `.gradle-cache`

Do not commit local/build files:

- `.gradle`
- `app\build`
- `local.properties`
