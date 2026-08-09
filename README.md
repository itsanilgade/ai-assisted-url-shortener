# AI-Assisted URL Shortener

A production-style **Java 17 + Spring Boot** URL-shortening service created for the AI-Proficient Software Engineer interview assignment. The project demonstrates requirement understanding, decomposition, implementation, testing, validation, risk control, brownfield reasoning, ambiguous-requirement handling, and engineer-led AI-assisted delivery.

## Repository name

`ai-assisted-url-shortener`

## Technology stack

- Java 17
- Spring Boot 3.5.16
- Spring MVC and Jakarta Bean Validation
- Spring Data JPA / Hibernate
- PostgreSQL 18
- Flyway database migrations
- Spring Boot Actuator / Micrometer
- Gradle
- JUnit 5 + Mockito + Spring Boot integration/repository tests
- H2 in PostgreSQL compatibility mode for automated tests
- JaCoCo coverage verification
- Docker / Docker Compose
- GitHub Actions
- OpenAPI 3 contract (`openapi.yaml`)

## Project package structure

```text
src/main/java/com/schwab/shortener
├── UrlShortenerApplication.java
├── config
│   ├── AppConfig.java
│   ├── RateLimitInterceptor.java
│   ├── SecurityHeadersFilter.java
│   └── WebConfig.java
├── controller
│   └── LinkController.java
├── domain
│   ├── ClickEvent.java
│   ├── LinkStatus.java
│   └── ShortLink.java
├── exception
│   ├── ApiException.java
│   └── GlobalExceptionHandler.java
├── model
│   ├── request
│   │   └── CreateLinkRequest.java
│   └── response
│       ├── AnalyticsResponse.java
│       ├── ApiError.java
│       └── LinkResponse.java
├── repository
│   ├── ClickEventRepository.java
│   └── ShortLinkRepository.java
├── service
│   ├── LinkService.java
│   └── impl
│       └── LinkServiceImpl.java
└── util
    ├── ShortCodeGenerator.java
    └── UrlPolicy.java
```

## Class responsibilities and application flow

### `UrlShortenerApplication`

Spring Boot entry point. It starts component scanning, auto-configuration, the embedded web server, persistence configuration, and Actuator endpoints.

### Controller layer

#### `controller/LinkController`

The only REST controller for the URL-shortener use cases. It is intentionally thin and contains HTTP-specific responsibilities only:

- accepts and validates HTTP requests;
- delegates business work to the `LinkService` interface;
- returns API response models;
- builds the `Location` header for redirects;
- returns appropriate HTTP statuses.

The controller does **not** directly access repositories or contain persistence/business rules.

### Service layer

#### `service/LinkService`

Business-service contract used by the controller. Keeping an interface here separates the API layer from implementation details and makes future replacement or mocking easier.

Exposed use cases:

- `create` — create a generated/custom short URL;
- `get` — retrieve link metadata;
- `resolveAndRecord` — resolve a code and record the click;
- `analytics` — return link analytics;
- `deactivate` — disable a short URL without deleting history.

#### `service/impl/LinkServiceImpl`

Concrete transactional business implementation. It coordinates repositories, domain objects, utility classes, the system clock, and configuration.

Key responsibilities:

- normalize and validate incoming URLs;
- validate reserved/custom aliases;
- retry generated-code collisions;
- create/persist links;
- determine active/expired state;
- atomically increment redirect counters;
- save click-event history;
- produce response models;
- deactivate links;
- translate missing/conflicting business conditions to controlled API exceptions.

### Request/response models

#### `model/request/CreateLinkRequest`

Input model for `POST /api/v1/links`. Jakarta validation ensures:

- URL is required and within the maximum supported length;
- custom alias, when supplied, uses URL-safe characters and supported length;
- expiration, when supplied, is in the future.

#### `model/response/LinkResponse`

Response model containing the short code, full short URL, original URL, status, timestamps, and access information. Its `from` factory maps a `ShortLink` domain entity into an API response without exposing the JPA entity itself.

#### `model/response/AnalyticsResponse`

Response model containing total clicks, last access time, and up to 100 recent click records.

### Repository layer

#### `repository/ShortLinkRepository`

Spring Data JPA repository for `ShortLink`. Besides normal CRUD operations, it includes the atomic `recordAccess` update. Updating the counter in the database prevents the common read-modify-write race where concurrent redirects could overwrite one another's click count.

#### `repository/ClickEventRepository`

Stores redirect events and retrieves the latest 100 events for analytics.

### Domain layer

#### `domain/ShortLink`

JPA entity representing a shortened URL. It owns link state such as original URL, code, active/inactive status, expiration, click count, and timestamps.

#### `domain/ClickEvent`

