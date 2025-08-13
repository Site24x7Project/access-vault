#  AccessVault — Secure Secrets Vault (Spring Boot)

> **By Aswathi Vipin**
> **Backend Engineer | SDE-1 Ready | Security, Scalability & Clean Code Focus**

---

## Tech Stack

Java 21 · Spring Boot 3.5 · Spring Security · JPA/Hibernate · MySQL · Bucket4j · JJWT · Lombok · OpenCSV · H2 (tests) · Swagger/OpenAPI · Docker/Compose

---

## Key Strengths & Impact

* **Defense-in-depth** security: JWT Auth, RBAC, AES-GCM encryption, rate limiting, and audit logging.
* **Data protection** — secrets encrypted at rest (AES-GCM 256-bit), masking for sensitive fields in responses.
* **Operational maturity** — Docker-ready, environment-driven config, export rate-limiting.
* **Auditability** — detailed, role-tagged audit logs stored in DB & mirrored to JSON logs.
* **Mindful engineering** — designed with production constraints in mind: role isolation, rate-limited exports, and real-world edge case handling.
* **Future-proof relevance** — applies core security & scalability patterns still critical in AI-driven, fast-evolving systems.
* **Full lifecycle coverage** — design, implementation, automated tests, manual edge case testing.
* **Swagger UI** for instant endpoint exploration.

---

## Architecture

```
[Client/Swagger] → [RateLimiterFilter] → [JwtFilter] → [Controllers] → [Services] → [Repositories] → [Entities/Converters]
```

AES-GCM encryption handled at entity/converter layer.

---

## 📍 API Endpoints & Roles

| Endpoint                 | Method | Roles Allowed    |
| ------------------------ | ------ | ---------------- |
| `/api/auth/register`     | POST   | Public           |
| `/api/auth/login`        | POST   | Public           |
| `/api/vault/add`         | POST   | ADMIN, DEV       |
| `/api/vault/my-secrets`  | GET    | ADMIN, DEV       |
| `/api/vault/all`         | GET    | ADMIN            |
| `/api/vault/delete/{id}` | DELETE | ADMIN, DEV (own) |
| `/api/test/admin`        | GET    | ADMIN            |
| `/api/test/dev`          | GET    | DEV              |
| `/api/test/ops`          | GET    | OPS              |
| `/api/logs/export-now`   | GET    | ADMIN            |
| `/api/admin/audit-logs`  | GET    | ADMIN            |

---

## ✅ Manual Test Checklist

1. Register new users for ADMIN, DEV, and OPS roles.
2. Login with each role and verify token issuance.
3. Test role-restricted endpoints (403 for unauthorized access).
4. Add secrets and confirm AES-GCM encryption in DB.
5. Verify `/my-secrets` shows masked secret values.
6. Attempt duplicate secret creation and empty field submission.
7. Delete own secret as DEV and test deletion of another role's secret.
8. Export logs twice as ADMIN; confirm 429 rate limit on third.
9. Attempt export as DEV/OPS (403 expected).
10. Access `/my-secrets` without token and with invalid token (403 expected).

---

##  Quick Start

**1) Env Vars**

```properties
SPRING_APPLICATION_NAME=accessvault
SERVER_PORT=8089
DB_URL=jdbc:mysql://localhost:3306/accessvault
DB_USER=<mysql_user>
DB_PASS=<mysql_pass>
JWT_SECRET=<32+_char_secret>
ENC_KEY=<base64_32_bytes_key>
EXPORT_PATH=/absolute/path/to/exports
```

**2) Run Locally**

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

Swagger UI → [http://localhost:8089/swagger-ui/index.html](http://localhost:8089/swagger-ui/index.html)

---

##  Docker Deployment

```bash
docker build -t accessvault:latest .
docker run --rm -p 8089:8089 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/accessvault" \
  -e DB_USER="root" \
  -e DB_PASS="changeme" \
  -e JWT_SECRET="your_32+_chars_secret" \
  -e ENC_KEY="your_base64_32_bytes_key" \
  -e EXPORT_PATH="/app/exports" \
  -v $(pwd)/exports:/app/exports \
  accessvault:latest
```

---

## License

MIT License © 2025 **Aswathi Vipin**
