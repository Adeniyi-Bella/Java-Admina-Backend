# Admina API (Java)

Spring Boot API for user auth, document ingestion, and async processing with RabbitMQ + Redis. PostgreSQL is used for persisted data. JWT validation uses Microsoft Entra ID.

## Architecture

```mermaid
flowchart LR
  FE[Frontend] -->|JWT + multipart| API[Spring Boot API]
  API -->|Validate + save temp| FS["Temp volume /app/temp"]
  API -->|Publish job| MQ[(RabbitMQ)]
  API -->|Status| R[(Redis)]
  API -->|Auth lookup| PG[(PostgreSQL)]
  MQ -->|Consume job| WORKER[DocumentJobListener]
  WORKER -->|Update status| R
  WORKER -->|Cleanup temp| FS
  WORKER -->|Gemini call (future)| AI["Gemini AI"]
  WORKER -->|Create document (future)| PG
  API -->|Welcome email| RESEND[Resend]
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
- Job status stored in Redis with a 15-minute TTL.
- Worker drops jobs if status expired or cancelled.
- Temp files are stored at `/app/temp` (mounted from host).

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
