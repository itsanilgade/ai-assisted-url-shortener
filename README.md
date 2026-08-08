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
- JUnit 5 + Spring Boot integration tests
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
│   ├── ApiError.java
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

## Testing

Tests are under matching packages in `src/test/java`:

```text
controller/LinkControllerIntegrationTest.java
domain/ShortLinkTest.java
util/ShortCodeGeneratorTest.java
util/UrlPolicyTest.java
```

`LinkControllerIntegrationTest` exercises the complete application context and covers:

- create → redirect → analytics → deactivate end-to-end flow;
- custom-alias collision handling;
- invalid request handling;
- generated code behavior;
- metadata lookup;
- reserved alias validation;
- missing-link response.

`ShortLinkTest` verifies expiration-boundary and deactivation domain behavior.

Run everything:

```bash
gradle clean check
```

`check` executes tests and JaCoCo coverage verification. HTML coverage is generated under:

```text
build/reports/jacoco/test/html/index.html
```

## GitHub Actions CI flow

`.github/workflows/ci.yml` runs for pushes to `main` and pull requests:

```text
Checkout
  → Set up Temurin Java 17
  → Restore Gradle cache
  → Start PostgreSQL service container
  → gradle clean check
  → start app against real PostgreSQL
  → call /actuator/health
  → upload JaCoCo report
```

A green workflow proves compilation, tests, coverage gate, and a real-PostgreSQL startup smoke test passed in CI.

## Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/urlshortener` | PostgreSQL JDBC URL |
| `DB_USER` | `urlshortener` | DB username |
| `DB_PASSWORD` | `urlshortener` | Local DB password |
| `BASE_URL` | `http://localhost:8080` | Public short-link base URL |

Production credentials should come from the target platform's secret manager and must not be committed.

## HTTP/error behavior

- invalid input → `400 Bad Request`;
- missing code → `404 Not Found`;
- duplicate custom alias → `409 Conflict`;
- expired/inactive short URL → `410 Gone`;
- create-link rate limit exceeded → `429 Too Many Requests`.

## Engineering documentation

- `docs/ARCHITECTURE.md` — architecture, data flow, scaling path, and decisions.
- `docs/REQUIREMENTS_TRACEABILITY.md` — assignment requirement → repository artifact mapping.
- `docs/scenarios/GREENFIELD.md` — greenfield decomposition/execution/validation.
- `docs/scenarios/BROWNFIELD.md` — enhancement/refactor/bug-fix reasoning.
- `docs/scenarios/AMBIGUOUS.md` — ambiguous requirement resolution and assumptions.
- `docs/AI_ASSISTED_ENGINEERING.md` — AI task contract, traceability, guardrails, and human sign-off.
- `docs/ai-trace/DECISION_LOG.md` — generated/edited/rejected decisions and rationale.
- `docs/TEST_STRATEGY.md` — test approach, quality gates, and failure cases.
- `docs/RISK_REGISTER.md` — risks, mitigations, and residual risks.
- `docs/FINAL_ENGINEERING_SUMMARY.md` — final review summary.

## Important design decisions

### Why service interface + implementation?

The controller programs to `LinkService`, while transaction and persistence orchestration live in `LinkServiceImpl`. This keeps API concerns separate from business implementation and provides a natural seam for tests or future alternative implementations.

### Why request/response DTOs instead of returning JPA entities?

The HTTP contract is intentionally decoupled from persistence. Database changes therefore do not automatically leak into the public API, and callers cannot mutate persistence entities through request binding.

### Why atomic click updates?

A traditional load-entity → increment Java field → save flow can lose increments under concurrent redirects. `ShortLinkRepository.recordAccess` performs `access_count = access_count + 1` directly in the database with active/expiration predicates.

### Why no redirect cache in this prototype?

A cache would improve latency but introduces invalidation correctness when a URL expires or is deactivated. The prototype prioritizes correctness and documents a coherent distributed-cache design as a production evolution.

## Deliberate prototype limitations

This is an interview-sized production-style prototype rather than a globally distributed URL platform. The rate limiter is per JVM, click-event analytics remain in PostgreSQL, no tenant/authentication model is included, and redirects are not cached. A production scale-out would introduce distributed rate limiting, authentication/authorization, event-stream analytics, caching with explicit invalidation, and operational SLOs.
