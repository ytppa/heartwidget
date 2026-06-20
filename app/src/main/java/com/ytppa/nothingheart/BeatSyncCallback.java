package com.ytppa.nothingheart;

public interface BeatSyncCallback {
    void onBeatSyncComplete(int receivedBeatCount, boolean changed);

    void onBeatSyncFailed(Exception exception);
}
