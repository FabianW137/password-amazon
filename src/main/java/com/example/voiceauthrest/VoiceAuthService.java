package com.example.voiceauthrest;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class VoiceAuthService {
    private final SecureRandom rng = new SecureRandom();
    private final Map<String, String> userPinHash = new HashMap<>();
    private final Map<String, Models.VoiceChallenge> challenges = new HashMap<>();
    private final Map<String, Models.PairingCode> pairingCodes = new HashMap<>();
    private final Map<String, String> alexaLinks = new HashMap<>();
    private final Map<String, Integer> failCounters = new HashMap<>();
    private final Map<String, Instant> locks = new HashMap<>();

    public String currentUserId() { return "user-1"; }

    public void setPin(String userId, String pin) { userPinHash.put(userId, BCrypt.hashpw(pin, BCrypt.gensalt(10))); }

    public Models.ChallengeRes createChallenge(String userId) {
        int codeInt = rng.nextInt(10_000);
        String codePlain = String.format("%04d", codeInt);
        Models.VoiceChallenge ch = new Models.VoiceChallenge();
        ch.id = UUID.randomUUID();
        ch.userId = userId;
        ch.codeHash = BCrypt.hashpw(codePlain, BCrypt.gensalt(10));
        ch.expiresAt = Instant.now().plusSeconds(120);
        ch.usedAt = null;
        ch.attemptCount = 0;
        challenges.put(userId, ch);
        return new Models.ChallengeRes(codePlain, ch.expiresAt.toString());
    }

    public Models.PairingRes createPairing(String userId) {
        String code = String.format("%06d", rng.nextInt(1_000_000));
        Models.PairingCode pc = new Models.PairingCode();
        pc.code = code;
        pc.userId = userId;
        pc.expiresAt = Instant.now().plusSeconds(600);
        pairingCodes.put(code, pc);
        return new Models.PairingRes(code);
    }

    public boolean link(String pairingCode, String alexaUserId) {
        Models.PairingCode pc = pairingCodes.get(pairingCode);
        if (pc == null || pc.usedAt != null || Instant.now().isAfter(pc.expiresAt)) return false;
        alexaLinks.put(alexaUserId, pc.userId);
        pc.usedAt = Instant.now();
        return true;
    }

    public Models.VerifyRes verify(String alexaUserId, String code, String pin, String deviceId) {
        Instant now = Instant.now();
        Instant until = locks.get(alexaUserId);
        if (until != null && now.isBefore(until)) return new Models.VerifyRes(false, "locked");
        String userId = alexaLinks.get(alexaUserId);
        if (userId == null) return new Models.VerifyRes(false, "not linked");

        Models.VoiceChallenge ch = challenges.get(userId);
        if (ch == null || ch.usedAt != null || now.isAfter(ch.expiresAt)) {
            registerFail(alexaUserId);
            return new Models.VerifyRes(false, "no active challenge");
        }
        String pinHash = userPinHash.get(userId);
        boolean pinOk = pinHash != null && BCrypt.checkpw(pin, pinHash);
        boolean codeOk = BCrypt.checkpw(code, ch.codeHash);

        if (pinOk && codeOk) {
            ch.usedAt = now;
            resetFail(alexaUserId);
            return new Models.VerifyRes(true, "ok");
        } else {
            ch.attemptCount += 1;
            registerFail(alexaUserId);
            return new Models.VerifyRes(false, "invalid");
        }
    }

    private void registerFail(String alexaUserId) {
        int n = failCounters.getOrDefault(alexaUserId, 0) + 1;
        failCounters.put(alexaUserId, n);
        if (n >= 5) {
            locks.put(alexaUserId, Instant.now().plus(Duration.ofMinutes(15)));
            failCounters.put(alexaUserId, 0);
        }
    }
    private void resetFail(String alexaUserId) {
        failCounters.put(alexaUserId, 0);
        locks.put(alexaUserId, null);
    }
}
