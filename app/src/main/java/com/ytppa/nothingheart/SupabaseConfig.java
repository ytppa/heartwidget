package com.ytppa.nothingheart;

public final class SupabaseConfig {
    private SupabaseConfig() {
    }

    public static boolean isConfigured() {
        return !isBlank(getUrl()) && !isBlank(getAnonKey());
    }

    public static String getUrl() {
        return trimTrailingSlash(BuildConfig.SUPABASE_URL);
    }

    public static String getAnonKey() {
        return BuildConfig.SUPABASE_ANON_KEY.trim();
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
