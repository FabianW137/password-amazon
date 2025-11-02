package com.example.ask;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.slu.entityresolution.Resolution;
import com.amazon.ask.model.slu.entityresolution.Resolutions;
import com.amazon.ask.model.slu.entityresolution.StatusCode;
import com.amazon.ask.model.slu.entityresolution.ValueWrapper;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.amazon.ask.request.Predicates.intentName;

@org.springframework.stereotype.Component
public class AuthenticateIntentHandler implements RequestHandler {

    private final VoiceBackend backend;
    public AuthenticateIntentHandler(VoiceBackend backend) { this.backend = backend; }

    @Override public boolean canHandle(HandlerInput input) { return input.matches(intentName("AuthenticateIntent")); }

    // src/main/java/com/example/ask/AuthenticateIntentHandler.java
    private static final String AUTHENTICATE_INTENT = "AuthenticateIntent";
    private static final String SLOT_CODE = "OneTimeCode";   // <- angepasst
    private static final String SLOT_PIN  = "Pin";           // <- angepasst

    @Override
    public Optional<Response> handle(HandlerInput input) {
        var env = input.getRequestEnvelope();
        var ir  = (IntentRequest) env.getRequest();

        String codeSpoken = getSlotValue(ir, SLOT_CODE);
        String pinSpoken  = getSlotValue(ir, SLOT_PIN);

        // Nur Ziffern
        String code = codeSpoken == null ? "" : codeSpoken.replaceAll("\\D", "");
        String pin  = pinSpoken  == null ? "" : pinSpoken.replaceAll("\\D", "");

        if (code.length() != 6 || pin.length() < 4 || pin.length() > 8) {
            return input.getResponseBuilder()
                    .withSpeech("Bitte nenne zuerst den sechsstelligen Code und danach deine vierstellige PIN, jeweils Ziffer für Ziffer.")
                    .withReprompt("Wie lautet der Code und die PIN?")
                    .build();
        }

        String alexaUserId = safe(() -> env.getContext().getSystem().getUser().getUserId());
        String deviceId    = safe(() -> env.getContext().getSystem().getDevice().getDeviceId());

        Map<String,Object> res = backend.verify(code, pin, alexaUserId, deviceId);
        String msg = String.valueOf(res.getOrDefault("message", "error"));

        String codeSsml = "<say-as interpret-as=\"digits\">" + code + "</say-as>";
        String pinSsml  = "<say-as interpret-as=\"digits\">" + pin.charAt(0)+" "+pin.charAt(1)+" "+pin.charAt(2)+" "+pin.charAt(3) + "</say-as>";

        String speech;
        boolean end;

        switch (msg) {
            case "ok" -> {
                speech = "Anmeldung erfolgreich.";
                end = true;
            }
            case "bad-code" -> {
                speech = "Der Code " + codeSsml + " ist ungültig. Bitte nenne einen neuen sechsstelligen Code.";
                end = false;
            }
            case "no-link" -> {
                speech = "Dein Konto ist noch nicht verknüpft. Sage zum Verknüpfen: „Verknüpfe meinen Account mit Code …“.";
                end = true;
            }
            case "no-pin" -> {
                speech = "Für dein Konto ist keine Sprach-PIN hinterlegt. Lege zuerst in der App eine PIN fest und versuche es dann erneut.";
                end = true;
            }
            case "bad-pin" -> {
                speech = "Die PIN war leider falsch. Bitte wiederhole deine vierstellige PIN, Ziffer für Ziffer.";
                end = false;
            }
            case "locked" -> {
                speech = "Zu viele Fehlversuche. Dein Konto ist vorübergehend gesperrt. Versuche es später erneut.";
                end = true;
            }
            default -> {
                speech = "Da ist etwas schiefgelaufen. Bitte versuche es später erneut.";
                end = true;
            }
        }

        var rb = input.getResponseBuilder().withSimpleCard("Anmeldung", "Status: " + msg);
        if (end) {
            return rb.withSpeech(speech).withShouldEndSession(true).build();
        } else {
            return rb.withSpeech(speech)
                    .withReprompt("Bitte nenne den sechsstelligen Code und deine PIN, jeweils Ziffer für Ziffer.")
                    .withShouldEndSession(false)
                    .build();
        }
    }
    // Gibt null zurück, wenn beim Zugriff irgendwo NPE/IllegalState o.ä. fliegt
    private static <T> T safe(Supplier<T> expr) {
        try {
            return expr.get();
        } catch (NullPointerException | IllegalStateException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Variante mit Fallback
    private static <T> T safe(Supplier<T> expr, T fallback) {
        try {
            T v = expr.get();
            return (v != null) ? v : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
    private static String getSlotValue(IntentRequest ir, String slotName) {
        if (ir == null || ir.getIntent() == null) return null;
        Map<String, Slot> slots = ir.getIntent().getSlots();
        if (slots == null) return null;

        Slot s = slots.get(slotName);
        if (s == null) return null;

        // 1) Entity Resolution: wenn es einen erfolgreichen Match gibt, nimm den kanonischen Namen
        Resolutions res = s.getResolutions();
        if (res != null && res.getResolutionsPerAuthority() != null) {
            for (Resolution r : res.getResolutionsPerAuthority()) {
                if (r.getStatus() != null && r.getStatus().getCode() == StatusCode.ER_SUCCESS_MATCH
                        && r.getValues() != null && !r.getValues().isEmpty()) {
                    ValueWrapper vw = r.getValues().get(0);
                    if (vw != null && vw.getValue() != null && vw.getValue().getName() != null) {
                        return vw.getValue().getName();
                    }
                }
            }
        }

        // 2) Fallback: Rohwert des Slots
        String raw = s.getValue();
        return (raw == null || raw.isBlank()) ? null : raw;
    }


}
