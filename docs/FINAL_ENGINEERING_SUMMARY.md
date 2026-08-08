# Final Engineering Summary

## Plan and rationale

The assignment was normalized into a small production-style service with a durable source of truth, explicit API semantics, bounded analytics, reliability controls, deterministic error behavior, automated tests, CI, and review artifacts. The design intentionally favors correctness and explainability over premature distributed infrastructure.

## Delivered artifacts

- Java 17 Spring Boot URL-shortener service.
- PostgreSQL + Flyway schema.
- Create/read/redirect/analytics/deactivate APIs.
- Validation, collision retry, expiration, reserved aliases, rate limiting, security headers.
- Concurrency-safe click counter and click event history.
- Actuator health/metrics.
- OpenAPI contract.
- Unit and end-to-end integration tests.
- Dockerfile + Compose environment.
- GitHub Actions pipeline with PostgreSQL production-config smoke test.
- Architecture, traceability, AI execution model, three required scenarios, tests, risk register.

## Main trade-offs

- No redirect cache: correctness for expiration/deactivation beats prototype latency optimization.
- Synchronous analytics write: simpler transactional correctness; high-scale design should stream events.
- JVM-local limiter: meaningful single-instance guardrail, not a distributed control.
- No auth: deliberately scoped out but required before production management endpoints are exposed.

## Validation

Primary command: `gradle clean check`.

CI then verifies application startup with PostgreSQL and Flyway and requires a healthy Actuator response. Manual smoke steps are available in `scripts/smoke-test.sh`.

## Assumptions

- Custom aliases are case-sensitive.
- Redirect is 302.
- Expiration timestamps are absolute instants and must be future values when created.
- Inactive/expired = 410; unknown = 404.
- Deactivation does not release an alias for reuse.
- Analytics capture does not include client IP.

## Limitations

This prototype does not claim global-scale throughput, multi-region failover, distributed rate limiting, auth/tenant isolation, or streaming analytics. Those are explicitly identified as next-stage production work.

## Engineer ownership

AI-assisted outputs are inputs to engineering work, not authority. Correctness, maintainability, security, schema/API decisions, quality gates, and release sign-off remain engineer responsibilities.
