package com.aga.nothingheart;

import android.content.Context;

public final class HeartRepositories {
    private HeartRepositories() {
    }

    public static HeartRepository get(Context context) {
        return new LocalHeartRepository(context);
    }
}
