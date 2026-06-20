package com.ytppa.nothingheart;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class HeartFirebaseMessagingService extends FirebaseMessagingService {
    private static final String MESSAGE_TYPE_HEART_BEAT = "heart_beat";

    @Override
    public void onNewToken(String token) {
        HeartRepositories.get(this).syncPushToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String messageType = message.getData().get("type");
        if (MESSAGE_TYPE_HEART_BEAT.equals(messageType)) {
            HeartBeatSync.syncReceivedBeatsAndRefreshWidgets(this);
        }
    }
}
