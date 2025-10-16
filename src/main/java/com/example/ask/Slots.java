package com.example.ask;

import com.amazon.ask.model.IntentRequest;

class Slots {
    static String get(IntentRequest ir, String name) {
        try { return ir.getIntent().getSlots().get(name).getValue(); }
        catch (Exception e) { return null; }
    }
}
