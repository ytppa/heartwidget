package com.ytppa.nothingheart;

public interface PairingSyncCallback {
    void onPairingSyncComplete(HeartPairingState pairingState, boolean changed);

    void onPairingSyncFailed(Exception exception);
}