JPA entity representing an individual redirect event. It records the short code, event time, referrer, and user agent.

#### `domain/LinkStatus`

Enum for `ACTIVE` and `INACTIVE` states.

### Utility layer

#### `util/ShortCodeGenerator`

Generates an opaque eight-character short code using `SecureRandom` and a URL-safe alphabet.

#### `util/UrlPolicy`

Centralizes original-URL normalization and validation. It permits only HTTP/HTTPS URLs with a valid host and rejects embedded credentials or unsupported schemes.

### Exception handling

#### `exception/ApiException`

Business/API exception carrying an HTTP status.

#### `exception/GlobalExceptionHandler`

Converts controlled exceptions and request-validation failures to consistent JSON error responses.

#### `model/response/ApiError`

Error response model with timestamp, status, message, and validation details.

### Configuration and infrastructure

#### `config/AppConfig`

Provides a UTC `Clock` bean so time-dependent business logic can be injected and tested cleanly.

#### `config/RateLimitInterceptor`

Applies a per-JVM fixed-window creation limit to `POST /api/v1/links`. This is a prototype guardrail; a distributed rate limiter is the documented production evolution.

#### `config/SecurityHeadersFilter`

Adds defensive HTTP response headers such as `X-Content-Type-Options`, `Referrer-Policy`, and CSP.

#### `config/WebConfig`

Registers the rate-limit interceptor with Spring MVC.

## End-to-end request flows

### 1. Create a short URL

```text
Client
  → POST /api/v1/links
  → RateLimitInterceptor
  → LinkController.create()
  → request validation (CreateLinkRequest)
  → LinkService.create()
  → LinkServiceImpl.create()
  → UrlPolicy.normalizeAndValidate()
  → ShortCodeGenerator.next() OR supplied custom alias
  → ShortLinkRepository.saveAndFlush()
  → PostgreSQL
  → LinkResponse.from()
  → HTTP 201 Created
```

### 2. Redirect a short URL

```text
Browser/client
  → GET /{code}
  → LinkController.redirect()
  → LinkService.resolveAndRecord()
  → LinkServiceImpl.resolveAndRecord()
  → ShortLinkRepository.findByShortCode()
  → validate active/expiration state
  → ShortLinkRepository.recordAccess() [atomic SQL update]
  → ClickEventRepository.save()
  → HTTP 302 Location: original-url
```

### 3. Read analytics

```text
Client
  → GET /api/v1/links/{code}/analytics
  → LinkController.analytics()
  → LinkService.analytics()
  → LinkServiceImpl.analytics()
  → ShortLinkRepository + ClickEventRepository
  → AnalyticsResponse
  → HTTP 200
```

### 4. Deactivate a URL

```text
Client
  → DELETE /api/v1/links/{code}
  → LinkController.deactivate()
  → LinkService.deactivate()
  → LinkServiceImpl.deactivate()
  → JPA dirty checking persists INACTIVE status
  → HTTP 204

Future redirect
  → service detects inactive state
  → HTTP 410 Gone
```

## API endpoints

| Method | Path | Purpose | Typical status |
|---|---|---|---|
| `POST` | `/api/v1/links` | Create a short link | `201` |
| `GET` | `/api/v1/links/{code}` | Read link metadata | `200` |
| `GET` | `/{code}` | Redirect and record analytics | `302` |
| `GET` | `/api/v1/links/{code}/analytics` | Read analytics | `200` |
| `DELETE` | `/api/v1/links/{code}` | Deactivate a link | `204` |
| `GET` | `/actuator/health` | Application health | `200` |

See `openapi.yaml` for the API contract.

## First-time setup

### Option A — easiest: Docker Compose

Prerequisite:

- Docker Desktop, or Docker Engine + Docker Compose.

From the repository root:

```bash
docker compose up --build
```

This starts:

1. PostgreSQL 18 on port `5432`;
2. the Java application on port `8080`;
3. Flyway automatically applies `V1__initial_schema.sql`;
4. Hibernate validates that the entities match the schema.

Verify:

```bash
curl http://localhost:8080/actuator/health
```

Expected response contains:

```json
{"status":"UP"}
```

Stop everything:

```bash
docker compose down
```

Remove the local database volume as well:

```bash
docker compose down -v
```

### Option B — run Java/Gradle locally

Prerequisites:

- JDK 17;
- PostgreSQL 18 (or compatible PostgreSQL version);
- a database named `urlshortener`;
- user/password `urlshortener`, unless environment variables are overridden.

