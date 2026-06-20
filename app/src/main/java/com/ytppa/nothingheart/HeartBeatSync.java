package com.ytppa.nothingheart;

import android.content.Context;
import android.util.Log;

public final class HeartBeatSync {
    private static final String TAG = "HeartBeatSync";

    private HeartBeatSync() {
    }

    public static void syncReceivedBeatsAndRefreshWidgets(Context context) {
        Context appContext = context.getApplicationContext();
        HeartRepositories.get(appContext).syncReceivedBeats(new BeatSyncCallback() {
            @Override
            public void onBeatSyncComplete(int receivedBeatCount, boolean changed) {
                HeartWidgetProvider.refreshAllWidgets(appContext);
            }

            @Override
            public void onBeatSyncFailed(Exception exception) {
                Log.w(TAG, "Received beat sync failed.", exception);
            }
        });
    }
}
