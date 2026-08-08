# Requirements Traceability

| Assignment expectation | Repository evidence |
|---|---|
| Requirement understanding | README, this matrix, scenario documents |
| Task decomposition | Greenfield/Brownfield/Ambiguous scenario documents |
| Brownfield codebase reasoning | `docs/scenarios/BROWNFIELD.md` impacted modules/data flow |
| AI-assisted implementation/debugging/refactoring/testing/docs | `docs/AI_ASSISTED_ENGINEERING.md`, `docs/ai-trace/DECISION_LOG.md` |
| Intent, constraints, acceptance criteria, technical context | Each scenario task contract |
| Iterative refinement | Decision log records accepted, edited, and rejected approaches |
| Quality gates | `gradle check`, integration tests, JaCoCo, GitHub Actions, Postgres smoke test |
| Secure AI usage | No secrets/customer data in prompts; human approval gate documented |
| Human sign-off | Explicit checklist in AI-assisted engineering doc |
| Production-quality code/API/schema/tests/docs | `src/`, `openapi.yaml`, Flyway migration, tests, docs |
| Risks/trade-offs/failure scenarios | `docs/RISK_REGISTER.md`, test strategy |
| Controlled oversight | Engineer-owned decisions and sign-off checklist |
| Final engineering summary | `docs/FINAL_ENGINEERING_SUMMARY.md` |
| Runnable end-to-end prototype | Docker Compose + API examples + smoke script |
| Architecture overview | `docs/ARCHITECTURE.md` |
| Three required scenarios | `docs/scenarios/*` |
| Setup instructions | README |
| Testing approach/limitations/trade-offs | Test strategy + final summary |
