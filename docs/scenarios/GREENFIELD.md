# Scenario 1 — Greenfield

## Requirement

Build a new URL shortener with core APIs, analytics, and reliability features.

## Normalized engineering problem

Create a stateless HTTP service backed by durable storage that can safely allocate short codes, redirect valid active links, expose bounded analytics, and behave deterministically for invalid, expired, missing, or deactivated links.

## Task decomposition

1. Define API contract and HTTP semantics.
2. Define relational schema and migration strategy.
3. Implement URL policy and short-code generation.
4. Implement create/get/redirect flows.
5. Add click analytics with concurrency-safe counters.
6. Add expiration/deactivation reliability semantics.
7. Add validation/error handling/rate limiting/security headers.
8. Add health/metrics.
9. Add unit + end-to-end integration tests.
10. Add Docker and CI validation.
11. Review failure modes and document production gaps.

## Acceptance criteria

- `POST /api/v1/links` returns `201` with generated or requested code.
- Duplicate custom alias returns `409`.
- Invalid/unsafe URL returns `400`.
- Active code redirects with `302` and exact `Location`.
- Redirect increments analytics and stores a click event.
- Missing code returns `404`.
- Expired/deactivated code returns `410`.
- Health endpoint becomes healthy against PostgreSQL schema.
- `gradle check` passes.

## Validation

Automated MVC integration test exercises create → redirect → analytics → deactivate → gone. Unit tests exercise URL policy and code generation. CI runs tests/coverage and separately boots the real PostgreSQL configuration.
