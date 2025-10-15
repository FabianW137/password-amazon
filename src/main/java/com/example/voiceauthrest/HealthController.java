package com.example.voiceauthrest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Object health() {
        return Map.of("status","ok","service","voiceauth-rest-one-domain");
    }
}
