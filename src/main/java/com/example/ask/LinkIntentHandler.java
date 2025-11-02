// src/main/java/com/example/ask/LinkIntentHandler.java
package com.example.ask;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import java.util.Map;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

@org.springframework.stereotype.Component
public class LinkIntentHandler implements RequestHandler {

  private final VoiceBackend backend;

  public LinkIntentHandler(VoiceBackend backend) {
    this.backend = backend;
  }

  @Override
  public boolean canHandle(HandlerInput input) {
    return input.matches(intentName("LinkIntent"));
  }

  // src/main/java/com/example/ask/LinkIntentHandler.java
  @Override
  public Optional<Response> handle(HandlerInput input) {
    IntentRequest ir = (IntentRequest) input.getRequestEnvelope().getRequest();

    String raw = Slots.get(ir, "LinkCode");
    String code = (raw == null) ? null : raw.replaceAll("\\D", "");

    if (code == null || code.length() != 6) {
      return input.getResponseBuilder()
              .withSpeech("Bitte nenne den sechsstelligen Verknüpfungs-Code, Ziffer für Ziffer.")
              .withReprompt("Wie lautet der sechsstellige Code?")
              .build();
    }

    String alexaUser = Ids.userId(input.getRequestEnvelope());
    Map<String, Object> res = backend.link(code, alexaUser);
    String msg = String.valueOf(res.getOrDefault("message", "error"));

    String codeSsml = "<say-as interpret-as=\"digits\">" + code + "</say-as>";
    String speech;
    boolean end = false;

    switch (msg) {
      case "ok" -> {
        speech = "Verknüpfung erfolgreich. Du kannst dich jetzt anmelden.";
        end = true;
      }
      case "bad-code" -> {
        speech = "Der Code " + codeSsml + " ist ungültig. Bitte nenne einen neuen sechsstelligen Code.";
      }
      case "expired" -> {
        speech = "Der Code ist abgelaufen. Erzeuge in der App einen neuen Code und nenne ihn Ziffer für Ziffer.";
      }
      case "alexa-id-already-linked" -> {
        speech = "Diese Alexa-ID ist bereits mit einem anderen Konto verknüpft. Bitte löse dort die Verknüpfung, oder melde dich mit dem passenden Konto an.";
        end = true;
      }
      case "alexa-id-missing" -> {
        speech = "Ich konnte deine Alexa-ID nicht lesen. Versuche es bitte noch einmal.";
      }
      case "http-500", "http-503", "network-error" -> {
        speech = "Der Server ist gerade nicht erreichbar. Bitte versuche es später erneut.";
        end = true;
      }
      default -> {
        speech = "Da ist etwas schiefgelaufen. Bitte probiere es später erneut.";
        end = true;
      }
    }

    var rb = input.getResponseBuilder().withSimpleCard("Verknüpfung", "Status: " + msg);

    if (end) {
      return rb.withSpeech(speech).withShouldEndSession(true).build();
    } else {
      return rb.withSpeech(speech)
              .withReprompt("Wie lautet der neue sechsstellige Code?")
              .withShouldEndSession(false)
              .build();
    }
  }
}
