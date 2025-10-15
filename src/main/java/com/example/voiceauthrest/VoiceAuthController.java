package com.example.voiceauthrest;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoiceAuthController {
    private final VoiceAuthService service;
    public VoiceAuthController(VoiceAuthService service) { this.service = service; }

    @PostMapping(value = "/pin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object setPin(@Valid @RequestBody Models.SetPinReq req) {
        service.setPin(service.currentUserId(), req.pin());
        return Map.of("ok", true);
    }

    @PostMapping(value = "/challenge", produces = MediaType.APPLICATION_JSON_VALUE)
    public Models.ChallengeRes challenge() { return service.createChallenge(service.currentUserId()); }

    @PostMapping(value = "/pairing", produces = MediaType.APPLICATION_JSON_VALUE)
    public Models.PairingRes pairing() { return service.createPairing(service.currentUserId()); }

    @PostMapping(value = "/link", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object link(@Valid @RequestBody Models.LinkReq req) { return Map.of("ok", service.link(req.pairingCode(), req.alexaUserId())); }

    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Models.VerifyRes verify(@Valid @RequestBody Models.VerifyReq req) { return service.verify(req.alexaUserId(), req.code(), req.pin(), req.deviceId()); }
}
