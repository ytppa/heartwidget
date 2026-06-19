package com.aga.nothingheart;

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

    HeartPairingState simulatePendingPairing();

    HeartPairingState simulatePairedPartner();

    HeartPairingState resetPairing();
}
