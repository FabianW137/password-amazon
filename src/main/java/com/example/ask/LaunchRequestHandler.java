package com.example.ask;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;

import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;

@org.springframework.stereotype.Component
public class LaunchRequestHandler implements RequestHandler {
  @Override public boolean canHandle(HandlerInput input) {
    return input.matches(requestType(LaunchRequest.class));
  }

  @Override public Optional<Response> handle(HandlerInput input) {
    String speech = "Willkommen beim Passwortmanager. Sage: verknüpfe meinen Account mit Code. "
            + "Oder: authentifiziere mich mit Code und PIN.";
    return input.getResponseBuilder()
            .withSpeech(speech)
            .withReprompt("Wie kann ich helfen?")
            .build();
  }
}
