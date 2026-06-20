package com.ytppa.nothingheart;

import android.content.Context;

final class LocalHeartRepository implements HeartRepository {
    private final Context context;

    LocalHeartRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public int getSentBeatCount() {
        return HeartStateStore.getSentBeatCount(context);
    }

    @Override
    public int getReceivedBeatCount() {
        return HeartStateStore.getReceivedBeatCount(context);
    }

    @Override
    public void sendBeatToPartner() {
        HeartStateStore.incrementSentBeatCount(context);
        HeartStateStore.resetReceivedBeatCount(context);
    }

    @Override
    public void simulateIncomingBeat() {
        HeartStateStore.incrementReceivedBeatCount(context);
    }

    @Override
    public void simulateIncomingBeats(int amount) {
        HeartStateStore.addReceivedBeatCount(context, amount);
    }

    @Override
    public void clearReceivedBeats() {
        HeartStateStore.resetReceivedBeatCount(context);
    }

    @Override
    public void resetAllBeatCounts() {
        HeartStateStore.resetAllBeatCounts(context);
    }

    @Override
    public HeartPairingState getPairingState() {
        return HeartStateStore.getPairingState(context);
    }

    @Override
    public HeartPairingState ensureLocalIdentity() {
        return HeartStateStore.ensureLocalIdentity(context);
    }

    @Override
    public void syncPushToken() {
        // Local repository has no remote push token to sync.
    }

    @Override
    public void syncPushToken(String token) {
        // Local repository has no remote push token to sync.
    }

    @Override
    public HeartPairingState requestPairing(String partnerPairCode) {
        return HeartStateStore.setPairingPending(context, partnerPairCode);
    }

    @Override
    public void syncPairing(PairingSyncCallback callback) {
        if (callback != null) {
            callback.onPairingSyncComplete(getPairingState(), false);
        }
    }

    @Override
    public void syncReceivedBeats(BeatSyncCallback callback) {
        if (callback != null) {
            callback.onBeatSyncComplete(getReceivedBeatCount(), false);
        }
    }

    @Override
    public HeartPairingState completeLocalPairing() {
        return HeartStateStore.completeLocalPairing(context);
    }

    @Override
    public HeartPairingState resetPairing() {
        return HeartStateStore.resetPairing(context);
    }
}
