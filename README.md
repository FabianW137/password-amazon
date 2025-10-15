# Alexa HTTPS Endpoint (Java, Spring Boot)

Direkter Alexa-Endpoint **ohne Lambda**, inkl. **Signatur- und Zertifikatsprüfung**. Der Endpoint verarbeitet
`LaunchRequest`, `LinkIntent`, `AuthenticateIntent` und spricht mit deinem REST-Backend (`BASE_URL`).

## Konfiguration (Env Vars)
- `PORT` (Render) – wird automatisch gesetzt
- `BASE_URL` – z. B. `https://dein-service.onrender.com` (ohne abschließenden Slash)
- `ALEXA_SKILL_ID` – deine Skill-ID (prüft `applicationId`)
- Optional: `app.timestamp-tolerance` (Sekunden, Default 150)

## Deploy
- Als Docker Web Service deployen. HTTPS kommt über deinen Provider (z. B. Render + Custom Domain).
- Alexa Developer Console → Endpoint: **HTTPS** → URL `https://deine-domain/alexa`

## Anforderungen (Alexa)
- Öffentliche Domain mit gültigem CA-Zertifikat (z. B. Let’s Encrypt).
- Endpoint **muss** die Signatur prüfen (dieser Service macht das).
- Akzeptiere nur `applicationId` deiner Skill-ID.

## Lokaler Test
```bash
mvn spring-boot:run
# POST http://localhost:8080/alexa  (ohne Signaturprüfung nur, wenn du die Verifier Config überschreibst)
```

## Endpunkt
- `POST /alexa` – Alexa Request Envelope (JSON), prüft Header `Signature` und `SignatureCertChainUrl`.
