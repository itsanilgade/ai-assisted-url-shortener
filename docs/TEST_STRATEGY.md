# Test Strategy

## Layers

### Unit

- URL accepts HTTP/HTTPS.
- Reject unsupported schemes and embedded credentials.
- Short-code format and practical uniqueness sample.

### Application integration

`LinkControllerIntegrationTest` boots Spring MVC + JPA and exercises:

- create with custom alias;
- redirect and `Location` header;
- analytics increment/event;
- deactivate;
- post-deactivation `410`;
- duplicate alias `409`;
- invalid input `400`;
- missing metadata `404`.

### CI production-config smoke

GitHub Actions starts PostgreSQL 18, runs `gradle check`, starts the application with the real PostgreSQL configuration/Flyway migration, and requires `/actuator/health` to return success.

## Quality gates

- Gradle build/test must pass.
- JaCoCo line coverage gate is enforced at 50% minimum for this small prototype; coverage is a floor, not a substitute for behavior coverage.
- PostgreSQL startup/migration smoke must pass.

## Additional production tests recommended

- Concurrent redirect load test proving no lost aggregate counts.
- Generated-code collision injection test.
- Expiration boundary test with an injected fixed clock.
- Rate-limit boundary/reset tests.
- Migration upgrade/rollback rehearsal.
- Large URL/header fuzzing.
- Performance SLO tests (p50/p95/p99).
- Multi-instance deactivation/cache-coherence tests if caching is added.
