# Scenario 2 — Brownfield Enhancement / Refactor / Bug Fix

This scenario treats the initial greenfield shortener as an existing codebase and demonstrates impact analysis before change.

## Change request

“Add link deactivation and make analytics reliable under concurrent redirects.”

## Impact analysis

Impacted areas:

- `ShortLink` domain state: add/consume `ACTIVE`/`INACTIVE` semantics.
- Redirect flow: inactive links must return `410`, not `404`.
- Repository update path: counter increments must not use non-atomic read-modify-write.
- API: add `DELETE /api/v1/links/{code}`.
- Analytics: preserve historical click data after deactivation.
- Tests: extend happy path through deactivation and verify analytics before state change.
- OpenAPI/docs: expose the endpoint and status semantics.

## Engineering change

The redirect counter is implemented as an atomic JPQL `UPDATE accessCount = accessCount + 1` guarded by `status=ACTIVE` and `expiresAt`. If that guarded update affects zero rows, the request fails with `410` rather than recording an invalid click. Deactivation changes state instead of deleting the row, preserving analytics/audit history.

## Rejected approach

A normal JPA entity `recordAccess()` followed by `save()` was rejected for the redirect hot path. Under concurrency, read-modify-write can lose updates or create optimistic-lock retries. The atomic update is simpler and more robust for this counter.

## Validation

- End-to-end test verifies redirect increments analytics.
- Same test deactivates the link and verifies the next redirect returns `410`.
- Repository unique constraint remains authoritative for alias collision safety.

## Residual risk

The click event and counter are in one database transaction, but this design still makes the primary database part of redirect write latency. A high-scale version should publish click events to a durable stream and make analytics asynchronous.