Set PostgreSQL configuration if needed:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/urlshortener
export DB_USER=urlshortener
export DB_PASSWORD=urlshortener
export BASE_URL=http://localhost:8080
```

Run tests and quality checks:

```bash
gradle clean check
```

Run the application:

```bash
gradle bootRun
```

Install Gradle 8.14.x locally if using this option. The Docker path does not require a local Gradle installation. GitHub Actions installs Gradle 8.14.5 automatically using the official Gradle setup action.

## Try the application

### Create a generated link

```bash
curl -i -X POST http://localhost:8080/api/v1/links \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://www.schwab.com"}'
```

### Create a custom alias

```bash
curl -i -X POST http://localhost:8080/api/v1/links \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/article","customAlias":"article1"}'
```

### Redirect

```bash
curl -i http://localhost:8080/article1
```

Expected: `302 Found` with `Location: https://example.com/article`.

### Get metadata

```bash
curl http://localhost:8080/api/v1/links/article1
```

### Get analytics

```bash
curl http://localhost:8080/api/v1/links/article1/analytics
```

### Deactivate

```bash
curl -i -X DELETE http://localhost:8080/api/v1/links/article1
```

A subsequent redirect returns `410 Gone`.

## Database schema and Flyway

`src/main/resources/db/migration/V1__initial_schema.sql` creates the two persistent tables:

- `short_links` — link definition and aggregate redirect statistics;
- `click_events` — individual redirect-event history.

In normal application startup, Flyway owns schema creation/migration and Hibernate uses `ddl-auto: validate` to detect mapping/schema mismatches rather than silently modifying production schema.


## Testing and quality gates

The test suite is deliberately layered so failures are easy to diagnose instead of relying only on one large end-to-end test.

```text
src/test/java/com/schwab/shortener
├── config
│   ├── RateLimitInterceptorTest.java
│   └── SecurityHeadersFilterTest.java
├── controller
│   └── LinkControllerIntegrationTest.java
├── domain
│   └── ShortLinkTest.java
├── exception
│   └── GlobalExceptionHandlerTest.java
├── repository
│   ├── ClickEventRepositoryTest.java
│   └── ShortLinkRepositoryTest.java
├── service
│   └── impl
│       └── LinkServiceImplTest.java
└── util
    ├── ShortCodeGeneratorTest.java
    └── UrlPolicyTest.java
```

### What is covered

| Area | Important scenarios |
|---|---|
| Controller / API | create, generated alias, custom alias, duplicate alias, invalid URL, reserved alias, blank URL, malformed JSON, invalid alias length, past expiration, metadata lookup, missing code, redirect, analytics, deactivate, idempotent deactivate, cache/security headers |
| Service | custom alias creation, reserved alias, duplicate alias, generated-code collision retry, retry exhaustion, metadata lookup, missing link, active redirect, expiration, race during atomic update, analytics mapping, deactivation, tracking-header truncation |
| Repository | lookup, existence check, unique-code constraint, atomic click increment, access timestamp, expired/inactive update rejection, recent-click ordering |
| Domain | active/expired boundary, deactivation |
| Utility | HTTP/HTTPS acceptance, normalization, unsupported schemes, missing host, blank URL, embedded credentials, short-code format and practical uniqueness |
| Infrastructure | rate-limit boundary and defensive security headers |
| End-to-end | create → redirect → analytics → deactivate → redirect rejected |

### Run all tests

This repository uses Gradle. With Java 17 and Gradle 8.x installed:

```bash
gradle clean check
```

`check` runs the JUnit suite and the JaCoCo verification rule. The HTML coverage report is generated at:

```text
build/reports/jacoco/test/html/index.html
```

To run only tests:

```bash
gradle test
```

### GitHub Actions validation

Every push to `main` and every pull request runs `.github/workflows/ci.yml`:

1. checks out the repository;
2. installs Java 17;
3. installs Gradle 8.14.5;
4. runs `gradle --no-daemon clean check`;
5. starts PostgreSQL 18 as a service container;
6. boots the application against PostgreSQL so Flyway and Hibernate schema validation execute;
7. waits for `/actuator/health` to report healthy;
8. uploads the JaCoCo report even when another CI step fails.

A green workflow therefore means compilation, automated tests, coverage verification, production database migration/schema validation, and application startup health all succeeded.

## First-time developer setup

### Option A — easiest: Docker Compose

Prerequisites: Docker Desktop or Docker Engine with Compose.

```bash
git clone https://github.com/<your-user>/ai-assisted-url-shortener.git
cd ai-assisted-url-shortener
docker compose up --build
```

Wait for the application to become healthy, then verify:

```bash
curl http://localhost:8080/actuator/health
```

Expected health state:

```json
{"status":"UP"}
```

Stop everything with:

```bash
docker compose down
```

### Option B — Java/Gradle locally, PostgreSQL in Docker

