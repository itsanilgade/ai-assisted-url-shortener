# AI-Assisted Engineering Execution

## Operating model

AI is used as an accelerator inside engineer-owned tasks. It does not approve architecture, merge changes, handle credentials, or determine production readiness. Every high-impact change requires engineer review.

## Task contract used for AI assistance

Each AI-assisted task is framed with:

- **Intent:** what outcome is required.
- **Context:** current modules/data flow/API behavior.
- **Constraints:** Java 17, production-style code, assignment scope, security/privacy boundaries.
- **Acceptance criteria:** observable behavior and test expectations.
- **Validation:** compile/test/CI/security/review checks.

## Where AI assisted

- Requirement normalization and ambiguity inventory.
- Architecture alternatives and trade-off exploration.
- Initial code scaffolding.
- Validation and API test generation.
- Review of concurrency, expiry, cache coherence, and collision failure modes.
- Documentation and review-preparation artifacts.

## Traceability categories

- **Generated:** AI proposed an initial implementation/artifact.
- **Edited:** engineer/AI review changed it based on correctness or maintainability concerns.
- **Rejected:** approach was not used; reason retained in decision log.

See `docs/ai-trace/DECISION_LOG.md`.

## Secure AI usage guardrails

- No customer PII, credentials, production secrets, tokens, or proprietary source code should be pasted into an external model.
- Use synthetic URLs/test data.
- Treat generated dependencies, shell commands, SQL, and security recommendations as untrusted until reviewed.
- Run deterministic quality gates after generated changes.
- Human review is mandatory for schema migrations, security controls, data retention, public API changes, and deployment configuration.

## Quality gates

1. Code review against normalized requirements.
2. `gradle check` unit/integration/coverage gate.
3. PostgreSQL-backed application startup and `/actuator/health` smoke check in GitHub Actions.
4. API contract review (`openapi.yaml`).
5. Migration review (`V1__initial_schema.sql`).
6. Risk/trade-off review.

## Human sign-off checklist

Before merge/release, an engineer must confirm:

- [ ] Requirements/assumptions are still valid.
- [ ] No secrets or sensitive data were exposed to AI.
- [ ] Tests and CI pass.
- [ ] Database migration is backward/forward operationally acceptable.
- [ ] API changes are intentional.
- [ ] Security/privacy controls are reviewed.
- [ ] Risks and limitations are accepted.
- [ ] Rollback path is understood.
