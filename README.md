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

Everything goes through `make` (run `make` alone to list all targets). You need Docker and nothing else. Java, Node and the Playwright browsers come in the `dev` container.

```bash
make dkr-start          # build + start the stack, wait until healthy
make dkr-sh             # shell inside the dev container; every make target works there too
```

| URL | What |
|---|---|
| http://localhost:3000 | The app |
| http://localhost:8080/api/songs/new | REST API |
| http://localhost:8090 | Kafka UI (topics `music.songs.discovered`, `music.songs.download-requested`) |

| Task | Command |
|---|---|
| Build / build without cache | `make dkr-build` / `make dkr-build-nc` |
| Up / stop / down / restart | `make dkr-up` / `make dkr-stop` / `make dkr-down` / `make dkr-restart [s=backend]` |
| Logs, status, MySQL shell | `make dkr-logs [s=backend]`, `make dkr-ps`, `make dkr-mysql` |
| Wipe all data (volumes) | `make dkr-reset` |
| Create `.env`, validate compose | `make config` |
| DB migrations (Flyway) | `make migrate`, `make migrate-info` |
| Run from source with hot reload | `make run-backend` (port 8081), `make run-frontend` (port 5173) |
| Clean build output / caches | `make clean`, `make cache-clean`, `make cache-clean-all` |

**Where commands run:** on your machine, tool targets (tests, lint, build, migrate) run inside the `dev` container. Inside `make dkr-sh`, or in CI (`CI=true`), they run directly. Use `LOCAL=1 make <target>` to run them with your own Java 21 / Node 22 instead.

Settings live in `.env` (see [.env.example](.env.example)):

| Variable | Default |
|---|---|
| `INGESTION_CRON` | `0 */15 * * * *` (`-` disables) |
| `INGESTION_QUERIES` | `bollywood,hindi songs,arijit singh,bollywood romantic,punjabi` |
| `DOWNLOAD_MAX_BYTES` | `104857600` (100 MB per song) |
| `JAMENDO_ENABLED` / `JAMENDO_CLIENT_ID` | `false` / empty |

## Songs stored in MySQL (downloads)

- **Song details** (title, artist, artwork, release date) are fetched by the cron job and stored in MySQL. The app always reads them from the database, never from the music API.
- **Streaming by default:** every song streams live from the music API over the internet. Nothing is stored unless you ask.
- **Download on demand:** click ⬇ on a song (or **Download all** on *Liked* / *All songs*). Only then is its audio copied into MySQL (`song_audio_chunks`, 1 MiB chunks) by a Kafka-driven background job.
- **After that,** the song plays from `/api/songs/{id}/audio`, from our own database with byte-range support, so it starts and seeks instantly. The external API is never called for it again. Downloaded songs show a green ✓ and are listed in the **Downloads** tab; click ✓ to remove one.
- **Storage:** each song takes about 3–12 MB in the MySQL volume (`mysql-data`).

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
| `GET /api/songs?q=&downloaded=&page=&size=` | catalogue / search / downloaded only |
| `GET /api/songs/new?limit=` | newest songs |
| `GET /api/songs/stream` | SSE: `song-added`, `song-updated` (download progress) |
| `GET /api/likes` · `PUT/DELETE /api/likes/{songId}` | liked songs |
| `GET /api/history` · `POST /api/history {songId}` · `DELETE /api/history` | play history |
| `POST /api/downloads {songIds:[...]}` · `POST/DELETE /api/songs/{id}/download` | store / remove audio in MySQL |
| `GET /api/songs/{id}/audio` | play a downloaded song from MySQL (supports `Range`) |
| `POST /api/admin/ingestion/run` | trigger the fetch job now |

## Tests

| Suite | Command | Covers |
|---|---|---|
| Backend unit (JUnit 5, Mockito, MockMvc) | `make test-unit` | providers, ingestion, Kafka producer/consumer, downloads, range requests, services, controllers |
| Backend acceptance (Cucumber + Testcontainers MySQL/Kafka + WireMock) | `make test-acceptance` | [features](backend/src/test/resources/features): cron → Kafka → DB → REST, dedupe, outages, likes, history, downloads served from DB without calling the API again |
| Frontend unit (Vitest + Testing Library) | `make test-js` (`make test-js-coverage`) | player, media-session controls, mini player, live updates, likes, downloads |
| Frontend acceptance (Playwright, desktop + mobile Chrome) | `make test-e2e` | listener journeys incl. download all → play from Downloads |
| Lint / types / build | `make lint`, `make typecheck`, `make build-js` | ESLint (0 warnings allowed), TypeScript, Vite build |
| Everything | `make ci` | what GitHub runs on a PR |

Quick loop while coding: `make tdd`. Clean rebuild + unit tests: `make tdd-clean`.

## CI/CD (GitHub)

- **[CI](.github/workflows/ci.yml)** runs on every pull request to `main` and again after merge: frontend lint/typecheck/unit/build, Playwright, backend unit, backend acceptance (Testcontainers), and a Docker build + smoke test of the full stack. The **`CI passed`** job succeeds only if all of them pass.
- **[CD](.github/workflows/cd.yml)**: when CI is green on `main` (i.e. after a PR is merged), the backend and frontend images are pushed to GHCR as `ghcr.io/<owner>/<repo>-backend|frontend:<sha>` and `:latest`.
- **Protect `main`** (GitHub → Settings → Branches → add rule for `main`): require a pull request, and require the status check **`CI passed`**. PRs then can't be merged until everything is green.
- Dependabot ([config](.github/dependabot.yml)) opens weekly update PRs for Maven, npm, Actions and Docker images.
