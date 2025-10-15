package com.example.voiceauthrest;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public class Models {
    public static class VoiceChallenge {
        public UUID id;
        public String userId;
        public String codeHash;
        public Instant expiresAt;
        public Instant usedAt;
        public int attemptCount;
    }
    public static class PairingCode {
        public String code;
        public String userId;
        public Instant expiresAt;
        public Instant usedAt;
    }

    public record SetPinReq(@NotBlank String pin) {}
    public record LinkReq(@NotBlank String pairingCode, @NotBlank String alexaUserId) {}
    public record VerifyReq(@NotBlank String code, @NotBlank String pin, @NotBlank String alexaUserId, String deviceId) {}

    public record ChallengeRes(String code, String expiresAt) {}
    public record PairingRes(String pairingCode) {}
    public record OkRes(boolean ok) {}
    public record VerifyRes(boolean success, String message) {}
}
