# GitHub File Checklist

This is the commit checklist for the final `ai-assisted-url-shortener` repository.

## Add these files and folders

```text
.github/
  workflows/
    ci.yml

src/
  main/
    java/com/schwab/shortener/
      UrlShortenerApplication.java
      config/
        AppConfig.java
        RateLimitInterceptor.java
        SecurityHeadersFilter.java
        WebConfig.java
      controller/
        LinkController.java
      domain/
        ClickEvent.java
        LinkStatus.java
        ShortLink.java
      exception/
        ApiException.java
        GlobalExceptionHandler.java
      model/
        request/
          CreateLinkRequest.java
        response/
          AnalyticsResponse.java
          ApiError.java
          LinkResponse.java
      repository/
        ClickEventRepository.java
        ShortLinkRepository.java
      service/
        LinkService.java
        impl/
          LinkServiceImpl.java
      util/
        ShortCodeGenerator.java
        UrlPolicy.java
    resources/
      application.yml
      db/migration/
        V1__initial_schema.sql

  test/
    java/com/schwab/shortener/
      config/
        RateLimitInterceptorTest.java
        SecurityHeadersFilterTest.java
      controller/
        LinkControllerIntegrationTest.java
      domain/
        ShortLinkTest.java
      exception/
        GlobalExceptionHandlerTest.java
      repository/
        ClickEventRepositoryTest.java
        ShortLinkRepositoryTest.java
      service/impl/
        LinkServiceImplTest.java
      util/
        ShortCodeGeneratorTest.java
        UrlPolicyTest.java
    resources/
      application-test.yml

docs/
  AI_ASSISTED_ENGINEERING.md
  ARCHITECTURE.md
  FINAL_ENGINEERING_SUMMARY.md
  GITHUB_FILE_CHECKLIST.md
  REQUIREMENTS_TRACEABILITY.md
  RISK_REGISTER.md
  TEST_STRATEGY.md
  ai-trace/
    DECISION_LOG.md
  scenarios/
    AMBIGUOUS.md
    BROWNFIELD.md
    GREENFIELD.md

scripts/
  smoke-test.sh

.gitignore
CONTRIBUTING.md
Dockerfile
PROJECT_HANDOFF.md
README.md
SECURITY.md
build.gradle
docker-compose.yml
openapi.yaml
settings.gradle
```

## Do not add these

```text
.gradle/
build/
.idea/
*.iml
.vscode/
*.log
*.pid
.env
.env.*
.DS_Store
Thumbs.db
postgres-data/
```

Also never commit passwords, API keys, access tokens, certificates/private keys, personal credentials, or generated database data.

## Verify before push

```bash
git status
gradle clean check
git diff --check
```

If Docker is available, also run:

```bash
docker compose up --build -d
curl http://localhost:8080/actuator/health
bash scripts/smoke-test.sh
docker compose down
```

Then commit and push:

```bash
git add .
git commit -m "Complete AI-assisted URL shortener assignment"
git branch -M main
git remote add origin https://github.com/<your-user>/ai-assisted-url-shortener.git
git push -u origin main
```
