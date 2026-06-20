# Development Log

## Current Milestone: Local Pairing Model

Goal:

- [x] Add local identity state with `myUserId`, `pairCode`, `partnerId`, and `pairStatus`.
- [x] Add a backend-independent repository/adaptor boundary.
- [x] Keep the first repository implementation local/offline-only.
- [x] Show meaningful local pairing status in the settings Activity.
- [x] Add a local pairing flow around partner code entry.
- [x] Keep existing widget behavior unchanged while the internal model evolves.
- [ ] Add backend-backed pairing.

Status:

- [x] Implemented locally.
- [x] Built successfully in this flattened workspace.
- [x] Installed on a connected test device from this workspace.

Files:

- [x] `app/src/main/java/com/ytppa/nothingheart/HeartRepository.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/LocalHeartRepository.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/HeartPairingState.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/HeartPairingStatus.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/HeartRepositories.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/HeartStateStore.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/HeartSettingsActivity.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/HeartWidgetProvider.java`
- [x] `app/src/main/res/layout/activity_heart_settings.xml`
- [x] `app/src/main/res/values/strings.xml`

Notes:

- [x] Local identity is generated on demand and stored in `SharedPreferences`.
- [x] Pair code is generated locally and displayed in the settings Activity.
- [x] Pairing status supports `none`, `pending`, and `paired`, now with the entered partner pair code.
- [x] Local pairing flow can create a local code, accept a partner code, create a pending request, complete local pairing, and unpair.
- [x] Installed APK was opened on connected Mi A3 and the local pairing flow was verified with partner code `ABCD23` through pending, paired, and unpaired states.
- [x] Widget tap still records a sent beat, clears received unread beats, and plays the heartbeat animation.
- [x] The repository boundary is transport-independent so a backend adapter can replace the local implementation later.
- [x] Backend decision gate is documented in `ROADMAP.md`; provider choice remains explicit and pending.
- [x] Android application id / Firebase package name now uses author nickname: `com.ytppa.nothingheart`.
- [x] Firebase Auth and Firestore dependencies are wired with BoM `34.15.0` and Google Services plugin `4.5.0`.
- [x] `FirebaseHeartRepository` wraps the local repository and asynchronously writes identity, pairing state, pair requests, and counters when Firebase is configured.
- [x] `syncPairing` can detect incoming requests via `incomingPairRequests/{pairCode}` and can detect accepted outgoing requests via `pairRequests/{requestId}`.
- [x] `app/google-services.json` is ignored by Git and used only for local Firebase builds.
- [x] Starter Firestore rules are tracked in `firestore.rules`.
- [x] Firebase smoke test on Mi A3 succeeded after phone DNS/network was restored: `users`, `pairCodes`, `pairRequests`, and `incomingPairRequests` writes were accepted.
- [x] Updated `firestore.rules` were published in Firebase Console after the `incomingPairRequests` rules update.
- [x] Paired beat delivery now writes `receivedUnreadBeatCount + 1` to the remote partner user document.
- [x] Fixed the partner beat write path after `PERMISSION_DENIED`: rules now validate an accepted pair request before allowing a paired device to increment the partner unread count.
- [x] `Sync pairing` and beat send now restore `partner_remote_user_id` from an accepted pair request when an older local paired state only has `local-partner-*`.
- [x] Settings refresh / resume / pairing sync pull remote `receivedUnreadBeatCount` into local state and refresh widgets.
- [x] FCM client wiring added: the app saves `fcmToken`, receives silent `heart_beat` data pushes, pulls received beats, and refreshes widgets.
- [x] Cloud Function source added in `functions/index.js` to send `heart_beat` pushes when `receivedUnreadBeatCount` increases.
- [ ] Deploy Firebase Functions and re-test automatic widget refresh on two phones.
- [ ] Republish `firestore.rules` after the paired-partner beat delivery rule update.
- [ ] Received beat updates are still pull-based in production until the Cloud Function is deployed.
- [ ] Full cross-device sync is not complete yet: pairing and beat delivery have Firestore paths, but received badge refresh is still pull-based.
- [x] Local JDK / Android SDK tooling was restored in ignored project-local folders.

## Completed Milestone: Local App + Counter

Goal:

- [x] Add a normal settings app / launcher Activity to the same APK.
- [x] Add a local offline beat counter on one device.
- [x] Split local state into sent beats and received unread beats.

Implementation plan:

- [x] Add `HeartSettingsActivity` as the launcher Activity.
- [x] Store beat counts in `SharedPreferences`.
- [x] Migrate the old `local_beat_count` value into the new received count once.
- [x] Increment sent beat count when the widget heart is tapped.
- [x] Clear received unread beat count when the widget heart is tapped.
- [x] Show the received unread count as a small badge on the widget.
- [x] Add settings screen controls:
  - [x] view current sent count;
  - [x] view current received unread count;
  - [x] send test beat;
  - [x] simulate incoming beat;
  - [x] simulate 1000 incoming beats;
  - [x] clear received beats;
  - [x] reset all counts;
  - [x] refresh widgets.
- [x] Coalesce rapid tap animations to avoid a long animation backlog.
- [x] Add local paired-device counter semantics without network.
- [ ] Add backend-backed pairing.

Status:

- [x] Implemented locally.
- [x] Built successfully.
- [x] Installed on connected Mi A3 with `adb install -r`.

Files:

- [x] `app/src/main/java/com/ytppa/nothingheart/HeartSettingsActivity.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/HeartStateStore.java`
- [x] `app/src/main/java/com/ytppa/nothingheart/HeartWidgetProvider.java`
- [x] `app/src/main/res/layout/activity_heart_settings.xml`
- [x] `app/src/main/res/layout/widget_heart.xml`

Notes:

- [x] The settings app is a plain Android `Activity`, no external dependencies.
- [x] Launcher icon uses a single smaller heart with generous dark breathing room inside the circular icon.
- [x] Sent and received counts are stored in `SharedPreferences`.
- [x] Widget badge uses the received unread count.
- [x] Widget tap increments the sent count, clears the received unread count, and plays the 10-frame heartbeat animation.
- [x] Heartbeat animation timing was shortened from about 660 ms to about 420 ms for a faster pulse.
- [x] Rapid taps update the counter immediately and coalesce animation playback to one active pulse plus at most two queued pulses.
- [x] The widget shows the count as a small white badge on the upper-right edge of the heart when the count is greater than zero.
- [x] The badge is right-anchored so compact values like `10K` expand leftward instead of drifting right.
- [x] Large counts are compacted for the badge: `1.2K`, `10K`, `999K`, `1.2M`, `99M+`.
- [x] The settings app can simulate incoming beats to test the widget badge without a backend.
- [ ] Full cross-device sync is not complete yet: received badge refresh still needs push or another automatic trigger.
