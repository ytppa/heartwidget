package com.ytppa.nothingheart;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

final class FirebaseHeartRepository implements HeartRepository {
    private static final String TAG = "HeartFirebaseRepo";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_PAIR_CODES = "pairCodes";
    private static final String COLLECTION_PAIR_REQUESTS = "pairRequests";
    private static final String COLLECTION_INCOMING_PAIR_REQUESTS = "incomingPairRequests";

    private final LocalHeartRepository localRepository;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final boolean firebaseReady;
    private final Context context;

    FirebaseHeartRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.context = appContext;
        localRepository = new LocalHeartRepository(appContext);

        FirebaseApp app = initializeFirebase(appContext);
        firebaseReady = app != null;
        auth = firebaseReady ? FirebaseAuth.getInstance(app) : null;
        firestore = firebaseReady ? FirebaseFirestore.getInstance(app) : null;
    }

    @Override
    public int getSentBeatCount() {
        return localRepository.getSentBeatCount();
    }

    @Override
    public int getReceivedBeatCount() {
        return localRepository.getReceivedBeatCount();
    }

    @Override
    public void sendBeatToPartner() {
        localRepository.sendBeatToPartner();
        withSignedInUser(user -> {
            Map<String, Object> data = new HashMap<>();
            data.put("sentBeatCount", FieldValue.increment(1));
            data.put("receivedUnreadBeatCount", 0);
            data.put("lastSentBeatAt", FieldValue.serverTimestamp());
            data.put("updatedAt", FieldValue.serverTimestamp());
            logFailures(firestore.collection(COLLECTION_USERS).document(user.getUid()).set(data, SetOptions.merge()));
        });
    }

    @Override
    public void simulateIncomingBeat() {
        localRepository.simulateIncomingBeat();
        syncBeatCounts();
    }

    @Override
    public void simulateIncomingBeats(int amount) {
        localRepository.simulateIncomingBeats(amount);
        syncBeatCounts();
    }

    @Override
    public void clearReceivedBeats() {
        localRepository.clearReceivedBeats();
        syncBeatCounts();
    }

    @Override
    public void resetAllBeatCounts() {
        localRepository.resetAllBeatCounts();
        syncBeatCounts();
    }

    @Override
    public HeartPairingState getPairingState() {
        return localRepository.getPairingState();
    }

    @Override
    public HeartPairingState ensureLocalIdentity() {
        HeartPairingState state = localRepository.ensureLocalIdentity();
        syncPairingState(state);
        return state;
    }

    @Override
    public HeartPairingState requestPairing(String partnerPairCode) {
        HeartPairingState state = localRepository.requestPairing(partnerPairCode);
        syncPairingState(state);
        writePairRequest(state, "pending");
        return state;
    }

    @Override
    public void syncPairing(PairingSyncCallback callback) {
        HeartPairingState state = localRepository.ensureLocalIdentity();
        syncPairingState(state);

        if (!firebaseReady) {
            notifySyncComplete(callback, state, false);
            return;
        }

        withSignedInUser(user -> {
            HeartPairingState latestState = localRepository.getPairingState();
            if (latestState.getPairStatus() == HeartPairingStatus.PENDING && latestState.hasPartnerPairCode()) {
                checkOutgoingPairingStatus(user, latestState, callback);
                return;
            }
            if (latestState.getPairStatus() == HeartPairingStatus.NONE) {
                checkIncomingPairingRequest(latestState, callback);
                return;
            }
            notifySyncComplete(callback, latestState, false);
        }, exception -> notifySyncFailed(callback, exception));
    }

    @Override
    public HeartPairingState completeLocalPairing() {
        HeartPairingState previousState = localRepository.getPairingState();
        HeartPairingState state = localRepository.completeLocalPairing();
        syncPairingState(state);
        if (previousState.hasPairRequestId() && previousState.hasPartnerRemoteUserId()) {
            acceptIncomingPairRequest(state);
        } else {
            writePairRequest(state, "accepted_local");
        }
        return state;
    }

    @Override
    public HeartPairingState resetPairing() {
        HeartPairingState state = localRepository.resetPairing();
        syncPairingState(state);
        return state;
    }

    private FirebaseApp initializeFirebase(Context context) {
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) {
                return FirebaseApp.getInstance();
            }
            return FirebaseApp.initializeApp(context);
        } catch (IllegalStateException exception) {
            Log.w(TAG, "Firebase is not configured; using local repository only.", exception);
            return null;
        }
    }

    private void syncPairingState(HeartPairingState state) {
        if (!state.hasLocalIdentity()) {
            return;
        }

        withSignedInUser(user -> {
            logFailures(firestore.collection(COLLECTION_USERS)
                    .document(user.getUid())
                    .set(buildUserData(state), SetOptions.merge()));

            Map<String, Object> pairCodeData = new HashMap<>();
            pairCodeData.put("ownerUserId", user.getUid());
            pairCodeData.put("ownerLocalUserId", state.getMyUserId());
            pairCodeData.put("updatedAt", FieldValue.serverTimestamp());

            logFailures(firestore.collection(COLLECTION_PAIR_CODES)
                    .document(state.getPairCode())
                    .set(pairCodeData, SetOptions.merge()));
        });
    }

    private Map<String, Object> buildUserData(HeartPairingState state) {
        Map<String, Object> data = new HashMap<>();
        data.put("localUserId", state.getMyUserId());
        data.put("pairCode", state.getPairCode());
        data.put("pairStatus", state.getPairStatus().getStoredValue());
        data.put("sentBeatCount", getSentBeatCount());
        data.put("receivedUnreadBeatCount", getReceivedBeatCount());
        data.put("updatedAt", FieldValue.serverTimestamp());

        if (state.hasPartnerPairCode()) {
            data.put("partnerPairCode", state.getPartnerPairCode());
        } else {
            data.put("partnerPairCode", FieldValue.delete());
        }

        if (state.hasPartner()) {
            data.put("partnerId", state.getPartnerId());
        } else {
            data.put("partnerId", FieldValue.delete());
        }

        return data;
    }

    private void writePairRequest(HeartPairingState state, String status) {
        if (!state.hasLocalIdentity() || !state.hasPartnerPairCode()) {
            return;
        }

        withSignedInUser(user -> {
            String pairRequestId = buildPairRequestId(user.getUid(), state.getPartnerPairCode());
            HeartStateStore.setOutgoingPairRequestId(context, pairRequestId);

            Map<String, Object> data = new HashMap<>();
            data.put("fromUserId", user.getUid());
            data.put("fromLocalUserId", state.getMyUserId());
            data.put("fromPairCode", state.getPairCode());
            data.put("toPairCode", state.getPartnerPairCode());
            data.put("status", status);
            data.put("updatedAt", FieldValue.serverTimestamp());

            if ("pending".equals(status)) {
                data.put("createdAt", FieldValue.serverTimestamp());
            }

            logFailures(firestore.collection(COLLECTION_PAIR_REQUESTS)
                    .document(pairRequestId)
                    .set(data, SetOptions.merge()));

            if ("pending".equals(status)) {
                Map<String, Object> incomingData = new HashMap<>();
                incomingData.put("requestId", pairRequestId);
                incomingData.put("fromUserId", user.getUid());
                incomingData.put("fromLocalUserId", state.getMyUserId());
                incomingData.put("fromPairCode", state.getPairCode());
                incomingData.put("toPairCode", state.getPartnerPairCode());
                incomingData.put("status", status);
                incomingData.put("updatedAt", FieldValue.serverTimestamp());
                incomingData.put("createdAt", FieldValue.serverTimestamp());

                logFailures(firestore.collection(COLLECTION_INCOMING_PAIR_REQUESTS)
                        .document(state.getPartnerPairCode())
                        .set(incomingData, SetOptions.merge()));
            }
        });
    }

    private void checkIncomingPairingRequest(HeartPairingState state, PairingSyncCallback callback) {
        firestore.collection(COLLECTION_INCOMING_PAIR_REQUESTS)
                .document(state.getPairCode())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists() || !"pending".equals(document.getString("status"))) {
                        notifySyncComplete(callback, state, false);
                        return;
                    }

                    String fromPairCode = document.getString("fromPairCode");
                    String fromUserId = document.getString("fromUserId");
                    String requestId = document.getString("requestId");
                    if (!HeartStateStore.isValidPairCode(fromPairCode) || isBlank(fromUserId) || isBlank(requestId)) {
                        notifySyncComplete(callback, state, false);
                        return;
                    }

                    HeartPairingState updatedState = HeartStateStore.setIncomingPairingPending(
                            context,
                            fromPairCode,
                            fromUserId,
                            requestId
                    );
                    syncPairingState(updatedState);
                    notifySyncComplete(callback, updatedState, true);
                })
                .addOnFailureListener(exception -> notifySyncFailed(callback, exception));
    }

    private void checkOutgoingPairingStatus(FirebaseUser user, HeartPairingState state, PairingSyncCallback callback) {
        String pairRequestId = state.hasPairRequestId()
                ? state.getPairRequestId()
                : buildPairRequestId(user.getUid(), state.getPartnerPairCode());

        firestore.collection(COLLECTION_PAIR_REQUESTS)
                .document(pairRequestId)
                .get()
                .addOnSuccessListener(document -> handleOutgoingPairingDocument(document, state, pairRequestId, callback))
                .addOnFailureListener(exception -> notifySyncFailed(callback, exception));
    }

    private void handleOutgoingPairingDocument(
            DocumentSnapshot document,
            HeartPairingState state,
            String pairRequestId,
            PairingSyncCallback callback
    ) {
        if (!document.exists() || !"accepted".equals(document.getString("status"))) {
            notifySyncComplete(callback, state, false);
            return;
        }

        String acceptedByUserId = document.getString("acceptedByUserId");
        String acceptedByPairCode = document.getString("acceptedByPairCode");
        if (isBlank(acceptedByUserId) || !HeartStateStore.isValidPairCode(acceptedByPairCode)) {
            notifySyncComplete(callback, state, false);
            return;
        }

        HeartPairingState updatedState = HeartStateStore.setPairedWithRemotePartner(
                context,
                acceptedByPairCode,
                acceptedByUserId,
                pairRequestId
        );
        syncPairingState(updatedState);
        notifySyncComplete(callback, updatedState, true);
    }

    private void acceptIncomingPairRequest(HeartPairingState state) {
        withSignedInUser(user -> {
            Map<String, Object> data = new HashMap<>();
            data.put("status", "accepted");
            data.put("acceptedByUserId", user.getUid());
            data.put("acceptedByPairCode", state.getPairCode());
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("acceptedAt", FieldValue.serverTimestamp());

            logFailures(firestore.collection(COLLECTION_PAIR_REQUESTS)
                    .document(state.getPairRequestId())
                    .set(data, SetOptions.merge()));

            Map<String, Object> incomingData = new HashMap<>(data);
            incomingData.put("requestId", state.getPairRequestId());
            logFailures(firestore.collection(COLLECTION_INCOMING_PAIR_REQUESTS)
                    .document(state.getPairCode())
                    .set(incomingData, SetOptions.merge()));
        });
    }

    private String buildPairRequestId(String userId, String partnerPairCode) {
        return userId + "_" + partnerPairCode;
    }

    private void syncBeatCounts() {
        withSignedInUser(user -> {
            Map<String, Object> data = new HashMap<>();
            data.put("sentBeatCount", getSentBeatCount());
            data.put("receivedUnreadBeatCount", getReceivedBeatCount());
            data.put("updatedAt", FieldValue.serverTimestamp());
            logFailures(firestore.collection(COLLECTION_USERS).document(user.getUid()).set(data, SetOptions.merge()));
        });
    }

    private void withSignedInUser(FirebaseUserConsumer consumer) {
        withSignedInUser(consumer, null);
    }

    private void withSignedInUser(FirebaseUserConsumer consumer, FirebaseFailureConsumer failureConsumer) {
        if (!firebaseReady) {
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            consumer.accept(currentUser);
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        Log.d(TAG, "Anonymous Firebase sign-in succeeded.");
                        consumer.accept(user);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.w(TAG, "Anonymous Firebase sign-in failed.", exception);
                    if (failureConsumer != null) {
                        failureConsumer.accept(exception);
                    }
                });
    }

    private void logFailures(com.google.android.gms.tasks.Task<?> task) {
        task.addOnFailureListener(exception -> Log.w(TAG, "Firebase write failed.", exception));
    }

    private interface FirebaseUserConsumer {
        void accept(FirebaseUser user);
    }

    private interface FirebaseFailureConsumer {
        void accept(Exception exception);
    }

    private void notifySyncComplete(PairingSyncCallback callback, HeartPairingState state, boolean changed) {
        if (callback != null) {
            callback.onPairingSyncComplete(state, changed);
        }
    }

    private void notifySyncFailed(PairingSyncCallback callback, Exception exception) {
        if (callback != null) {
            callback.onPairingSyncFailed(exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
