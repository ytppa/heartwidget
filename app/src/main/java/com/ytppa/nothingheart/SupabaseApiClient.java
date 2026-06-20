package com.ytppa.nothingheart;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class SupabaseApiClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private final SupabaseSessionStore sessionStore;
    private final String baseUrl;
    private final String anonKey;

    SupabaseApiClient(Context context) {
        sessionStore = new SupabaseSessionStore(context);
        baseUrl = SupabaseConfig.getUrl();
        anonKey = SupabaseConfig.getAnonKey();
    }

    synchronized String ensureAccessToken() throws IOException, JSONException {
        if (sessionStore.hasValidAccessToken()) {
            return sessionStore.getAccessToken();
        }

        if (sessionStore.hasRefreshToken()) {
            try {
                return refreshSession();
            } catch (IOException | JSONException exception) {
                sessionStore.clear();
            }
        }

        return signInAnonymously();
    }

    String rpc(String functionName, JSONObject body) throws IOException, JSONException {
        String accessToken = ensureAccessToken();
        return request(
                "POST",
                "/rest/v1/rpc/" + functionName,
                body == null ? new JSONObject() : body,
                accessToken
        );
    }

    private String signInAnonymously() throws IOException, JSONException {
        String response = request("POST", "/auth/v1/signup", new JSONObject().put("data", new JSONObject()), "");
        JSONObject json = new JSONObject(response);
        saveSession(json);
        return sessionStore.getAccessToken();
    }

    private String refreshSession() throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("refresh_token", sessionStore.getRefreshToken());
        String response = request("POST", "/auth/v1/token?grant_type=refresh_token", body, "");
        JSONObject json = new JSONObject(response);
        saveSession(json);
        return sessionStore.getAccessToken();
    }

    private void saveSession(JSONObject json) {
        JSONObject user = json.optJSONObject("user");
        sessionStore.save(
                json.optString("access_token", ""),
                json.optString("refresh_token", ""),
                user == null ? "" : user.optString("id", ""),
                json.optLong("expires_in", 3600L)
        );
    }

    private String request(String method, String path, JSONObject body, String accessToken) throws IOException {
        if (!SupabaseConfig.isConfigured()) {
            throw new IOException("Supabase is not configured.");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod(method);
        connection.setRequestProperty("apikey", anonKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        if (!isBlank(accessToken)) {
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        }
        connection.setDoInput(true);

        if (body != null) {
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }

        int statusCode = connection.getResponseCode();
        String responseBody = readResponseBody(statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream());
        connection.disconnect();

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Supabase HTTP " + statusCode + ": " + responseBody);
        }

        return responseBody;
    }

    private static String readResponseBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
