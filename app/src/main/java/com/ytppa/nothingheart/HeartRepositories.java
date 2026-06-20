package com.ytppa.nothingheart;

import android.content.Context;

public final class HeartRepositories {
    private HeartRepositories() {
    }

    public static HeartRepository get(Context context) {
        if (SupabaseConfig.isConfigured()) {
            return new SupabaseHeartRepository(context);
        }
        return new FirebaseHeartRepository(context);
    }
}
