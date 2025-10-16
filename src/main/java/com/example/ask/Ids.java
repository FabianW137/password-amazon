package com.example.ask;

import com.amazon.ask.model.RequestEnvelope;

class Ids {
    static String userId(RequestEnvelope env) {
        try { return env.getContext().getSystem().getUser().getUserId(); }
        catch (Exception e) { return "unknown-user"; }
    }
    static String deviceId(RequestEnvelope env) {
        try { return env.getContext().getSystem().getDevice().getDeviceId(); }
        catch (Exception e) { return "unknown-device"; }
    }
}
