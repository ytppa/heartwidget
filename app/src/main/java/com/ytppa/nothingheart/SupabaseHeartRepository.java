package com.ytppa.nothingheart;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class SupabaseHeartRepository implements HeartRepository {
    private static final String TAG = "HeartSupabaseRepo";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final Context context;
    private final LocalHeartRepository localRepository;
    private final SupabaseApiClient apiClient;

    SupabaseHeartRepository(Context context) {
        this.context = context.getApplicationContext();
        localRepository = new LocalHeartRepository(this.context);
        apiClient = new SupabaseApiClient(this.context);
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
        runAsync(() -> {
            String response = apiClient.rpc("send_beat", new JSONObject());
            JSONObject beat = firstObject(response);
            int sentCount = beat.optInt("sent_count", localRepository.getSentBeatCount());
            HeartStateStore.setSentBeatCount(context, sentCount);
            syncRemoteState();
        }, exception -> Log.w(TAG, "Supabase send beat failed.", exception));
    }

    @Override
    public void simulateIncomingBeat() {
        localRepository.simulateIncomingBeat();
    }

    @Override
    public void simulateIncomingBeats(int amount) {
        localRepository.simulateIncomingBeats(amount);
    }

    @Override
    public void clearReceivedBeats() {
        localRepository.clearReceivedBeats();
        runAsync(() -> {
            apiClient.rpc("clear_received_beats", new JSONObject());
            syncRemoteState();
        }, exception -> Log.w(TAG, "Supabase clear received beats failed.", exception));
    }

    @Override
    public void resetAllBeatCounts() {
        localRepository.resetAllBeatCounts();
    }

    @Override
    public HeartPairingState getPairingState() {
        return localRepository.getPairingState();
    }

    @Override
    public HeartPairingState ensureLocalIdentity() {
        HeartPairingState state = localRepository.ensureLocalIdentity();
        runAsync(() -> {
            ensureRemoteProfile(null);
            apiClient.rpc("create_pair_code", new JSONObject());
            syncRemoteState();
            Log.d(TAG, "Supabase identity sync succeeded.");
        }, exception -> Log.w(TAG, "Supabase identity sync failed.", exception));
        return state;
    }

    @Override
    public void syncPushToken() {
        try {
            FirebaseMessaging.getInstance()
                    .getToken()
                    .addOnSuccessListener(this::syncPushToken)
                    .addOnFailureListener(exception -> Log.w(TAG, "Supabase FCM token read failed.", exception));
        } catch (RuntimeException exception) {
            Log.w(TAG, "Supabase FCM token read skipped.", exception);
        }
    }

    @Override
    public void syncPushToken(String token) {
        if (isBlank(token)) {
            return;
        }

        runAsync(() -> {
            ensureRemoteProfile(token);
            Log.d(TAG, "Supabase FCM token sync succeeded.");
        }, exception -> Log.w(TAG, "Supabase FCM token sync failed.", exception));
    }

    @Override
    public HeartPairingState requestPairing(String partnerPairCode) {
        HeartPairingState state = localRepository.requestPairing(partnerPairCode);
        runAsync(() -> {
            JSONObject body = new JSONObject()
                    .put("p_to_pair_code", HeartStateStore.normalizePairCode(partnerPairCode));
            apiClient.rpc("request_pairing", body);
            syncRemoteState();
        }, exception -> Log.w(TAG, "Supabase pairing request failed.", exception));
        return state;
    }

    @Override
    public void syncPairing(PairingSyncCallback callback) {
        runAsync(() -> {
            Log.d(TAG, "Supabase pairing sync started.");
            ensureRemoteProfile(null);
            apiClient.rpc("create_pair_code", new JSONObject());
            RemoteState state = syncRemoteState();
            Log.d(TAG, "Supabase pairing sync succeeded: " + state.pairingState.getPairStatus().getStoredValue()
                    + " / " + state.pairingState.getPairCode());
            if (callback != null) {
                callback.onPairingSyncComplete(state.pairingState, state.changed);
            }
        }, exception -> {
            Log.w(TAG, "Supabase pairing sync failed.", exception);
            if (callback != null) {
                callback.onPairingSyncFailed(exception);
            }
        });
    }

    @Override
    public void syncReceivedBeats(BeatSyncCallback callback) {
        runAsync(() -> {
            RemoteState state = syncRemoteState();
            if (callback != null) {
                callback.onBeatSyncComplete(state.receivedUnreadCount, state.changed);
            }
        }, exception -> {
            Log.w(TAG, "Supabase received beat sync failed.", exception);
            if (callback != null) {
                callback.onBeatSyncFailed(exception);
            }
        });
    }

    @Override
    public HeartPairingState completeLocalPairing() {
        HeartPairingState previousState = localRepository.getPairingState();
        HeartPairingState state = localRepository.completeLocalPairing();
        if (previousState.hasPairRequestId()) {
            runAsync(() -> {
                JSONObject body = new JSONObject()
                        .put("p_request_id", previousState.getPairRequestId());
                apiClient.rpc("accept_pairing", body);
                syncRemoteState();
            }, exception -> Log.w(TAG, "Supabase accept pairing failed.", exception));
        }
        return state;
    }

    @Override
    public HeartPairingState resetPairing() {
        return localRepository.resetPairing();
    }

    private void ensureRemoteProfile(String fcmToken) throws JSONException, java.io.IOException {
        JSONObject body = new JSONObject()
                .put("p_fcm_token", isBlank(fcmToken) ? JSONObject.NULL : fcmToken);
        apiClient.rpc("ensure_profile", body);
    }

    private RemoteState syncRemoteState() throws JSONException, java.io.IOException {
        int previousSentCount = localRepository.getSentBeatCount();
        int previousReceivedCount = localRepository.getReceivedBeatCount();
        HeartPairingState previousPairingState = localRepository.getPairingState();

        String response = apiClient.rpc("get_my_pairing_state", new JSONObject());
        JSONObject json = firstObject(response);
        HeartPairingStatus status = mapPairStatus(json.optString("pair_status", "none"));
        HeartPairingState pairingState = HeartStateStore.setRemotePairingState(
                context,
                json.optString("pair_code", ""),
                json.optString("partner_pair_code", ""),
                json.optString("partner_user_id", ""),
                json.optString("pair_request_id", ""),
                status
        );

        int sentCount = json.optInt("sent_count", previousSentCount);
        int receivedUnreadCount = json.optInt("received_unread_count", previousReceivedCount);
        HeartStateStore.setSentBeatCount(context, sentCount);
        HeartStateStore.setReceivedBeatCount(context, receivedUnreadCount);

        boolean changed = sentCount != previousSentCount
                || receivedUnreadCount != previousReceivedCount
                || !samePairingState(previousPairingState, pairingState);
        return new RemoteState(pairingState, receivedUnreadCount, changed);
    }

    private HeartPairingStatus mapPairStatus(String status) {
        if ("paired".equals(status)) {
            return HeartPairingStatus.PAIRED;
        }
        if ("pending_incoming".equals(status) || "pending_outgoing".equals(status)) {
            return HeartPairingStatus.PENDING;
        }
        return HeartPairingStatus.NONE;
    }

    private boolean samePairingState(HeartPairingState first, HeartPairingState second) {
        return first.getPairStatus() == second.getPairStatus()
                && first.getPairCode().equals(second.getPairCode())
                && first.getPartnerPairCode().equals(second.getPartnerPairCode())
                && first.getPartnerRemoteUserId().equals(second.getPartnerRemoteUserId())
                && first.getPairRequestId().equals(second.getPairRequestId());
    }

    private JSONObject firstObject(String response) throws JSONException {
        String trimmed = response == null ? "" : response.trim();
        if (trimmed.startsWith("[")) {
            JSONArray array = new JSONArray(trimmed);
            if (array.length() == 0) {
                return new JSONObject();
            }
            return array.getJSONObject(0);
        }
        if (trimmed.startsWith("{")) {
            return new JSONObject(trimmed);
        }
        return new JSONObject();
    }

    private void runAsync(ThrowingRunnable runnable, FailureConsumer failureConsumer) {
        EXECUTOR.execute(() -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                if (failureConsumer != null) {
                    failureConsumer.accept(exception);
                }
            }
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private interface FailureConsumer {
        void accept(Exception exception);
    }

    private static final class RemoteState {
        private final HeartPairingState pairingState;
        private final int receivedUnreadCount;
        private final boolean changed;

        private RemoteState(HeartPairingState pairingState, int receivedUnreadCount, boolean changed) {
            this.pairingState = pairingState;
            this.receivedUnreadCount = receivedUnreadCount;
            this.changed = changed;
        }
    }
}
