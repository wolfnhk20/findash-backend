# Finance Dashboard - Backend

Spring Boot REST API for a financial transaction system. Handles authentication, role-based access control, transaction management, and analytics.

Full system documentation lives in the frontend repository.

---

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Security (JWT, stateless)
- JPA / Hibernate
- PostgreSQL

---

## Responsibilities

- Stateless JWT authentication (email-only login)
- Role enforcement on every request via a custom security filter
- Transaction CRUD with ownership validation
- Analytics aggregation
- User management (ADMIN only)

---

## Setup

```bash
# Clone the repo
git clone <repo-url>
cd finance-dashboard-backend

# Configure environment
cp .env.example .env
# Set: DB_URL, DB_USER, DB_PASS, JWT_SECRET

# Run
./mvnw spring-boot:run
```

Requires PostgreSQL running locally or via Docker.

---

## Authentication

Login is email-only. No password.

```
POST /auth/login?email={email}
```

- User must exist in the database
- User status must be `ACTIVE`
- `INACTIVE` users are rejected with `403`
- On success, returns a signed JWT

**JWT payload:**

```json
{
  "email": "user@example.com",
  "role": "ADMIN",
  "userId": 42
}
```

Every request must include the token:

```
Authorization: Bearer <token>
```

A custom Spring Security filter validates the token on every request before it reaches any controller.

---

## Roles

| Role     | Access                                                         |
|----------|----------------------------------------------------------------|
| ADMIN    | Full CRUD on transactions, user management, analytics          |
| ANALYST  | Read-only access to transactions and analytics                 |
| VIEWER   | Can only access their own transactions, must pass `userId`     |

VIEWER ownership is validated server-side. Passing another user's `userId` returns `403`.

---

## API Overview

### Auth

| Method | Endpoint                       | Access |
|--------|-------------------------------|--------|
| POST   | `/auth/login?email={email}`   | Public |

### Transactions

| Method | Endpoint                  | Access              |
|--------|--------------------------|---------------------|
| GET    | `/transactions`           | ADMIN, ANALYST, VIEWER (own only) |
| POST   | `/transactions`           | ADMIN               |
| PUT    | `/transactions/{id}`      | ADMIN               |
| DELETE | `/transactions/{id}`      | ADMIN               |

### Analytics

| Method | Endpoint                    | Access          |
|--------|-----------------------------|-----------------|
| GET    | `/transactions/analytics`   | ADMIN, ANALYST  |

### Users

| Method | Endpoint                    | Access  |
|--------|-----------------------------|---------|
| GET    | `/users?authRole=ADMIN`     | ADMIN   |

---

## Data Model

### users

| Column      | Type      | Notes                        |
|-------------|-----------|------------------------------|
| id          | BIGINT    | Primary key                  |
| email       | VARCHAR   | Unique                       |
| name        | VARCHAR   |                              |
| role        | ENUM      | ADMIN, ANALYST, VIEWER       |
| status      | ENUM      | ACTIVE, INACTIVE             |
| created_at  | TIMESTAMP |                              |

### transactions

| Column      | Type      | Notes                        |
|-------------|-----------|------------------------------|
| id          | BIGINT    | Primary key                  |
| amount      | DECIMAL   |                              |
| type        | VARCHAR   |                              |
| category    | VARCHAR   |                              |
| description | TEXT      |                              |
| date        | DATE      |                              |
| user_id     | BIGINT    | Foreign key to users         |
| created_at  | TIMESTAMP |                              |

---

## Behavior

- JWT validation happens in a filter before Spring Security's auth chain
- Invalid or expired tokens return `401`
- VIEWER requests without a matching `userId` return `403`
- INACTIVE users cannot log in, even with a valid email
- No session state is stored server-side

---

## Tradeoffs

- Email-only login removes password management complexity but shifts identity verification responsibility to whoever controls email delivery
- Stateless JWT means tokens cannot be revoked before expiry. Logout is client-side only
- VIEWER ownership check relies on `userId` being passed explicitly. No implicit scoping from the token alone

---

## Known Limitations

- No token refresh endpoint. Expired tokens require re-login
- No rate limiting on `/auth/login`
- INACTIVE status check only runs at login, not on subsequent requests with an existing token
- No soft delete on transactions

---
 
## Deployment / Live API
 
- Render: [https://findash-backend-m4ta.onrender.com/](https://findash-backend-m4ta.onrender.com/)
- Railway: [https://findash-backend-production.up.railway.app](https://findash-backend-production.up.railway.app)
 
Railway is used for faster response and to avoid cold start delays.

---

## Related Repository

Frontend + full system documentation: [https://github.com/wolfnhk20/findash-frontend/](https://github.com/wolfnhk20/findash-frontend/)
