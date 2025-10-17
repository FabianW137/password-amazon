package com.example.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
public class VoiceBackend {
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public VoiceBackend(@Value("${app.base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
    }

    public boolean link(String pairingCode, String alexaUserId) {
        try {
            byte[] json = mapper.writeValueAsBytes(Map.of("pairingCode", pairingCode, "alexaUserId", alexaUserId));
            var req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/link"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json))
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) return false;
            Map<?,?> m = mapper.readValue(res.body(), Map.class);
            Object ok = m.get("ok");
            return (ok instanceof Boolean) ? (Boolean) ok : false;
        } catch (Exception e) { return false; }
    }

    public Map<String,Object> verify(String code, String pin, String alexaUserId, String deviceId) {
        try {
            byte[] json = mapper.writeValueAsBytes(Map.of(
                    "code", code, "pin", pin, "alexaUserId", alexaUserId, "deviceId", deviceId
            ));
            var req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/verify"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json))
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) return Map.of("success", false, "message", "http "+res.statusCode());
            return mapper.readValue(res.body(), Map.class);
        } catch (Exception e) {
            return Map.of("success", false, "message", "error");
        }
    }
}
