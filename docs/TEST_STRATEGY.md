# Test Strategy

## Purpose

The test strategy validates behavior at the smallest useful layer and keeps one end-to-end Spring MVC path for cross-layer confidence. Tests are part of the engineering quality gate, not an afterthought.

## Layers

### Unit tests

- `LinkServiceImplTest`: business orchestration, collisions, state changes, not-found behavior, expiration, analytics mapping, race handling, and tracking-header truncation.
- `ShortLinkTest`: domain expiration boundary and deactivation.
- `UrlPolicyTest`: safe schemes, host validation, credentials rejection, trimming, and URI normalization.
- `ShortCodeGeneratorTest`: URL-safe code format and practical uniqueness.
- `RateLimitInterceptorTest`: non-create bypass and 60-per-minute boundary.
- `SecurityHeadersFilterTest`: required defensive response headers.
- `GlobalExceptionHandlerTest`: stable error mapping for controlled API exceptions.

### Repository tests

- `ShortLinkRepositoryTest`: persistence lookup, unique code constraint, atomic counter/timestamp update, inactive and expired guards.
- `ClickEventRepositoryTest`: recent-click filtering and descending time order.

Repository tests use H2 in PostgreSQL compatibility mode for speed. The CI production smoke test separately validates Flyway/Hibernate startup against PostgreSQL 18.

### Controller/integration tests

`LinkControllerIntegrationTest` starts the Spring application context with MockMvc and validates:

- custom and generated code creation;
- metadata lookup;
- redirect and Location header;
- click analytics;
- deactivation and 410 behavior;
- duplicate alias conflict;
- unsafe URL rejection;
- reserved alias rejection;
- bean validation for blank URL, alias length, and expiration;
- malformed JSON rejection;
- missing resource handling;
- redirect cache/security headers;
- idempotent deactivation.

### End-to-end flow

The primary automated flow is:

```text
create → redirect → analytics → deactivate → redirect rejected
```

`scripts/smoke-test.sh` provides the same style of black-box check against a running application.

## Quality gates

`gradle clean check` must pass before merge. It includes JUnit execution and JaCoCo verification. GitHub Actions then starts the service against PostgreSQL 18 and requires `/actuator/health` to succeed.

## Risk-based coverage

Higher emphasis is placed on code allocation, URL validation, redirect state checks, atomic click accounting, alias conflicts, expiration/deactivation, and error behavior because those paths affect correctness, abuse resistance, or data integrity.

## Known limits / production evolution

This prototype does not run full distributed concurrency/load testing or browser tests. A production path would add Testcontainers-backed PostgreSQL integration tests, load/concurrency testing, gateway/distributed rate-limit tests, observability assertions, contract tests for dependent clients, and security scanning in the delivery pipeline.
