# Finly — Student Finance Management System

Finly is a full-stack personal finance application for tracking income and expenses, planning budgets and savings goals, managing recurring subscriptions, and reviewing account reports. It uses a React frontend and a Spring Boot REST API backed by PostgreSQL. Authenticated API requests use JWT bearer tokens.

## Features

- Register and sign in; protected modules require an authenticated session.
- Record, filter, edit, and delete income and expense transactions.
- Review the backend-provided balance summary. Spendable balance is calculated server-side as total income minus actual expenses, savings-goal contributions already saved, and unpaid active subscription commitments. Yearly active subscriptions are converted to monthly equivalents for this commitment calculation. When a current subscription payment is recorded as an expense, its commitment is not deducted a second time.
- Create and manage monthly budgets and savings goals, including goal contributions.
- Track active subscriptions and record subscription payments.
- Review financial analysis and account reports, with CSV export and browser print-to-PDF.
- Switch between light and dark appearance themes.

## Technology

- Frontend: React, React Router, Vite, Tailwind CSS, JavaScript
- Backend: Java 21, Spring Boot, Spring Security, Maven
- Persistence: PostgreSQL with Spring Data JPA / Hibernate
- Authentication: JWT (JJWT, HS256)
- Backend tests: Spring Boot Test, Spring Security Test, H2

## Repository layout

```text
Finly/
├── frontend/       React + Vite application
├── backend/        Spring Boot API, Maven wrapper, and tests
├── .gitattributes  wrapper line-ending rules
└── README.md       setup and project guide
```

## Prerequisites

- Java 21
- PostgreSQL (local development database)
- Node.js `^20.19.0` or `>=22.12.0` (required by the locked Vite version) and npm
- Git

## Clone and configure

```sh
git clone https://github.com/harshmourya01/Finly-Student-Finance-Management.git
cd Finly-Student-Finance-Management
```

Create a PostgreSQL database for the application, for example:

```sql
CREATE DATABASE student_finance;
```

Set the backend environment variables in the shell where the backend will run. The application requires `JWT_SECRET`: provide securely generated random bytes, Base64-encoded, with at least 32 decoded bytes. Keep the value private and out of source control. The application validates its format and minimum length during startup.

macOS/Linux (Bash/Zsh):

```sh
export DB_URL='<POSTGRES_JDBC_URL>'
export DB_USERNAME='<POSTGRES_USERNAME>'
export DB_PASSWORD='<POSTGRES_PASSWORD>'
export JWT_SECRET='<BASE64_RANDOM_KEY_AT_LEAST_32_BYTES>'
# Optional; defaults to 86400000 milliseconds (24 hours).
export JWT_EXPIRATION='<TOKEN_LIFETIME_IN_MILLISECONDS>'
```

Windows PowerShell:

```powershell
$env:DB_URL = '<POSTGRES_JDBC_URL>'
$env:DB_USERNAME = '<POSTGRES_USERNAME>'
$env:DB_PASSWORD = '<POSTGRES_PASSWORD>'
$env:JWT_SECRET = '<BASE64_RANDOM_KEY_AT_LEAST_32_BYTES>'
# Optional; defaults to 86400000 milliseconds (24 hours).
$env:JWT_EXPIRATION = '<TOKEN_LIFETIME_IN_MILLISECONDS>'
```

The placeholders above are examples only. The backend reads PostgreSQL settings from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Flyway applies the versioned backend schema migrations, and Hibernate validates the resulting schema. For an existing database without Flyway history, Flyway baselines it at version 1 and applies subsequent migrations. Validate this on an isolated Neon branch before production use; see `backend/README.md` for the migration procedure.

## Run locally

Start the backend from one terminal after configuring its environment:

```sh
cd backend
./mvnw spring-boot:run
```

On Windows, run `.\mvnw.cmd spring-boot:run` in PowerShell, or `mvnw.cmd spring-boot:run` in Command Prompt, from `backend`. The backend defaults to port `8080`.

Start the frontend in a second terminal:

```sh
cd frontend
npm install
npm run dev
```

Vite prints the local frontend URL (typically `http://localhost:5173`). Its development server proxies `/api` requests to `http://localhost:8080`. `VITE_API_BASE_URL` is optional; it defaults to an empty base so the development proxy can handle API requests. If setting it, configure it in the frontend's local Vite environment and never put credentials there.

## Checks

Backend tests:

```sh
cd backend
./mvnw test
```

Frontend production build:

```sh
cd frontend
npm ci
npm run build
```

The frontend lint script is `npm run lint`.

## API overview

All paths below are implemented by backend controllers. User-specific data endpoints require `Authorization: Bearer <JWT>`; registration, login, and health are public.

| Area | Methods and paths |
| --- | --- |
| Health | `GET /api/health` |
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Categories | `GET /api/categories` |
| Transactions | `GET /api/transactions` (supports type, search, date, category, and payment-method filters); `GET /api/transactions/balance-summary`; `GET /api/transactions/{id}`; `POST /api/transactions`; `PUT /api/transactions/{id}`; `DELETE /api/transactions/{id}` |
| Budgets | `GET /api/budgets`, `GET /api/budgets/{id}`, `POST /api/budgets`, `PUT /api/budgets/{id}`, `DELETE /api/budgets/{id}` |
| Savings goals | `GET /api/savings-goals`, `GET /api/savings-goals/{id}`, `POST /api/savings-goals`, `PUT /api/savings-goals/{id}`, `DELETE /api/savings-goals/{id}`, `POST /api/savings-goals/{id}/add-money` |
| Subscriptions | `GET /api/subscriptions`, `GET /api/subscriptions/{id}`, `POST /api/subscriptions`, `PUT /api/subscriptions/{id}`, `DELETE /api/subscriptions/{id}`, `POST /api/subscriptions/{id}/pay` |

Analysis and report views are assembled by the frontend from the existing authenticated APIs; there are no separate analysis or report controller endpoints in this backend.

## Security and troubleshooting

- Never commit `.env` files, database credentials, or JWT signing keys. The root `.gitignore` excludes local environment files; use placeholder values in documentation.
- `JWT_SECRET` is required at backend startup and must be valid Base64 that decodes to at least 32 bytes. If startup reports a JWT configuration error, set a valid key in the backend process environment. Do not reuse a source-controlled or public key.
- If the frontend cannot reach the API, confirm PostgreSQL and the backend are available, check `DB_URL`/credentials, and ensure port `8080` is reachable by Vite's `/api` proxy.
- If an authenticated request returns `401`, sign in again to obtain a current token.
- The browser keeps the authentication token in session storage; closing the browser session may require signing in again.

## Contributing

1. Create a focused branch from `main`.
2. Keep changes scoped to the relevant frontend or backend module and preserve user ownership/security checks.
3. Run the backend test suite and frontend build for changes that affect those areas.
4. Do not include local credentials, database exports, build output, or personal account data in a contribution.
5. Open a pull request describing the change and verification performed.
