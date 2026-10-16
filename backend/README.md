# Student Finance Backend

Spring Boot and PostgreSQL backend for the Student Financial Planning System.

## Configuration

Set the environment variables before starting the application. `JWT_SECRET` is required; the application exits during startup if it is missing, is not valid Base64, or decodes to fewer than 32 bytes. It is used for HS256 JWT signing and validation. Supply a securely generated random value and keep it out of source control and logs.

```sh
export DB_URL='<POSTGRES_JDBC_URL>'
export DB_USERNAME='<POSTGRES_USERNAME>'
export DB_PASSWORD='<POSTGRES_PASSWORD>'
export JWT_SECRET='<BASE64_RANDOM_KEY_AT_LEAST_32_BYTES>'
export JWT_EXPIRATION='<TOKEN_LIFETIME_IN_MILLISECONDS>'
export CORS_ALLOWED_ORIGINS='<FRONTEND_ORIGIN>'
```

`CORS_ALLOWED_ORIGINS` is a comma-separated list of exact browser origins (scheme, host, and optional port; no path). Its local development default allows the existing localhost frontends. For Render, set it to the deployed Vercel origin, such as `https://your-app.vercel.app`.

Database settings remain environment-configurable. Hibernate is configured with `ddl-auto=validate`; Flyway owns schema changes. Versioned migrations are in `src/main/resources/db/migration`.

### Database migrations

For a new database, Flyway applies `V1__baseline_finly_schema.sql` and then the later versioned migrations. For an existing Finly database without Flyway history, `spring.flyway.baseline-on-migrate=true` and baseline version `1` record the existing schema as the V1 baseline; Flyway then applies V2 and later migrations. Hibernate validates the resulting schema at startup and does not alter it.

Before applying this to any shared database, validate against an isolated Neon branch cloned from the intended database. Confirm the existing core schema matches the current entity mappings, deploy the candidate backend only against that branch, and verify Flyway history plus all tables, constraints, and indexes. Take a recoverable Neon backup/branch before production migration. Apply migrations in order, verify them, then start the backend. Never treat `ddl-auto=update` as a production migration. This repository change does not run migrations against any production database.

## Run

```sh
./mvnw spring-boot:run
```

## Endpoints

* **Health**: `GET /api/health` (Public)
* **Register**: `POST /api/auth/register` (Public)
* **Login**: `POST /api/auth/login` (Public)
* **Current User**: `GET /api/auth/me` (Protected - requires `Authorization: Bearer <token>`)

## Run Tests

```sh
./mvnw test
```

## Docker and Render

The backend Docker image builds the executable JAR with Maven and Java 21, then runs it on a Java 21 JRE as a non-root user. The application listens on Render's `PORT` environment variable and defaults to port `8080` when it is unset. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and the required `JWT_SECRET` remain supplied through environment variables; no credentials are included in the image.

Build and run locally after exporting the required database and JWT variables described above:

```sh
docker build -t finly-backend .
docker run --rm -p 8080:8080 \
  --env DB_URL --env DB_USERNAME --env DB_PASSWORD --env JWT_SECRET \
  --env JWT_EXPIRATION --env PORT \
  finly-backend
```

For Render, create a Web Service from this repository, set its root directory to `backend`, and select the Docker runtime. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` in the service environment. Set `DB_URL` to the JDBC URL supplied for the Neon database (including its required SSL parameters). `CORS_ALLOWED_ORIGINS` must contain the exact deployed Vercel origin. `PORT` is provided by Render; `JWT_EXPIRATION` is optional.

The container starts Flyway before Hibernate schema validation. Existing databases with no Flyway history are baselined at version 1, then receive the additive later migrations. Verify that behavior on an isolated Neon branch before production use.
