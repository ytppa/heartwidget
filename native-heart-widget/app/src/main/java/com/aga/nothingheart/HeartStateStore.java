package com.aga.nothingheart;

import android.content.Context;
import android.content.SharedPreferences;

public final class HeartStateStore {
    private static final String PREFS_NAME = "heart_state";
    private static final String KEY_LOCAL_BEAT_COUNT = "local_beat_count";

    private HeartStateStore() {
    }

    public static int getLocalBeatCount(Context context) {
        return prefs(context).getInt(KEY_LOCAL_BEAT_COUNT, 0);
    }

    public static int incrementLocalBeatCount(Context context) {
        return addLocalBeatCount(context, 1);
    }

    public static int addLocalBeatCount(Context context, int amount) {
        int nextCount = Math.max(0, getLocalBeatCount(context) + amount);
        prefs(context).edit().putInt(KEY_LOCAL_BEAT_COUNT, nextCount).apply();
        return nextCount;
    }

    public static void resetLocalBeatCount(Context context) {
        prefs(context).edit().putInt(KEY_LOCAL_BEAT_COUNT, 0).apply();
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

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
