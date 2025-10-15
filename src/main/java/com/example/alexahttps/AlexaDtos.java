package com.example.alexahttps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlexaDtos {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RequestEnvelope {
    public String version;
    public Session session;
    public Context context;
    public Request request;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Session {
    public Application application;
    public String sessionId;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Application {
    public String applicationId;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Context {
    public SystemContext System;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SystemContext {
    public User user;
    public Device device;
    public Application application;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class User { public String userId; }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Device { public String deviceId; }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Request {
    public String type;
    public String requestId;
    public Intent intent;
    public String locale;
    public OffsetDateTime timestamp;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Intent {
    public String name;
    public Map<String, Slot> slots;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Slot {
    public String name;
    public String value;
  }

  // Response
  public static class PlainSpeech {
    public String type = "PlainText";
    public String text;
    public PlainSpeech(String t) { this.text = t; }
  }

  public static class OutputSpeechResponse {
    public String version = "1.0";
    public Response response;
    public OutputSpeechResponse(String text, String reprompt) {
      this.response = new Response();
      this.response.outputSpeech = new PlainSpeech(text);
      if (reprompt != null) this.response.reprompt = new Reprompt(new PlainSpeech(reprompt));
      this.response.shouldEndSession = (reprompt == null);
    }
  }

  public static class Response {
    public PlainSpeech outputSpeech;
    public Reprompt reprompt;
    public boolean shouldEndSession;
  }
  public static class Reprompt {
    public PlainSpeech outputSpeech;
    public Reprompt(PlainSpeech s){ this.outputSpeech = s; }
  }
}
