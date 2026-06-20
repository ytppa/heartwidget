package com.ytppa.nothingheart;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;
import java.util.UUID;

public final class HeartStateStore {
    private static final String PREFS_NAME = "heart_state";
    private static final String KEY_LEGACY_LOCAL_BEAT_COUNT = "local_beat_count";
    private static final String KEY_LEGACY_LOCAL_BEAT_COUNT_MIGRATED = "local_beat_count_migrated";
    private static final String KEY_SENT_BEAT_COUNT = "sent_beat_count";
    private static final String KEY_RECEIVED_BEAT_COUNT = "received_beat_count";
    private static final String KEY_MY_USER_ID = "my_user_id";
    private static final String KEY_PAIR_CODE = "pair_code";
    private static final String KEY_PARTNER_ID = "partner_id";
    private static final String KEY_PARTNER_PAIR_CODE = "partner_pair_code";
    private static final String KEY_PAIR_STATUS = "pair_status";
    private static final int PAIR_CODE_LENGTH = 6;
    private static final String PAIR_CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final char[] PAIR_CODE_CHARS = PAIR_CODE_ALPHABET.toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

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

    public static HeartPairingState getPairingState(Context context) {
        return readPairingState(prefs(context));
    }

    public static HeartPairingState ensureLocalIdentity(Context context) {
        SharedPreferences preferences = prefs(context);
        ensureLocalIdentityExists(preferences);
        return readPairingState(preferences);
    }

    public static HeartPairingState setPairingPending(Context context, String partnerPairCode) {
        SharedPreferences preferences = prefs(context);
        ensureLocalIdentityExists(preferences);

        String normalizedPartnerPairCode = normalizePairCode(partnerPairCode);
        String ownPairCode = preferences.getString(KEY_PAIR_CODE, "");
        if (!isValidPairCode(normalizedPartnerPairCode) || normalizedPartnerPairCode.equals(ownPairCode)) {
            return readPairingState(preferences);
        }

        preferences.edit()
                .putString(KEY_PAIR_STATUS, HeartPairingStatus.PENDING.getStoredValue())
                .putString(KEY_PARTNER_PAIR_CODE, normalizedPartnerPairCode)
                .remove(KEY_PARTNER_ID)
                .apply();
        return readPairingState(preferences);
    }

    public static HeartPairingState completeLocalPairing(Context context) {
        SharedPreferences preferences = prefs(context);
        ensureLocalIdentityExists(preferences);

        String partnerPairCode = preferences.getString(KEY_PARTNER_PAIR_CODE, "");
        if (!isValidPairCode(partnerPairCode)) {
            return readPairingState(preferences);
        }

        preferences.edit()
                .putString(KEY_PAIR_STATUS, HeartPairingStatus.PAIRED.getStoredValue())
                .putString(KEY_PARTNER_ID, buildLocalPartnerId(partnerPairCode))
                .apply();
        return readPairingState(preferences);
    }

    public static HeartPairingState resetPairing(Context context) {
        SharedPreferences preferences = prefs(context);
        preferences.edit()
                .putString(KEY_PAIR_STATUS, HeartPairingStatus.NONE.getStoredValue())
                .remove(KEY_PARTNER_ID)
                .remove(KEY_PARTNER_PAIR_CODE)
                .apply();
        return readPairingState(preferences);
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

    public static String normalizePairCode(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(PAIR_CODE_LENGTH);
        for (int i = 0; i < value.length() && builder.length() < PAIR_CODE_LENGTH; i++) {
            char character = Character.toUpperCase(value.charAt(i));
            if (Character.isLetterOrDigit(character)) {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    public static boolean isValidPairCode(String value) {
        if (value == null || value.length() != PAIR_CODE_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (PAIR_CODE_ALPHABET.indexOf(value.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
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

    private static void ensureLocalIdentityExists(SharedPreferences preferences) {
        String myUserId = preferences.getString(KEY_MY_USER_ID, "");
        String pairCode = preferences.getString(KEY_PAIR_CODE, "");
        boolean hasPairStatus = preferences.contains(KEY_PAIR_STATUS);

        if (!isBlank(myUserId) && !isBlank(pairCode) && hasPairStatus) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        if (isBlank(myUserId)) {
            editor.putString(KEY_MY_USER_ID, generateUserId());
        }
        if (isBlank(pairCode)) {
            editor.putString(KEY_PAIR_CODE, generatePairCode());
        }
        if (!hasPairStatus) {
            editor.putString(KEY_PAIR_STATUS, HeartPairingStatus.NONE.getStoredValue());
        }
        editor.apply();
    }

    private static HeartPairingState readPairingState(SharedPreferences preferences) {
        String myUserId = preferences.getString(KEY_MY_USER_ID, "");
        String pairCode = preferences.getString(KEY_PAIR_CODE, "");
        String partnerId = preferences.getString(KEY_PARTNER_ID, "");
        String partnerPairCode = preferences.getString(KEY_PARTNER_PAIR_CODE, "");
        HeartPairingStatus pairStatus = HeartPairingStatus.fromStoredValue(
                preferences.getString(KEY_PAIR_STATUS, HeartPairingStatus.NONE.getStoredValue())
        );

        if (pairStatus == HeartPairingStatus.PENDING && !isValidPairCode(partnerPairCode)) {
            pairStatus = HeartPairingStatus.NONE;
        }
        if (pairStatus == HeartPairingStatus.PAIRED && isBlank(partnerId)) {
            pairStatus = HeartPairingStatus.NONE;
        }

        return new HeartPairingState(myUserId, pairCode, partnerId, partnerPairCode, pairStatus);
    }

    private static String generateUserId() {
        return "local-" + UUID.randomUUID();
    }

    private static String buildLocalPartnerId(String partnerPairCode) {
        return "local-partner-" + partnerPairCode;
    }

    private static String generatePairCode() {
        StringBuilder builder = new StringBuilder(PAIR_CODE_LENGTH);
        for (int i = 0; i < PAIR_CODE_LENGTH; i++) {
            builder.append(PAIR_CODE_CHARS[RANDOM.nextInt(PAIR_CODE_CHARS.length)]);
        }
        return builder.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
