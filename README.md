# STANDALONE API-FIRST COMMUNICATION, DOCUMENT GENERATION & REPORTING PLATFORM

## Deployment & Verification Guide

### 1. Technology & Security Compliance
- **Java**: 21 (LTS)
- **Spring Boot**: 3.3.5
- **Dependencies**: Curated with latest stable releases to eliminate known CVEs and vulnerabilities:
  - `hypersistence-utils-hibernate-63`: 3.8.3
  - `hypersistence-tsid`: 5.2.6 (monotonic TSID generation for REF_NO)
  - `json-schema-validator`: 1.5.1 (dynamic request contracts)
  - `poi` & `poi-ooxml`: 5.3.0 (streaming SXSSF memory-efficient Excel generation)
  - `openhtmltopdf`: 1.0.10 (PDF from CSS/HTML)
  - `freemarker`: 2.3.33 (sandboxed template engine)
  - `bcprov-jdk18on`: 1.78.1 (Argon2id credential verification)
  - `aws-sdk-s3`: 2.29.1 (Supabase Storage S3-compliant client)

---

### 2. Environment Configuration
The platform is fully decoupled and configurable via environment variables:

| Environment Variable | Description | Example |
|---|---|---|
| `PORT` | HTTP Server port | `8080` |
| `DB_URL` | Supabase PostgreSQL Connection String | `jdbc:postgresql://db.xxxx.supabase.co:5432/postgres` |
| `DB_USERNAME` | PostgreSQL User | `postgres` |
| `DB_PASSWORD` | PostgreSQL Password | `[PROTECTED]` |
| `RABBITMQ_HOST` | RabbitMQ Host | `rabbitmq.cloudamqp.com` |
| `RABBITMQ_PORT` | RabbitMQ Port | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ User | `user` |
| `RABBITMQ_PASSWORD` | RabbitMQ Password | `[PROTECTED]` |
| `SUPABASE_STORAGE_ENDPOINT` | Supabase S3 Endpoint | `https://xxxx.supabase.co/storage/v1/s3` |
| `SUPABASE_STORAGE_ACCESS_KEY` | Supabase S3 Access Key | `[PROTECTED]` |
| `SUPABASE_STORAGE_SECRET_KEY` | Supabase S3 Secret Key | `[PROTECTED]` |
| `SUPABASE_STORAGE_BUCKET` | Dedicated Private Bucket | `platform-documents` |

---

### 3. Database Migrations (Flyway)
The platform manages its own schema using Flyway under `src/main/resources/db/migration`:
- **`V1__baseline_schema.sql`**: Full schema with monthly partitions for high-throughput tables (`out_message`, `out_message_sms`, `out_message_email`, `out_message_document`, `out_message_error`, `out_message_execution`, `out_message_delivery_log`, `out_message_audit`).
- **`V2__seed_retention_policies.sql`**: Configurable data retention policies for automated, non-blocking cleanup.

---

### 4. Running the Application Locally / Render
To build and run:
```bash
# Build package (with test verification)
mvn clean package -DskipTests=false

# Run Spring Boot service
java -jar target/communication-platform-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### 5. Swagger / OpenAPI Documentation
Once booted, the complete interactive OpenAPI 3 documentation is available at:
`http://localhost:8080/swagger-ui.html`
