# Agents Guide

## Workspace

- Work only in `C:\Games\MySandbox`.
- Do not edit or rely on `C:\Users\AGA\Documents\Ничтофон`; it is an old detached copy.
- Before running commands, explicitly set the working directory to `C:\Games\MySandbox` and verify with `Get-Location` when context is unclear.
- If Git reports dubious ownership, run:

```powershell
git config --global --add safe.directory C:/Games/MySandbox
```

## Project Shape

- Android project: `C:\Games\MySandbox`.
- Legacy Nothing Playground prototype: `C:\Games\MySandbox\nothing-heart-widget`; keep it as reference, do not make it the main target.
- Main Android package: `com.aga.nothingheart`.
- Current product is a native Android home-screen widget plus a normal settings Activity in one APK.

## Development Principles

- Keep the widget quiet, small, emotional, and practical.
- Preserve the transparent `2x2` widget surface.
- Use pre-rendered PNG frames for widget heartbeat animation; Android widgets are `RemoteViews`, not live custom views.
- Keep changes scoped and consistent with the existing plain Android / Java code. Do not add AppCompat, Compose, Kotlin, or large dependencies unless there is a real reason.
- Store progress and plans in Markdown and keep checkboxes current: `[x]` completed, `[ ]` not completed.
- Do not break the local offline prototype while preparing network behavior.
- Keep backend choice transport-independent for now. Do not hard-lock the code to Firebase unless explicitly chosen.
- Prefer a repository/adaptor layer before adding real sync, so Firebase, Supabase, or a custom HTTP backend can be swapped in later.
- Heart customization / mood styles are postponed for now.

## Current Widget Behavior

- Widget shows a glassy red heart on a transparent background.
- Tap plays a 10-frame heartbeat animation.
- Rapid taps update counters immediately and coalesce animation playback to one active pulse plus at most two queued pulses.
- Badge shows received unread beats only.
- Tapping the heart records a sent beat and clears received unread beats.
- Badge formatting supports large values: `1.2K`, `10K`, `999K`, `1.2M`, `99M+`.

## Build And Install

- Build helper: `C:\Games\MySandbox\build-debug.ps1`.
- Debug APK output:

```text
C:\Games\MySandbox\app\build\outputs\apk\debug\app-debug.apk
```

- ADB path, if the local SDK exists in this copy:

```text
C:\Games\MySandbox\android-sdk\platform-tools\adb.exe
```

- If this copy does not include `android-sdk` or `.tools`, inspect the repo and restore/download toolchain before building.

## Git Notes

- Current copied repo may need its branch and remote checked before push:

```powershell
git branch -vv
git remote -v
```

- Previous GitHub repo was `https://github.com/ytppa/heartwidget`.
- Prefer `main` for GitHub unless the user asks otherwise.
- Do not commit local toolchains, build outputs, `local.properties`, `.gradle`, or SDK caches.
