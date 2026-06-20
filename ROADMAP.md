# Heart Widget Roadmap

## Product Direction

Turn the widget into a paired "heart attention" experience:

- [x] One APK contains both:
  - [x] the Android home-screen widget;
  - [x] a normal settings app / Activity.
- [x] The widget remains small, emotional, and quiet.
- [ ] The app handles setup, pairing, identity, and customization.

## Wishlist

Longer-range ideas that are not in the active milestone are kept in `BACKLOG.md`.

- [x] Settings app inside the same APK
  - [x] Implemented as local prototype.
  - [x] Openable launcher app.
  - [x] Local pairing status prototype.
  - [ ] Heart appearance settings.
  - [x] Debug / test tools while developing.

- [ ] Pairing with one other device
  - [x] User gets a local pair code in the offline prototype.
  - [x] User enters another person's connection ID in the local prototype.
  - [x] Local prototype creates a pending request from the entered partner code.
  - [x] User A can publish a backend-backed connect request to User B's pair code.
  - [x] User B can sync and accept an incoming backend-backed request.
  - [ ] User B can reject an incoming request.
  - [x] Local paired state is limited to one partner.
  - [x] Local unpair / reset pairing prototype.
  - [ ] Backend-backed unpair / reset pairing.

- [ ] Heartbeat attention counter
  - [x] Local offline counter prototype implemented.
  - [x] Local sent/received beat counters implemented.
  - [x] Widget badge shows received unread beats.
  - [x] Tapping the local heart records a sent beat and clears received unread beats.
  - [x] User A taps their own heart and writes an unread beat to paired User B in Firestore.
  - [x] A counter accumulates for User B in Firestore.
  - [x] User B can pull the count into the local widget badge.
  - [x] Android app can receive a silent FCM data push and refresh the widget badge.
  - [ ] Deploy and test the Cloud Function that sends the push after the Firestore counter increases.
  - [ ] When User B taps their own heart:
    - [ ] User B's received counter resets;
    - [ ] User A starts accumulating a received counter.
  - [ ] Product meaning: "my heart beat for you", like unread attention beats.

- [ ] Remote heart appearance / mood
  - [ ] User can choose how their heart appears to the other person.
  - [ ] This works like a tiny emotional avatar or mood signal.
  - [ ] Only one paired partner sees this chosen appearance.

## Technical Notes

- [x] Android widget and settings Activity can live in one APK.
- [x] Local paired-counter semantics can be prototyped without a backend.
- [x] Backend-independent repository/adaptor boundary exists.
- [x] Local identity and pair status can be prototyped without a backend.
- [x] Firebase Auth / Firestore SDK wiring exists behind the repository boundary.
- [ ] Cross-device state requires a backend or peer-to-peer transport.
- [ ] For a first reliable version, use a small backend rather than direct peer-to-peer.
- [ ] User cannot attach a payment card for Firebase Blaze right now, so prefer a free custom backend push bridge over Firebase Cloud Functions.
- [x] Supabase project path was started with Anonymous Auth, Postgres tables, RLS policies, and pairing/beat RPC functions.
- [ ] The widget should stay functional even when network is unavailable, then sync later.

## Backend Decision Gate

Current decision state:

- [x] Repository operations now map to backend-shaped actions: create identity, request pairing, complete local pairing, unpair, send beat, and clear received beats.
- [x] Firebase was selected for the first backend path.
- [x] The code keeps `LocalHeartRepository` available as the offline fallback.
- [x] Firestore rules starter file is tracked as `firestore.rules`.
- [x] Backend request exchange has a first Firestore-backed path through `pairRequests` and `incomingPairRequests`.
- [x] Firebase writes passed a real-device smoke test on Mi A3 after phone DNS/network was restored.
- [x] Firebase beat delivery writes unread beats to the paired partner document.
- [ ] Received beat refresh has FCM client/function source, but automatic widget refresh still needs a deployable push sender and two-phone testing.
- [ ] Firebase Cloud Functions / Blaze is blocked for now because the user cannot attach a payment card.
- [x] Next backend direction shifted to Supabase Free: anonymous Auth, Postgres/RLS, RPC functions, and FCM client transport.
- [ ] Android still needs a Supabase repository adapter before this path is usable from the app.

Selected first option:

- [x] Firebase Authentication with anonymous users.
- [x] Cloud Firestore for pairing and counters.
- [x] Firebase Cloud Messaging client wiring for widget refresh triggers.
- [ ] Custom backend bridge for sending widget refresh triggers without Firebase Blaze.
- [ ] Firebase Cloud Functions deployment for sending widget refresh triggers is parked as an optional later path.

Alternatives:

- [ ] Supabase Auth + Postgres + Realtime.
- [x] Supabase Auth + Postgres/RPC selected as the current free path around Blaze/card constraints.
- [ ] Custom small backend with REST/FCM bridge remains a fallback if Supabase limits or Android integration block us.

## Data Sketch

```text
users/{userId}
  publicPairCode
  pairedUserId
  chosenHeartStyle

pairRequests/{requestId}
  fromUserId
  toPairCode
  status: pending | accepted | rejected | expired
  createdAt

pairs/{pairId}
  userAId
  userBId
  beatCountForA
  beatCountForB
  heartStyleForA
  heartStyleForB
  updatedAt
```

Current Firestore collections:

```text
users/{firebaseUid}
pairCodes/{publicPairCode}
pairRequests/{fromUserId_toPairCode}
incomingPairRequests/{toPairCode}
```

Current Supabase objects are tracked in `supabase/schema.sql`:

```text
profiles
pair_codes
pair_requests
pairs
pair_member_state
beat_events
RPC: ensure_profile, create_pair_code, request_pairing, accept_pairing,
     get_my_pairing_state, send_beat, clear_received_beats
```

## Open Questions

- [x] Should the counter be shown as a number directly on the heart, or as a small badge? Current prototype uses a small badge.
- [ ] Should tapping always send a beat, or should there be a cooldown / rhythm limit?
- [ ] Should beats accumulate forever until acknowledged, or expire after time?
- [ ] Should appearance changes require partner approval, or apply immediately?
- [ ] Should the app work anonymously only, or later support account recovery?
