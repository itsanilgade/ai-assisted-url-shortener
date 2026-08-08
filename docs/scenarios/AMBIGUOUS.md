# Scenario 3 — Ambiguous Requirement

## Original ambiguity

“Support custom aliases, expiration, analytics, and reliability.”

That leaves important behavior undefined.

## Questions an engineer would surface

- Are custom aliases case-sensitive?
- What characters/lengths are legal?
- What happens after expiration: `404` or `410`?
- Is expiration inclusive and in which timezone?
- Should analytics collect IP/geolocation?
- Should redirects use `301`, `302`, `307`, or `308`?
- Are deleted links physically removed?
- What does “reliability” mean for collisions and concurrent clicks?

## Explicit prototype decisions

1. Aliases are **case-sensitive** because PostgreSQL uniqueness and route matching preserve case; changing this later requires a deliberate canonicalization migration.
2. Alias format is `[A-Za-z0-9_-]`, length 4–32. Service route names are reserved.
3. Expiration is an absolute ISO-8601 instant. Validation requires a future instant at creation.
4. Expired/inactive links return **410 Gone**; unknown codes return **404 Not Found**.
5. Redirect uses **302 Found**, avoiding permanent client caching assumptions.
6. Analytics store total count, last-access time, referrer, and bounded user-agent. IP is not retained in the prototype.
7. “Delete” is implemented as deactivation to preserve analytics and avoid accidental alias reuse.
8. Generated-code collisions are retried up to five times; the database unique constraint is the final authority.
9. Concurrent counters use an atomic update.

## Acceptance criteria derived from assumptions

These decisions are encoded in validation, repository constraints, redirect behavior, OpenAPI, tests, and risk documentation. A product owner can replace any assumption later without silently changing semantics.
