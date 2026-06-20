package com.ytppa.nothingheart;

import android.content.Context;
import android.content.SharedPreferences;

final class SupabaseSessionStore {
    private static final String PREFS_NAME = "supabase_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EXPIRES_AT_MS = "expires_at_ms";
    private static final long REFRESH_SKEW_MS = 60_000L;

    private final SharedPreferences preferences;

    SupabaseSessionStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, "");
    }

    String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, "");
    }

    String getUserId() {
        return preferences.getString(KEY_USER_ID, "");
    }

    boolean hasValidAccessToken() {
        String accessToken = getAccessToken();
        long expiresAtMs = preferences.getLong(KEY_EXPIRES_AT_MS, 0L);
        return !isBlank(accessToken) && expiresAtMs > System.currentTimeMillis() + REFRESH_SKEW_MS;
    }

    boolean hasRefreshToken() {
        return !isBlank(getRefreshToken());
    }

    void save(String accessToken, String refreshToken, String userId, long expiresInSeconds) {
        long expiresAtMs = System.currentTimeMillis() + Math.max(0L, expiresInSeconds) * 1000L;
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, nullToEmpty(accessToken))
                .putString(KEY_REFRESH_TOKEN, nullToEmpty(refreshToken))
                .putString(KEY_USER_ID, nullToEmpty(userId))
                .putLong(KEY_EXPIRES_AT_MS, expiresAtMs)
                .apply();
    }

    void clear() {
        preferences.edit().clear().apply();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
