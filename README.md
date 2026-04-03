# Admina API (Java)

Spring Boot API for user auth, document ingestion, and async processing with RabbitMQ + Redis. PostgreSQL is used for persisted data. JWT validation uses Microsoft Entra ID.

## Architecture

```mermaid
flowchart LR
  FE[Frontend / Client]

  subgraph API["Spring Boot API (8080)"]
    CTRL_USER["UserController"]
    CTRL_DOC["DocumentController"]
    SVC_AUTH["AuthService"]
    SVC_USER["UserService"]
    SVC_DOC["DocumentService"]
    SVC_REDIS["RedisService"]
    PUB_DOC["DocumentJobPublisher"]
    PUB_EMAIL["SendWelcomeEmailPublisher"]
  end

  subgraph WORKER["Document Worker (Rabbit Listener)"]
    LISTENER["DocumentJobListener"]
    TEMP_CLEAN["TempFileCleaner"]
    STATUS_SVC["DocumentStatusService"]
    AI_SVC["AiService (planned)"]
  end

  FS["Temp Volume (/app/temp)"]
  MQ["RabbitMQ"]
  R["Redis"]
  PG["PostgreSQL"]
  RESEND["Resend API"]
  AI["Gemini API (future)"]

  FE -->|JWT + multipart| CTRL_DOC
  FE -->|JWT| CTRL_USER
  FE -->|poll status| CTRL_DOC

  CTRL_USER --> SVC_AUTH
  CTRL_USER --> SVC_USER
  CTRL_DOC --> SVC_USER
  CTRL_DOC --> SVC_DOC
  CTRL_DOC --> SVC_DOC
  SVC_DOC --> SVC_REDIS

  SVC_DOC --> FS
  SVC_DOC --> SVC_REDIS
  SVC_DOC --> PUB_DOC

  SVC_USER --> PG
  SVC_USER --> PUB_EMAIL

  PUB_DOC --> MQ
  PUB_EMAIL --> MQ

  MQ --> LISTENER
  LISTENER --> STATUS_SVC
  LISTENER --> TEMP_CLEAN
  LISTENER --> AI_SVC
  LISTENER --> PG

  STATUS_SVC --> R
  SVC_REDIS --> R
  TEMP_CLEAN --> FS

  PUB_EMAIL --> RESEND
  AI_SVC --> AI
```

## Services

- `auth`: JWT claim extraction and validation
- `user`: user lookup/registration
- `document`: upload validation, job enqueue, status lookup, worker
- `notification`: welcome email sending (Resend)
- `redis`: shared Redis operations (status, locks)
- `ai` (planned): Gemini integration

## Prerequisites

- Java 21
- Docker + Docker Compose
- PostgreSQL, RabbitMQ, Redis (provided via compose)

## Configuration

`.env` (not committed) must include at least:

```
DATABASE_USER=admina
DATABASE_PASSWORD=admina_password
AZURE_ISSUER_URI=https://<TENANT_ID>.ciamlogin.com/<TENANT_ID>/v2.0
AZURE_API_AUDIENCE=<YOUR_API_CLIENT_ID_OR_APP_ID_URI>
RABBITMQ_USER=admina
RABBITMQ_PASSWORD=admina_password
REDIS_PORT=6379
```

Optional:
```
RESEND_API_KEY=XXXXXXXXXXXXX
RESEND_FROM_EMAIL=XXXXXXXXXXX
```

## Local Run (Docker)

Build the jar, then run the stack:

```
./gradlew clean build
docker compose up -d --build
```

API runs on `http://localhost:8080`.

Logs:
```
docker compose logs -f api
```

## Endpoints

### Auth

```
POST /api/users/authenticate
Authorization: Bearer <ACCESS_TOKEN>
```

### Documents

Create job:
```
POST /api/documents/createDocument
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: multipart/form-data
file=<PDF|PNG|JPG> (max 6MB)
docLanguage=<2-5 letters>
targetLanguage=<2-5 letters>
```

Response:
```
{ "docId": "<uuid>", "status": "PENDING" }
```

Status:
```
GET /api/documents/status/{docId}
```

## Document Processing Behavior

- One in-flight document per user (Redis lock).
- Document pipeline capacity is capped at 40 total in-flight jobs per instance via Redis admission control.
- Job status stored in Redis with a 15-minute TTL.
- Worker drops jobs if status expired or cancelled.
- Temp files are stored at `/app/temp` (mounted from host).
- Rabbit listener execution uses virtual threads.
- Document listeners run with concurrency `5` and max concurrency `20`.
- Notification listeners run with concurrency `2` and max concurrency `5`.

## Rate Limiting

API-level rate limiting is enforced via Redis (global per user/IP).

Defaults (configurable):
- Authenticated: 60 req/min per user
- Unauthenticated: 30 req/min per IP

Response headers:
- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset` (seconds)

## Testing the API (UI)

Standard Java approach: **Swagger/OpenAPI UI** via `springdoc-openapi`. This project does not include it yet.

Recommended options:
1. Add Swagger UI (standard in Spring Boot):
   - Add `org.springdoc:springdoc-openapi-starter-webmvc-ui`
   - Then use `http://localhost:8080/swagger-ui.html`
2. Use Postman/Insomnia for manual testing.

If you want Swagger UI wired now, say so.

## Notes

- RabbitMQ management UI: `http://localhost:15672` (credentials from `.env`)
- Redis is used for job status + per-user locks.
- JWT validation uses Entra ID issuer/audience configuration.
