package com.example.alexahttps;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@RestController
public class AlexaController {

  private final AlexaSignatureVerifier verifier;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
  private final String baseUrl;

  public AlexaController(AlexaSignatureVerifier verifier, @Value("${app.base-url}") String baseUrl) {
    this.verifier = verifier;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
  }

  @PostMapping(value = "/alexa", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<byte[]> handle(@RequestHeader("Signature") String signature,
                                       @RequestHeader("SignatureCertChainUrl") String certUrl,
                                       @RequestBody byte[] body) throws Exception {
    // Verify signature & timestamp/appId
    verifier.verify(certUrl, signature, body);
    verifier.assertAppAndTimestamp(body);

    // Parse request envelope
    AlexaDtos.RequestEnvelope env = mapper.readValue(body, AlexaDtos.RequestEnvelope.class);
    String type = env.request != null ? env.request.type : null;
    String intent = env.request != null && env.request.intent != null ? env.request.intent.name : null;

    AlexaDtos.OutputSpeechResponse out;

    if ("LaunchRequest".equals(type)) {
      String s = "Willkommen beim Passwortmanager. Sage: verknüpfe meinen Account mit Code, oder: authentifiziere mich.";
      out = new AlexaDtos.OutputSpeechResponse(s, s);
    } else if ("IntentRequest".equals(type)) {
      if ("AMAZON.HelpIntent".equals(intent)) {
        String s = "Du kannst sagen: verknüpfe meinen Account mit Code. Oder: authentifiziere mich.";
        out = new AlexaDtos.OutputSpeechResponse(s, s);
      } else if ("AMAZON.CancelIntent".equals(intent) || "AMAZON.StopIntent".equals(intent)) {
        out = new AlexaDtos.OutputSpeechResponse("Bis bald.", null);
      } else if ("LinkIntent".equals(intent)) {
        String linkCode = getSlot(env, "LinkCode");
        if (linkCode == null || linkCode.isBlank()) {
          out = new AlexaDtos.OutputSpeechResponse("Bitte nenne den Verknüpfungs-Code.", "Wie lautet der Verknüpfungs-Code?");
        } else {
          boolean ok = callLink(linkCode, getUserId(env));
          String s = ok ? "Verknüpfung erfolgreich. Du kannst dich jetzt authentifizieren." : "Der Code ist ungültig oder abgelaufen.";
          out = new AlexaDtos.OutputSpeechResponse(s, null);
        }
      } else if ("AuthenticateIntent".equals(intent)) {
        String code = getSlot(env, "OneTimeCode");
        String pin  = getSlot(env, "Pin");
        if (isBlank(code) || isBlank(pin)) {
          out = new AlexaDtos.OutputSpeechResponse("Bitte nenne zuerst den vierstelligen Code und dann deine PIN.", "Bitte nenne Code und PIN.");
        } else {
          Map<String,Object> res = callVerify(code, pin, getUserId(env), getDeviceId(env));
          boolean success = Boolean.TRUE.equals(res.get("success"));
          String message = (String) res.get("message");
          if (success) out = new AlexaDtos.OutputSpeechResponse("Authentifizierung erfolgreich.", null);
          else {
            String t = "locked".equals(message) ? "Zu viele Fehlversuche. Versuche es später erneut." : "Authentifizierung fehlgeschlagen. Versuche es erneut.";
            out = new AlexaDtos.OutputSpeechResponse(t, "Bitte nenne Code und PIN.");
          }
        }
      } else {
        out = new AlexaDtos.OutputSpeechResponse("Das habe ich nicht verstanden.", "Wie kann ich helfen?");
      }
    } else {
      out = new AlexaDtos.OutputSpeechResponse("Das habe ich nicht verstanden.", "Wie kann ich helfen?");
    }

    byte[] resp = mapper.writeValueAsBytes(out);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
  }

  private String getSlot(AlexaDtos.RequestEnvelope env, String name) {
    try {
      return env.request.intent.slots.get(name).value;
    } catch (Exception e) {
      return null;
    }
  }
  private String getUserId(AlexaDtos.RequestEnvelope env) {
    try { return env.context.System.user.userId; } catch (Exception e) { return "unknown-user"; }
  }
  private String getDeviceId(AlexaDtos.RequestEnvelope env) {
    try { return env.context.System.device.deviceId; } catch (Exception e) { return "unknown-device"; }
  }
  private boolean isBlank(String s){ return s == null || s.isBlank(); }

  private boolean callLink(String pairingCode, String alexaUserId) {
    try {
      var req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/link"))
          .timeout(Duration.ofSeconds(5))
          .header("Content-Type","application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{"pairingCode":""+escape(pairingCode)+"","alexaUserId":""+escape(alexaUserId)+""}", StandardCharsets.UTF_8))
          .build();
      var res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
      if (res.statusCode() != 200) return false;
      var map = mapper.readValue(res.body(), Map.class);
      Object ok = map.get("ok");
      return ok instanceof Boolean ? (Boolean) ok : false;
    } catch (Exception e) {
      return false;
    }
  }

  private Map<String,Object> callVerify(String code, String pin, String alexaUserId, String deviceId) {
    try {
      String json = String.format("{"code":"%s","pin":"%s","alexaUserId":"%s","deviceId":"%s"}",
          escape(code), escape(pin), escape(alexaUserId), escape(deviceId));
      var req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/verify"))
          .timeout(Duration.ofSeconds(5))
          .header("Content-Type","application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
          .build();
      var res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
      if (res.statusCode() != 200) return Map.of("success", false, "message", "http "+res.statusCode());
      return mapper.readValue(res.body(), Map.class);
    } catch (Exception e) {
      return Map.of("success", false, "message", "error");
    }
  }

  private String escape(String s) {
    return s.replace("\", "\\").replace(""","\"");
  }
}
