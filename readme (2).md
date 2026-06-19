# Codex Handoff Notes

This file is written for the next Codex instance that continues the project.

## Absolute Current Workspace

Use only:

```text
C:\Games\MySandbox
```

The older path is a detached copy and must not be touched:

```text
C:\Users\AGA\Documents\Ничтофон
```

The Codex app may still open with the old path as `cwd`, so always pass `workdir: C:\Games\MySandbox` for shell commands.

## Project Summary

We are building a native Android widget for a small intimate paired-heart idea.

Original idea:

- A `2x2` transparent home-screen widget.
- Static warm heart by default.
- On tap, the heart beats a couple of times.
- Later, two users connect hearts. A tap means "my heart beat for you".
- Partner sees unread beat count on their widget, similar to unread messages but more personal.

Current implementation:

- Native Android project at the repository root.
- Java, plain Android Activity, no AppCompat/Compose.
- Package: `com.aga.nothingheart`.
- One APK contains:
  - `HeartWidgetProvider` home-screen widget;
  - `HeartSettingsActivity` launcher settings app.
- `minSdk` is 23, Android 6.0+.
- Widget uses `RemoteViews`, so animation is implemented by switching PNG resources.
- Heart frames live in `app/src/main/res/drawable-nodpi/heart_00.png` ... `heart_09.png`.
- Frame generator: `tools/generate-heart-frames.ps1`.

## Current UX And Behavior

Widget:

- Transparent `2x2` surface.
- Centered glassy red heart PNG.
- Small white badge on the upper-right edge of the heart.
- Badge is right-anchored so large compact values expand left.
- Badge is hidden when received count is zero.
- Tap records a sent beat, clears received unread beats, and plays heartbeat animation.
- Animation queue is capped: one active pulse plus at most two queued pulses after rapid taps.

Settings app:

- Shows `Sent beats`.
- Shows `Received unread beats`.
- Shows local pairing identity, pair code, and pairing status.
- Buttons:
  - create local identity;
  - simulate pending pairing;
  - simulate paired partner;
  - reset pairing;
  - send test beat;
  - simulate incoming beat;
  - simulate 1000 incoming beats;
  - clear received beats;
  - reset all counts;
  - refresh widgets.

State:

- Stored in `SharedPreferences` in `HeartStateStore`.
- Current keys include:
  - `sent_beat_count`;
  - `received_beat_count`;
  - `my_user_id`;
  - `pair_code`;
  - `partner_id`;
  - `pair_status`;
  - old `local_beat_count` migration flag/key.
- Old `local_beat_count` migrates once into received count if present.
- Counter formatting is in `HeartStateStore.formatBeatCount`.
- Beat and pairing operations are accessed through `HeartRepository`.

## Important Files

```text
README.md
DEVELOPMENT.md
ROADMAP.md
app/src/main/AndroidManifest.xml
app/src/main/java/com/aga/nothingheart/HeartWidgetProvider.java
app/src/main/java/com/aga/nothingheart/HeartSettingsActivity.java
app/src/main/java/com/aga/nothingheart/HeartStateStore.java
app/src/main/res/layout/widget_heart.xml
app/src/main/res/layout/activity_heart_settings.xml
app/src/main/res/values/strings.xml
app/src/main/res/drawable/count_badge_background.xml
app/src/main/res/drawable/ic_launcher_heart_foreground.xml
tools/generate-heart-frames.ps1
```

## Git State At Handoff

Repository layout target:

```text
Android project files live at the Git repository root, not under native-heart-widget/.
GitHub branch should be main unless the user asks otherwise.
```

Previous GitHub repo used by the old workspace:

```text
https://github.com/ytppa/heartwidget
```

Before pushing from this copy:

1. Check `git status --short --branch`.
2. Check `git branch -vv`.
3. Check `git remote -v`.
4. Reconnect `origin` if needed.
5. Push the flattened root layout to `main`.

## Build Context

Previous Android toolchain in the old location used:

- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.7.3
- Gradle Wrapper 8.9

In this new copy, verify whether these exist before building:

```text
C:\Games\MySandbox\.tools
C:\Games\MySandbox\android-sdk
C:\Games\MySandbox\.gradle-cache
```

Build command from project folder:

```powershell
powershell -ExecutionPolicy Bypass -File 'C:\Games\MySandbox\build-debug.ps1'
```

Install command if ADB exists here:

```powershell
C:\Games\MySandbox\android-sdk\platform-tools\adb.exe install -r C:\Games\MySandbox\app\build\outputs\apk\debug\app-debug.apk
```

If phone appears as `unauthorized`, user must approve USB debugging on the phone.

## Product Direction

Immediate direction agreed with user:

- Postpone heart customization / mood style.
- Continue developing the future pair/sync model locally first.
- Do not assume Firebase is final.
- Design a backend-independent sync boundary.

Reasoning:

- Firebase is convenient because it gives anonymous auth, realtime data, offline persistence, security rules, Android SDK, and FCM push.
- But alternatives are valid:
  - Supabase Auth + Postgres + Realtime + RLS;
  - custom small backend with REST/WebSocket + PostgreSQL;
  - Firebase if speed matters most.
- Android app should not connect directly to a raw PostgreSQL/MySQL database with embedded DB credentials.
- Proper shape is Android -> API/SDK/backend -> database.

Recommended next implementation step:

- Build the real backend-backed pairing adapter after choosing the backend with the user.
- Keep the current local repository implementation as the offline prototype and fallback shape.
- Consider renaming semantics from `sent/received` toward backend-ready concepts:
  - `beatsForMe`;
  - `beatsForPartner`.
- Keep widget behavior unchanged while internal model evolves.

## Testing Checklist

After installing APK:

- Open `Heart Widget` app.
- Tap `Simulate incoming beat`; widget badge should appear.
- Tap widget heart; badge should disappear and `Sent beats` should increase.
- Tap `Simulate 1000 incoming beats`; badge should compact to a `K` value.
- Rapidly tap widget; counter should update immediately and animation should not build a long backlog.

## Style Preferences From User

- First heart style should remain the simple warm/glassy red PNG version.
- The user rejected stronger blur and returned to weak blur.
- Badge should be subtle, white, small, upper-right on the heart edge, right-anchored.
- Launcher icon should have the heart smaller and centered with breathing room inside the circular icon.
- Keep project notes in Markdown and update them continuously.
