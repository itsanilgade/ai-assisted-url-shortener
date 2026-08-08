# Risk Register

| Risk | Impact | Current control | Production follow-up |
|---|---|---|---|
| Generated-code collision | Wrong/conflicting link | DB unique constraint + bounded retry | Monitor collision metric; increase code length if scale demands |
| Concurrent click lost update | Incorrect analytics | Atomic DB increment | Load test; move events to Kafka at scale |
| Expired/deactivated stale redirect | Incorrect redirect | No redirect cache in prototype | Coherent distributed cache with version/expiry validation |
| Primary DB write on redirect | Latency/availability coupling | Simple transactional correctness | Event stream + async analytics aggregation |
| Abuse of create endpoint | Capacity/security | Input validation + JVM rate limit | API gateway/WAF + distributed limit + auth |
| Malicious URL schemes | Client harm | Only http/https; no embedded credentials | Reputation/phishing policy if product requires it |
| Sensitive analytics | Privacy risk | No IP retention; bounded headers | Retention policy, encryption, access controls |
| Unauthenticated management APIs | Unauthorized changes | Documented prototype limitation | OAuth2/OIDC + ownership/tenant authorization |
| DB secrets | Credential leak | Env configuration, gitignore | Secret manager + rotation |
| Single-region dependency | Availability | Not solved in prototype | Multi-AZ DB and service deployment, DR plan |
