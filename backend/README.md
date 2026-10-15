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

Database settings remain environment-configurable; the application properties retain local development defaults where configured. Hibernate uses `ddl-auto=update` for local development.

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
