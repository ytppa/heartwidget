package com.ytppa.nothingheart;

public interface HeartRepository {
    int getSentBeatCount();

    int getReceivedBeatCount();

    void sendBeatToPartner();

    void simulateIncomingBeat();

    void simulateIncomingBeats(int amount);

    void clearReceivedBeats();

    void resetAllBeatCounts();

    HeartPairingState getPairingState();

    HeartPairingState ensureLocalIdentity();

    HeartPairingState requestPairing(String partnerPairCode);

    void syncPairing(PairingSyncCallback callback);

    HeartPairingState completeLocalPairing();

    HeartPairingState resetPairing();
}
