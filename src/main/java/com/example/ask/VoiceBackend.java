package com.example.ask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class VoiceBackend {

    private static final String LINK_PATH   = "/api/voice/link/complete";
    private static final String VERIFY_PATH = "/api/voice/verify";

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;


    public VoiceBackend(@Value("${voice.backend.base-url}") String baseUrl) {
        this.baseUrl = sanitizeBaseUrl(baseUrl);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
    }


    public Map<String, Object> link(String code, String alexaUserId) {
        try {
            byte[] json = mapper.writeValueAsBytes(Map.of(
                    "code", code,
                    "alexaUserId", alexaUserId
            ));

            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + LINK_PATH))
                    .timeout(Duration.ofSeconds(7))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json))
                    .build();

            HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            return toResponseMap(res);
        } catch (Exception e) {
            return Map.of("ok", false, "message", "network-error");
        }
    }


    public Map<String, Object> verify(String code, String pin, String alexaUserId, String deviceId) {
        try {
            byte[] json = mapper.writeValueAsBytes(Map.of(
                    "code", code,
                    "pin", pin,
                    "alexaUserId", alexaUserId,
                    "deviceId", deviceId
            ));

            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + VERIFY_PATH))
                    .timeout(Duration.ofSeconds(7))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json))
                    .build();

            HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            return toResponseMap(res);
        } catch (Exception e) {
            return Map.of("ok", false, "message", "network-error");
        }
    }

    // -------------------- interne Hilfen --------------------

    private static String sanitizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be null/blank");
        }
        String trimmed = url.trim();
        // abschließenden Slash entfernen
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }


    private Map<String, Object> toResponseMap(HttpResponse<byte[]> res) throws Exception {
        if (res.statusCode() != 200) {
            return Map.of("ok", false, "message", "http-" + res.statusCode());
        }

        byte[] body = res.body();
        if (body == null || body.length == 0) {
            return Map.of("ok", false, "message", "empty-body");
        }

        String asText = new String(body).trim();

        // Legacy: nacktes "true"/"false"
        if (!asText.startsWith("{")) {
            boolean ok = "true".equalsIgnoreCase(asText);
            return Map.of("ok", ok, "message", ok ? "ok" : "legacy-boolean");
        }

        // Normale JSON-Map
        Map<String, Object> parsed = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        // Fallback-Sicherung: ok/message sicherstellen
        Object okObj = parsed.get("ok");
        Object msgObj = parsed.get("message");

        boolean ok = (okObj instanceof Boolean) ? (Boolean) okObj : false;
        String message = (msgObj instanceof String) ? (String) msgObj : (ok ? "ok" : "error");

        // Original-Map plus garantierte Felder ok/message zurückgeben
        if (!parsed.containsKey("ok") || !parsed.containsKey("message")) {
            // neues Immutable-Map-Objekt bauen
            return new java.util.HashMap<String, Object>(parsed) {{
                put("ok", ok);
                put("message", message);
            }};
        }
        return parsed;
    }
}
