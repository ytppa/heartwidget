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
- Local pairing flow can create your code, enter a partner code, create a pending request, complete local pairing, and unpair.
- Backend-independent repository boundary around beat and pairing operations.
- Optional Firebase-backed repository wrapper for anonymous Auth and Firestore writes when local `app/google-services.json` is present.
- Firebase pairing sync can publish outgoing requests, check incoming requests by pair code, accept an incoming request, and detect accepted outgoing requests.
- Firebase beat delivery increments the paired partner's unread count; the receiving device pulls it when the app opens, refreshes, or runs `Sync pairing`.
- FCM client wiring saves each device push token and can refresh the widget when a silent `heart_beat` data push arrives.
- Widget badge shows received unread beats.
- Widget tap records a sent beat, clears received unread beats, and plays the heartbeat animation.
- Compact counter badge formatting: `1.2K`, `10K`, `999K`, `1.2M`, `99M+`.
- Settings app controls can send a local test beat and simulate incoming beats.
- Minimum supported Android version: Android 6.0 / API 23.

The heartbeat is implemented with pre-rendered `drawable-nodpi` PNG frames because Android home-screen widgets are not normal live Android views.

## Project Files

- `app/src/main/java/com/ytppa/nothingheart/HeartWidgetProvider.java`
- `app/src/main/java/com/ytppa/nothingheart/HeartSettingsActivity.java`
- `app/src/main/java/com/ytppa/nothingheart/HeartStateStore.java`
- `app/src/main/java/com/ytppa/nothingheart/HeartRepository.java`
- `app/src/main/java/com/ytppa/nothingheart/LocalHeartRepository.java`
- `app/src/main/java/com/ytppa/nothingheart/FirebaseHeartRepository.java`
- `app/src/main/java/com/ytppa/nothingheart/HeartFirebaseMessagingService.java`
- `functions/index.js`
- `firebase.json`
- `firestore.rules`
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

## Firebase

The Android application id / Firebase package name is:

```text
com.ytppa.nothingheart
```

For local Firebase testing, download `google-services.json` from Firebase Console and place it at:

```text
app\google-services.json
```

That file is machine-specific and ignored by Git. When it is present, the build applies the Google Services plugin and the app uses `FirebaseHeartRepository`; when it is absent, Firebase initialization falls back to local behavior.

Initial Firestore rules are kept in:

```text
firestore.rules
```

Publish those rules from Firebase Console after every rules change.

Silent widget refresh uses Firebase Cloud Messaging plus a Cloud Function:

```text
functions/index.js
```

The function watches `users/{userId}` and sends a `heart_beat` data push when `receivedUnreadBeatCount` increases. Deploy it after Firebase CLI login/project setup:

```powershell
firebase deploy --only functions,firestore:rules
```

Cloud Functions deployment may require enabling the Blaze plan in Firebase.

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
