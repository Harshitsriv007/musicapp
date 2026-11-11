# Dhun – Bollywood music streaming app

Play free Bollywood music. The app keeps your **history** and **liked songs**, and a **New songs** list that updates itself. It installs as an app on phones and laptops, and keeps playing when minimised, with play / pause / next / previous / stop controls on the lock screen and in the notification shade.

```
 Audius API ──(cron, every 15 min)──▶ SongIngestionService ──▶ Kafka topic music.songs.discovered
                                                                     │  (async, keyed per song, DLT on failure)
 React PWA ◀── REST /api/** + SSE /api/songs/stream ◀── SongEventConsumer ──▶ MySQL (Flyway)
```

| Part | Tech |
|---|---|
| Backend | Java 21, Spring Boot 3.4, Spring Data JPA, Spring Kafka, Flyway, virtual threads |
| DB / messaging | MySQL 8.4, Kafka 3.8 (KRaft), Kafka UI |
| Frontend | React 18 + TypeScript + Vite, installable PWA |
| Music source | [Audius](https://audius.co) open API (free, no key). Optional: [Jamendo](https://developer.jamendo.com) (free client id) |

## Run it

```bash
docker compose up -d --build
```

| URL | What |
|---|---|
| http://localhost:3000 | The app |
| http://localhost:8080/api/songs/new | REST API |
| http://localhost:8090 | Kafka UI (topic `music.songs.discovered`) |

The first fetch runs a few seconds after startup, then on the cron schedule. **Check now** in the app triggers a fetch straight away.

Settings (environment variables / `.env`):

| Variable | Default |
|---|---|
| `INGESTION_CRON` | `0 */15 * * * *` (`-` disables) |
| `INGESTION_QUERIES` | `bollywood,hindi songs,arijit singh,bollywood romantic,punjabi` |
| `JAMENDO_ENABLED` / `JAMENDO_CLIENT_ID` | `false` / empty |

## Playback when minimised, mini player, mobile app

- **Background playback:** one `<audio>` element lives for the whole app session, so music keeps playing when you switch tabs, minimise the window or lock the phone.
- **Small player outside the app:** the [Media Session API](https://developer.mozilla.org/docs/Web/API/Media_Session_API) shows the song name, artist and artwork with play/pause/next/previous/stop on the Android and iOS lock screen and notification, the Chrome/Edge media hub, macOS Now Playing, and keyboard media keys.
- **Pop-out window (desktop Chrome/Edge):** the ⧉ button opens an always-on-top mini player (Document Picture-in-Picture).
- **Install as an app:** open the site and choose "Install app" (Chrome/Edge/Android) or "Share → Add to Home Screen" (iOS Safari).
  Installing needs HTTPS (localhost is exempt). To try it on a phone, expose port 3000 through an HTTPS tunnel such as `cloudflared tunnel --url http://localhost:3000`, or put the app behind TLS.

## API

All endpoints accept an `X-User-Id` header. The app generates a random id per device, so there is no login.

| Method & path | |
|---|---|
| `GET /api/songs?q=&page=&size=` | catalogue / search |
| `GET /api/songs/new?limit=` | newest songs |
| `GET /api/songs/stream` | SSE feed of newly added songs |
| `GET /api/likes` · `PUT/DELETE /api/likes/{songId}` | liked songs |
| `GET /api/history` · `POST /api/history {songId}` · `DELETE /api/history` | play history |
| `POST /api/admin/ingestion/run` | trigger the fetch job now |

## Tests

| Suite | Command | Covers |
|---|---|---|
| Backend unit tests (JUnit 5, Mockito, MockMvc) | `./scripts/test-backend.sh` | providers, ingestion, Kafka producer/consumer, services, controllers |
| Backend acceptance tests (Cucumber + Testcontainers MySQL/Kafka + WireMock) | `./scripts/test-backend.sh all` | `backend/src/test/resources/features/*.feature`: cron → Kafka → DB → REST, dedupe, API outage, likes, history |
| Frontend unit tests (Vitest + Testing Library) | `cd frontend && npm test` | player logic, media-session controls, mini player, live updates, likes |
| Frontend acceptance tests (Playwright, desktop + mobile) | `cd frontend && npx playwright install chromium && npm run test:e2e` | listener journeys in a real browser |

The backend scripts run Maven inside Docker, so you don't need Java installed locally. With Java 21 installed, `mvn test` / `mvn verify` in `backend/` also work.
