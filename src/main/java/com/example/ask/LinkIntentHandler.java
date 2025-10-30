// src/main/java/com/example/ask/LinkIntentHandler.java
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

  @Override
  public boolean canHandle(HandlerInput input) {
    return input.matches(intentName("LinkIntent"));
  }

  @Override
  public Optional<Response> handle(HandlerInput input) {
    IntentRequest ir = (IntentRequest) input.getRequestEnvelope().getRequest();

    // 1) Slot auslesen
    String raw = Slots.get(ir, "LinkCode");

    // 2) Auf Ziffern normalisieren (alles Nicht-Ziffern entfernen)
    String code = (raw == null) ? null : raw.replaceAll("\\D", "");

    // 3) Validieren (6-stellig)
    if (code == null || code.length() != 6) {
      return input.getResponseBuilder()
              .withSpeech("Bitte nenne den sechsstelligen Verknüpfungs-Code, Ziffer für Ziffer.")
              .withReprompt("Wie lautet der sechsstellige Code?")
              .build();
    }

    String alexaUser = Ids.userId(input.getRequestEnvelope());
    boolean ok = backend.link(code, alexaUser);

    String speech = ok
            ? "Verknüpfung erfolgreich. Du kannst dich jetzt authentifizieren."
            : "Der Code " + code + " ist ungültig oder abgelaufen.";
    return input.getResponseBuilder().withSpeech(speech).build();
  }
}
