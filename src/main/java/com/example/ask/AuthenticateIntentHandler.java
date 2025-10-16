package com.example.ask;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

@org.springframework.stereotype.Component
public class AuthenticateIntentHandler implements RequestHandler {

    private final VoiceBackend backend;
    public AuthenticateIntentHandler(VoiceBackend backend) { this.backend = backend; }

    @Override public boolean canHandle(HandlerInput input) { return input.matches(intentName("AuthenticateIntent")); }

    @Override public Optional<Response> handle(HandlerInput input) {
        var env = input.getRequestEnvelope();
        var ir  = (IntentRequest) env.getRequest();

        String code = Slots.get(ir, "OneTimeCode");
        String pin  = Slots.get(ir, "Pin");

        if (code == null || pin == null || code.isBlank() || pin.isBlank()) {
            return input.getResponseBuilder()
                    .withSpeech("Bitte nenne zuerst den vierstelligen Code und dann deine PIN.")
                    .withReprompt("Bitte nenne Code und PIN.")
                    .build();
        }

        String userId   = Ids.userId(env);
        String deviceId = Ids.deviceId(env);

        var res = backend.verify(code, pin, userId, deviceId);
        boolean success = Boolean.TRUE.equals(res.get("success"));
        String message  = (String) res.get("message");

        String speech;
        if (success)      speech = "Authentifizierung erfolgreich.";
        else if ("locked".equals(message)) speech = "Zu viele Fehlversuche. Versuche es später erneut.";
        else              speech = "Authentifizierung fehlgeschlagen. Versuche es erneut.";

        return input.getResponseBuilder().withSpeech(speech).build();
    }
}
