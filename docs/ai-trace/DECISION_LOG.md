# AI / Engineering Decision Log

| Item | Status | Rationale |
|---|---|---|
| Java 17 baseline | Edited/accepted | Initial exploration considered newer LTS JDKs; engineer selected Java 17 for target-enterprise fit. |
| Spring Boot 3.5.x | Accepted | Current mature line compatible with Java 17; lower assignment risk than forcing framework-major migration concerns. |
| PostgreSQL + Flyway | Generated/accepted | Durable constraints, transactions, explicit schema ownership. |
| Random 8-char URL-safe code | Generated/accepted | Simple, opaque, enough prototype keyspace; DB uniqueness remains authority. |
| Retry collision up to 5 times | Edited/accepted | Handles rare generated collision without unbounded retry. |
| Read-modify-write analytics counter | Rejected | Can lose updates / create retry pressure under concurrent redirects. |
| Atomic guarded counter update | Edited/accepted | Concurrency-safe and rejects inactive/expired writes. |
| Redirect cache with fixed TTL | Rejected | Can serve stale targets after expiration/deactivation without coherence design. |
| Soft deactivation | Generated/accepted | Preserves analytics/history and avoids alias recycling ambiguity. |
| Store requester IP for analytics | Rejected | Not required for prototype; unnecessary privacy/data-retention exposure. |
| 302 redirect | Accepted | Avoids irreversible/permanent client caching semantics. |
| JVM fixed-window rate limiter | Accepted with limitation | Useful abuse guardrail for one instance; explicitly not represented as distributed protection. |
| H2 for fast integration tests | Accepted | Fast deterministic test profile; CI also boots real PostgreSQL to cover dialect/schema configuration. |
