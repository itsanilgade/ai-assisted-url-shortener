# Project Handoff

## GitHub repository

Create the repository as:

`ai-assisted-url-shortener`

Keep it empty when creating it on GitHub (do not pre-create a README/license/gitignore because this package already contains them).

## Push commands

From the project directory:

```bash
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/ai-assisted-url-shortener.git
git push -u origin main
```

## Expected GitHub Actions

Opening/pushing to `main` triggers `.github/workflows/ci.yml`:

1. Java 17 Temurin setup.
2. Gradle dependency cache.
3. PostgreSQL 18 service container.
4. `gradle check` (compile + tests + JaCoCo gate).
5. Start the application against PostgreSQL/Flyway.
6. Require `/actuator/health` to succeed.
7. Upload the JaCoCo report artifact.

## Demo sequence

```bash
docker compose up --build
./scripts/smoke-test.sh
```

For the interview, start with `docs/FINAL_ENGINEERING_SUMMARY.md`, then use the three scenario files to walk through decomposition, execution, and validation.
