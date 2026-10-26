# Alpme Gateway

A local, OpenAI-compatible gateway for [Alpme](https://alpme.com), packaged as a modern Android app.

Alpme Gateway runs a small HTTP server on your phone, holds multiple Alpme API keys, and exposes one stable endpoint to OpenCode, Cursor, scripts, or any OpenAI-compatible client. It rewrites the `User-Agent` Alpme expects, streams responses, rotates keys, retries overload errors, and shows every request in a live log.

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square" />
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Compose-Material%203-4285F4?style=flat-square" />
  <img alt="Min SDK" src="https://img.shields.io/badge/minSdk-26-3DDC84?style=flat-square" />
</p>

## Why

Alpme's API is OpenAI-compatible, but it can reject generic clients and intermittently return `503 server_overloaded`. This gateway:

- Sends `User-Agent: Alpme/1.0.0`, which Alpme accepts.
- Supports unlimited keys, each with an optional model override.
- Exposes a universal model plus a stable `/v1` endpoint.
- Retries `429/500/502/503/504` across keys with short exponential backoff.
- Optionally falls back to another model when Alpme is overloaded.
- Streams SSE responses straight through.
- Keeps request metadata in a live, in-memory log.

## Features

| Area | Details |
| --- | --- |
| Endpoint | `http://<phone-ip>:8787/v1` (configurable port) |
| API | `GET /v1/models`, `POST /v1/chat/completions`, `GET /health` |
| Keys | Unlimited, AES-256 encrypted with Android Keystore |
| Models | One universal model, optional per-key override, optional fallback |
| Routing | Round robin, priority failover, least used |
| Streaming | Server-Sent Events passed through |
| Security | Bearer token for clients, optional localhost-only binding |
| Service | Foreground service with optional boot autostart |
| Themes | Midnight Slate, Graphite, Warm Neutral |

## Install

1. Open the **Actions** tab of this repository.
2. Download the latest **alpme-gateway-debug** artifact.
3. Unzip it and install `app-debug.apk` on Android 8.0+.
4. Allow notifications so the foreground service can stay visible.

The debug APK is signed with the standard Android debug key. For a production build, add your own signing configuration.

## Setup

1. Open **Keys** and add one or more Alpme API keys.
2. Leave **Use universal model** on, or choose a model per key.
3. Open **Routing** and choose the universal model and fallback model.
4. Tap **Start** in the top bar.
5. Open the dashboard and tap **Connect agent**.

The gateway token is generated automatically. Use it as the API key in your client. It is not an Alpme key, and Alpme keys never leave the phone.

### OpenCode

Add a provider to `~/.config/opencode/opencode.json`:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "provider": {
    "alpme-gateway": {
      "npm": "@ai-sdk/openai-compatible",
      "name": "Alpme Gateway",
      "options": {
        "baseURL": "http://192.168.1.20:8787/v1",
        "apiKey": "sk_gw_your_token"
      },
      "models": {
        "auto": { "name": "Alpme Auto" }
      }
    }
  }
}
```

Use the LAN address shown in the app. The phone and computer must be on the same Wi-Fi, and **Allow LAN access** must be enabled.

### cURL

```bash
curl "http://192.168.1.20:8787/v1/chat/completions" \
  -H "Authorization: Bearer sk_gw_your_token" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "auto",
    "messages": [{"role": "user", "content": "hello"}]
  }'
```

Use `"stream": true` for SSE streaming.

## Routing behavior

For each request:

1. Read the requested model.
2. If the key has a model override, use it.
3. Otherwise use the universal model when **Force universal model** is on.
4. Try enabled keys according to the selected strategy, up to the configured attempt count.
5. On a retryable status, cycle to the next key and back off briefly before retrying.
6. If every key fails and fallback is enabled, try the fallback model across the keys.
7. If everything fails, return the last upstream status and an explanatory error body.

Fallback responses include:

```http
X-Alpme-Key: Personal
X-Alpme-Model: deepseek-v4.1-flash
X-Alpme-Fallback: true
```

The live log shows the key, requested model, resolved model, status, latency, attempts, and whether a fallback was used.

## Supported endpoints

| Endpoint | Behavior |
| --- | --- |
| `GET /v1/models` | Returns the universal model, fallback model, cached Alpme catalog, `auto`, and `universal` |
| `POST /v1/chat/completions` | Routes the request to Alpme with key rotation, retries, and streaming |
| `GET /health` | Returns status, endpoint, key count, version, uptime |
| `POST /v1/responses` | `404` — not supported by Alpme |
| `POST /v1/messages` | `404` — not supported by Alpme |

## Architecture

- **Kotlin + Jetpack Compose** UI, Material 3, IBM Plex Sans and JetBrains Mono.
- **NanoHTTPD** local OpenAI-compatible server.
- **OkHttp** upstream client with SSE streaming.
- **EncryptedSharedPreferences** for Alpme keys.
- **DataStore** for settings.
- **Foreground service** for background availability.
- **In-memory capped log store** (300 entries) exposed as a `StateFlow`.

```
app/src/main/java/com/vastobb/alpmegateway/
├── data/          settings, encrypted keys, model catalog, logs
├── net/           HTTP client, key tester, proxy engine
├── service/       foreground service and boot receiver
└── ui/            Compose theme, screens, components, view model
```

## Build locally

Requires JDK 17, Android SDK 35, and Gradle 8.11.1+.

```bash
gradle :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds it automatically on every push to `main`.

## Security notes

- Alpme keys are encrypted at rest using a key held by the Android Keystore.
- The gateway token is stored in DataStore and required for `/v1/*` calls. `/health` is unauthenticated.
- Binding to `0.0.0.0` exposes the gateway to your local network. Use a trusted Wi-Fi network and a strong token.
- Logs are in-memory only and capped at 300 requests.
- The app does not include analytics, ads, or a remote backend.
- Debug builds are not hardened against a determined attacker with physical access and a debuggable device.

## Limitations

- Alpme only supports `/v1/models` and `/v1/chat/completions`; this gateway does not fake unsupported OpenAI endpoints.
- Android may stop background services under aggressive battery optimization. Exempt the app if needed.
- Upstream capacity errors can still happen after all keys and the fallback model are exhausted.
- The universal model must be a model your Alpme account can access.

## License

MIT
