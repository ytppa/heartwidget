# Heart Widget Roadmap

## Product Direction

Turn the widget into a paired "heart attention" experience:

- [x] One APK contains both:
  - [x] the Android home-screen widget;
  - [x] a normal settings app / Activity.
- [x] The widget remains small, emotional, and quiet.
- [ ] The app handles setup, pairing, identity, and customization.

## Wishlist

- [x] Settings app inside the same APK
  - [x] Implemented as local prototype.
  - [x] Openable launcher app.
  - [x] Local pairing status prototype.
  - [ ] Heart appearance settings.
  - [x] Debug / test tools while developing.

- [ ] Pairing with one other device
  - [x] User gets a local pair code in the offline prototype.
  - [ ] User enters another person's connection ID.
  - [ ] User A sends a connect request to User B.
  - [ ] User B accepts or rejects.
  - [ ] Each heart can be connected to only one other heart.
  - [x] Local unpair / reset pairing prototype.
  - [ ] Backend-backed unpair / reset pairing.

- [ ] Heartbeat attention counter
  - [x] Local offline counter prototype implemented.
  - [x] Local sent/received beat counters implemented.
  - [x] Widget badge shows received unread beats.
  - [x] Tapping the local heart records a sent beat and clears received unread beats.
  - [ ] User A taps their own heart.
  - [ ] A counter accumulates for User B.
  - [ ] User B sees the count on or near their heart widget.
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
- [ ] Cross-device state requires a backend or peer-to-peer transport.
- [ ] For a first reliable version, use a small backend rather than direct peer-to-peer.
- [ ] The widget should stay functional even when network is unavailable, then sync later.

## Possible Backend

Preferred first option:

- [ ] Firebase Authentication with anonymous users.
- [ ] Cloud Firestore for pairing and counters.
- [ ] Firebase Cloud Messaging for optional push nudges / widget refresh triggers.

Alternative:

- [ ] Supabase Auth + Postgres + Realtime.
- [ ] Custom small backend with REST/WebSocket.

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

## Open Questions

- [x] Should the counter be shown as a number directly on the heart, or as a small badge? Current prototype uses a small badge.
- [ ] Should tapping always send a beat, or should there be a cooldown / rhythm limit?
- [ ] Should beats accumulate forever until acknowledged, or expire after time?
- [ ] Should appearance changes require partner approval, or apply immediately?
- [ ] Should the app work anonymously only, or later support account recovery?
