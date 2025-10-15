# VoiceAuth REST (eine Domain)
Ein einfacher REST-Dienst (Spring Boot 3, Java 21) mit statischer HTML/JS-Oberfläche — keine Angular/GraphQL-Abhängigkeiten.

## Endpunkte
- `POST /api/pin` → Body `{ "pin": "1234" }`
- `POST /api/challenge` → erzeugt 4-stelligen Code (TTL 120s) → `{ code, expiresAt }`
- `POST /api/pairing` → erzeugt Pairing-Code (TTL 10min) → `{ pairingCode }`
- `POST /api/link` → Body `{ "pairingCode":"...", "alexaUserId":"..." }` → `{ ok }`
- `POST /api/verify` → Body `{ "code":"0001","pin":"1234","alexaUserId":"alexa-user-xyz","deviceId":"echo-1" }` → `{ success, message }`
- `GET /health` → `{ status: "ok" }`

## Lokal
```bash
mvn spring-boot:run
# Öffne http://localhost:8080
```

## Render (Docker Web Service)
- Neues Web Service → **Docker** → dieses Repo/ZIP deployen.
- Nach Deploy: UI unter `/`, API unter `/api/*`.

## Sicherheit
Dies ist eine **in-memory Demo**. Für Produktion: DB, Rate-Limits, Logging, OAuth Account Linking.
