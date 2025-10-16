package com.example.ask;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

@org.springframework.stereotype.Component
public class LinkIntentHandler implements RequestHandler {

  private final VoiceBackend backend;
  public LinkIntentHandler(VoiceBackend backend) { this.backend = backend; }

  @Override public boolean canHandle(HandlerInput input) { return input.matches(intentName("LinkIntent")); }

  @Override public Optional<Response> handle(HandlerInput input) {
    var ir = (IntentRequest) input.getRequestEnvelope().getRequest();
    String code = Slots.get(ir, "LinkCode");
    if (code == null || code.isBlank()) {
      return input.getResponseBuilder()
              .withSpeech("Bitte nenne den Verknüpfungs Code.")
              .withReprompt("Wie lautet der Verknüpfungs Code?")
              .build();
    }

    String alexaUser = Ids.userId(input.getRequestEnvelope());
    boolean ok = backend.link(code, alexaUser);

    String speech = ok ? "Verknüpfung erfolgreich. Du kannst dich jetzt authentifizieren."
            : "Der Code ist ungültig oder abgelaufen.";
    return input.getResponseBuilder().withSpeech(speech).build();
  }
}
