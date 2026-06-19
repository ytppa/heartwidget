package com.aga.nothingheart;

import android.content.Context;
import android.content.SharedPreferences;

public final class HeartStateStore {
    private static final String PREFS_NAME = "heart_state";
    private static final String KEY_LEGACY_LOCAL_BEAT_COUNT = "local_beat_count";
    private static final String KEY_LEGACY_LOCAL_BEAT_COUNT_MIGRATED = "local_beat_count_migrated";
    private static final String KEY_SENT_BEAT_COUNT = "sent_beat_count";
    private static final String KEY_RECEIVED_BEAT_COUNT = "received_beat_count";

    private HeartStateStore() {
    }

    public static int getSentBeatCount(Context context) {
        migrateLegacyLocalCount(context);
        return prefs(context).getInt(KEY_SENT_BEAT_COUNT, 0);
    }

    public static int getReceivedBeatCount(Context context) {
        migrateLegacyLocalCount(context);
        return prefs(context).getInt(KEY_RECEIVED_BEAT_COUNT, 0);
    }

    public static int incrementSentBeatCount(Context context) {
        return addSentBeatCount(context, 1);
    }

    public static int addSentBeatCount(Context context, int amount) {
        return addBeatCount(context, KEY_SENT_BEAT_COUNT, amount);
    }

    public static int incrementReceivedBeatCount(Context context) {
        return addReceivedBeatCount(context, 1);
    }

    public static int addReceivedBeatCount(Context context, int amount) {
        return addBeatCount(context, KEY_RECEIVED_BEAT_COUNT, amount);
    }

    public static void resetReceivedBeatCount(Context context) {
        migrateLegacyLocalCount(context);
        prefs(context).edit().putInt(KEY_RECEIVED_BEAT_COUNT, 0).apply();
    }

    public static void resetAllBeatCounts(Context context) {
        prefs(context).edit()
                .putInt(KEY_LEGACY_LOCAL_BEAT_COUNT, 0)
                .putBoolean(KEY_LEGACY_LOCAL_BEAT_COUNT_MIGRATED, true)
                .putInt(KEY_SENT_BEAT_COUNT, 0)
                .putInt(KEY_RECEIVED_BEAT_COUNT, 0)
                .apply();
    }

    public static String formatBeatCount(int count) {
        if (count <= 0) {
            return "";
        }
        if (count < 1000) {
            return String.valueOf(count);
        }
        if (count < 10000) {
            return formatOneDecimal(count, 1000, "K");
        }
        if (count < 1000000) {
            return (count / 1000) + "K";
        }
        if (count < 10000000) {
            return formatOneDecimal(count, 1000000, "M");
        }
        if (count < 100000000) {
            return (count / 1000000) + "M";
        }
        return "99M+";
    }

    private static String formatOneDecimal(int count, int divisor, String suffix) {
        int whole = count / divisor;
        int decimal = (count % divisor) / (divisor / 10);

        if (decimal == 0) {
            return whole + suffix;
        }
        return whole + "." + decimal + suffix;
    }

    private static int addBeatCount(Context context, String key, int amount) {
        migrateLegacyLocalCount(context);

        SharedPreferences preferences = prefs(context);
        int currentCount = preferences.getInt(key, 0);
        int nextCount = clampCount((long) currentCount + amount);

        preferences.edit().putInt(key, nextCount).apply();
        return nextCount;
    }

    private static int clampCount(long count) {
        if (count <= 0) {
            return 0;
        }
        if (count > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) count;
    }

    private static void migrateLegacyLocalCount(Context context) {
        SharedPreferences preferences = prefs(context);
        if (preferences.getBoolean(KEY_LEGACY_LOCAL_BEAT_COUNT_MIGRATED, false)) {
            return;
        }

        int legacyCount = preferences.getInt(KEY_LEGACY_LOCAL_BEAT_COUNT, 0);
        SharedPreferences.Editor editor = preferences.edit()
                .putBoolean(KEY_LEGACY_LOCAL_BEAT_COUNT_MIGRATED, true);

        if (legacyCount > 0 && !preferences.contains(KEY_RECEIVED_BEAT_COUNT)) {
            editor.putInt(KEY_RECEIVED_BEAT_COUNT, legacyCount);
        }

        editor.apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
