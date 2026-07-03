# URL Shortener (Demo)

Spring Boot + MongoDB + Redis backend, React frontend.

## How it works

- **Short code generation**: Redis `INCR` on `url:counter` hands out a globally unique,
  atomic sequence number; it's base62-encoded into a fixed 5-character code
  (capacity: 62^5 ≈ 916M URLs before you'd need 6 characters).
- **Redirects**: `GET /{shortCode}` checks Redis cache first (`url:cache:{code}`,
  24h TTL), falls back to MongoDB on a miss, then re-populates the cache.
- **Click counting**: every redirect does a Redis `INCR` on `clicks:count:{code}`
  (no MongoDB write on the hot path) and marks the code in a `clicks:dirty` set.
  A scheduled job (`app.click-flush-interval-ms`, default 10s) flushes dirty
  counts into MongoDB by **overwriting** (not incrementing) `clickCount`, so
  re-running a flush is always safe and never double-counts.
- **Retention**: links are kept forever — no expiry/cleanup job.

## Run it

### Prerequisites

- Java 17+, Maven
- Node 18+
- MongoDB running on `localhost:27017`
- Redis running on `localhost:6379`

### Backend

```bash
cd backend
mvn spring-boot:run
```

Runs on `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm start
```

Runs on `http://localhost:3000`.

## API

- `POST /api/shorten` — body `{"url": "https://..."}` → `{"shortUrl": "...", "originalUrl": "..."}`
- `GET /{shortCode}` — 302 redirect to the original URL
- `GET /api/stats/{shortCode}` — `{"shortCode", "originalUrl", "clickCount", "createdAt"}`

## Known tradeoffs (it's a demo)

- If Redis is wiped between flushes, the most recent unflushed click counts are lost.
- If Redis loses the `url:counter` key without persistence enabled, new codes could
  collide with previously issued ones. Enable Redis AOF persistence if this matters.
- No rate limiting or abuse protection on the shorten endpoint.