Prerequisites: Java 17, Gradle 8.x, and Docker.

Start only PostgreSQL:

```bash
docker compose up -d postgres
```

Run the service:

```bash
gradle bootRun
```

The default local configuration uses:

```text
DB_URL=jdbc:postgresql://localhost:5432/urlshortener
DB_USER=urlshortener
DB_PASSWORD=urlshortener
BASE_URL=http://localhost:8080
```

Override them with environment variables when needed.

## Quick API walkthrough

### Create

```bash
curl -i -X POST http://localhost:8080/api/v1/links \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://www.schwab.com","customAlias":"demo1234"}'
```

### Redirect

```bash
curl -i http://localhost:8080/demo1234
```

### Metadata

```bash
curl http://localhost:8080/api/v1/links/demo1234
```

### Analytics

```bash
curl http://localhost:8080/api/v1/links/demo1234/analytics
```

### Deactivate

```bash
curl -i -X DELETE http://localhost:8080/api/v1/links/demo1234
```

A redirect after deactivation returns HTTP `410 Gone`.

## Database lifecycle

Production/local runtime uses PostgreSQL. On startup:

```text
Spring Boot
 → DataSource
 → Flyway
 → src/main/resources/db/migration/V1__initial_schema.sql
 → Hibernate ddl-auto=validate
 → application starts
```

Flyway owns schema creation and versioning. Hibernate validates the mapped entities against the migrated schema rather than silently changing production tables.

Automated repository/controller tests use H2 in PostgreSQL compatibility mode with `ddl-auto=create-drop` for fast deterministic tests. CI separately starts the real application against PostgreSQL 18, providing a production-configuration migration and startup gate.

## Important design decisions

- **Service interface + implementation** — the controller depends on `LinkService`; orchestration resides in `service.impl.LinkServiceImpl`.
- **Separate request/response models** — external API models do not expose JPA entities.
- **Atomic redirect counter** — the database increments click count in one update to avoid lost updates from concurrent redirects.
- **Click-event table** — aggregate count is fast while recent event history remains queryable.
- **Soft deactivation** — analytics/history are preserved instead of deleting records.
- **Expiration checked twice** — the entity is checked first for a clear response, and the atomic update also guards state/expiration to handle races.
- **URL policy** — only HTTP/HTTPS URLs with a host and without embedded credentials are accepted.
- **SecureRandom codes** — opaque short codes are not predictable sequence IDs.
- **Prototype rate limiter** — useful for a single instance; a distributed store/gateway limiter is the production scaling path.
- **No redirect cache in this prototype** — avoids stale deactivation/expiration behavior unless cache invalidation is designed explicitly.

## Interview-assignment documentation

The repository intentionally contains engineering artifacts in addition to code:

- `docs/ARCHITECTURE.md` — architecture and scaling decisions;
- `docs/scenarios/GREENFIELD.md` — greenfield decomposition/execution/validation;
- `docs/scenarios/BROWNFIELD.md` — brownfield enhancement reasoning;
- `docs/scenarios/AMBIGUOUS.md` — ambiguous-requirement resolution;
- `docs/AI_ASSISTED_ENGINEERING.md` — controlled AI-assisted execution model;
- `docs/ai-trace/DECISION_LOG.md` — generated/edited/rejected decision traceability;
- `docs/REQUIREMENTS_TRACEABILITY.md` — assignment requirement-to-artifact mapping;
- `docs/TEST_STRATEGY.md` — test layers and quality approach;
- `docs/RISK_REGISTER.md` — risks, trade-offs, and mitigations;
- `docs/FINAL_ENGINEERING_SUMMARY.md` — final engineering summary and limitations;
- `docs/GITHUB_FILE_CHECKLIST.md` — exact repository files to commit and files to exclude.

## Troubleshooting

**Port 5432 already used:** stop the existing PostgreSQL process/container or change the host-side Compose port and `DB_URL` together.

**Port 8080 already used:** set `SERVER_PORT`/Spring server configuration or stop the conflicting application.

**Database connection refused:** confirm `docker compose ps` shows PostgreSQL healthy and that `DB_URL`, `DB_USER`, and `DB_PASSWORD` match.

**Flyway/Hibernate validation failure:** do not switch to `ddl-auto=update` as a shortcut. Inspect the migration/entity mismatch and add an explicit Flyway migration.

**Gradle/Java mismatch:** verify `java -version` reports Java 17 and use Gradle 8.x.

## GitHub repository

Recommended name:

```text
ai-assisted-url-shortener
```

Before pushing, review `docs/GITHUB_FILE_CHECKLIST.md`. Do not commit local IDE metadata, Gradle caches, generated build output, logs, secrets, or database data.
