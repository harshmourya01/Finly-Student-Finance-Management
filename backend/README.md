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
```

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
