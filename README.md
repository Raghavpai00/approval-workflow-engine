# Approval Workflow Engine

A backend project built with Java 17, Spring Boot, Spring Security (JWT) and H2 database.

It handles approval requests like **Leave** and **Expense**. The approval steps are stored in the **database**, not in the code. To add a new type of request, you only add rows to a table. No code change is needed.

---

## What it does

- Users log in and get a JWT token
- A requester creates a request (for example LEAVE)
- Approvers approve or reject it, step by step
- Admin can override any step
- Every action is saved in a history that cannot be changed
- All endpoints need a token, except login

---

## Tech used

- Java 17
- Spring Boot 4
- Spring Security with JWT
- Spring Data JPA and Hibernate
- H2 database (in-memory)
- Maven
- JUnit 5, Mockito, JaCoCo, SonarQube

---

## How to run

You need Java 17, Maven and Git.

```
git clone https://github.com/Raghavpai00/approval-workflow-engine.git
cd approval-workflow-engine
mvn clean install
mvn spring-boot:run
```

The app runs at `http://localhost:8080`.

Settings are in `src/main/resources/application.yml`.

The database is in-memory, so all data is cleared when you restart the app. Log in again after a restart.

---

## Demo users

| Username | Password | Role |
|---|---|---|
| rahul | rahul123 | REQUESTER |
| anita | anita123 | APPROVER |
| vikram | vikram123 | APPROVER |
| admin | admin123 | ADMIN |

---

## Workflow setup (stored in the database)

The steps are in the `approval_step` table. They are loaded from `src/main/resources/data.sql` when the app starts.

| Request type | Step | Role who approves |
|---|---|---|
| LEAVE | 1 | APPROVER |
| LEAVE | 2 | ADMIN |
| EXPENSE | 1 | APPROVER |
| EXPENSE | 2 | APPROVER |
| EXPENSE | 3 | ADMIN |

LEAVE has 2 steps and EXPENSE has 3 steps, but both use the same code.

To add a new workflow, just insert rows. Example:

```sql
INSERT INTO approval_step (request_type, step_order, role) VALUES ('PURCHASE_ORDER', 1, 'APPROVER');
INSERT INTO approval_step (request_type, step_order, role) VALUES ('PURCHASE_ORDER', 2, 'ADMIN');
```

If a request type has no steps in the table, the API returns 400.

---

## How the workflow works

```
Create request  ->  PENDING (step 1)
Approve         ->  next step, still PENDING
Approve last    ->  APPROVED (done)
Reject (any step) -> REJECTED (done)
```

- The current step is worked out from the history: number of approvals so far + 1.
- The code never checks "is this LEAVE or EXPENSE". It only reads the steps from the table.
- Approving or rejecting a request that is already finished returns 409.

---

## API list

| Method | URL | Who can use it | What it does |
|---|---|---|---|
| POST | /auth/login | Everyone | Get a token |
| POST | /requests | REQUESTER | Create a request |
| GET | /requests | REQUESTER (own only), APPROVER, ADMIN (all) | List requests |
| GET | /requests/{id} | REQUESTER, APPROVER, ADMIN | See one request and its current step |
| POST | /requests/{id}/approve | APPROVER, ADMIN | Approve the current step |
| POST | /requests/{id}/reject | APPROVER, ADMIN | Reject the request |
| GET | /requests/history/{id} | REQUESTER (own), ADMIN | See the history |

For every call except login, send the header:
`Authorization: Bearer <your token>`

---

## Example: a LEAVE request

**1. Login as rahul**

```
POST /auth/login
{ "username": "rahul", "password": "rahul123" }
```

Response:

```
{ "token": "eyJ...", "tokenType": "Bearer", "role": "REQUESTER" }
```

**2. Create the request (rahul's token)**

```
POST /requests
{ "type": "LEAVE" }
```

Response (201):

```
{
  "id": 1,
  "type": "LEAVE",
  "status": "PENDING",
  "createdBy": "rahul",
  "currentStep": 1,
  "currentStepRole": "APPROVER"
}
```

**3. Anita approves step 1 (anita's token)**

```
POST /requests/1/approve
```

The request moves to step 2 and now needs an ADMIN.

**4. Admin approves step 2 (admin's token)**

```
POST /requests/1/approve
```

Status becomes `APPROVED`.

**5. See the history (rahul's or admin's token)**

```
GET /requests/history/1
```

You will see three entries: CREATED (rahul), APPROVED (anita), APPROVED (admin).

---

## Rules

- A requester cannot approve or reject their own request.
- Only the role set for the current step can act on it.
- One approver cannot approve two steps of the same request.
- Admin can act on any step (this is the override). It is saved in the history as ADMIN_OVERRIDE_APPROVED or ADMIN_OVERRIDE_REJECTED.
- A requester can only see their own requests. Approvers and admin can see all.

---

## Error responses

All errors look like this:

```
{
  "timestamp": "2026-11-23T02:35:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Current step requires role ADMIN",
  "path": "/requests/1/approve"
}
```

| Code | Meaning |
|---|---|
| 400 | Bad input, bad JSON, or request type has no steps configured |
| 401 | Missing, wrong or expired token, or wrong login |
| 403 | Role not allowed, or acting on your own request |
| 404 | Request not found |
| 405 | Wrong HTTP method |
| 409 | Request is already APPROVED or REJECTED |
| 415 | Body is not JSON |
| 500 | Unexpected error |

---

## Assumptions and design decisions

- The current step is calculated from the history, so the Request table has only the fields from the assignment.
- The assignment lists approve and reject for APPROVER only, but also says admin can override at any time. So ADMIN is also allowed on these two endpoints.
- Approvers are equal. There is no hierarchy between two users with the same role.
- Request type is converted to upper case (leave becomes LEAVE).
- A user table (AppUser), the login endpoint and the "list requests" endpoint are extra. They are not in the assignment but are needed for JWT login and for approvers to find requests.
- There is no logout. JWT is stateless and the token expires after 1 hour.
- Actions are transactional. No row locking is used.
- The H2 console is not exposed, because all endpoints must be authenticated.
- The demo passwords and JWT secret are only for demo use.

---

## Testing

Run all tests:

```
mvn test
```

Run tests and check coverage (build fails if line coverage is below 80%):

```
mvn clean verify
```

The coverage report is at `target/site/jacoco/index.html`.

| Item | Result |
|---|---|
| Tests passed | 72 |
| Line coverage (JaCoCo) | 96.2% |
| Line coverage (SonarQube) | 89.2% |
| Reliability and Maintainability | A and A |
| Duplications | 0.0% |

Run SonarQube:

```
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=YOUR_TOKEN
```

---

## Project structure

```
src/main/java/com/techpulse/approval_workflow_engine/
  config/                     creates the demo users
  controller/                 REST endpoints
  dto/                        request and response objects
  entity/                     database entities and enums
  globle_exception_handling/  error handling
  repository/                 database access
  security/                   JWT and security setup
  service/                    business logic (the workflow engine)

src/main/resources/
  application.yml             app settings
  data.sql                    LEAVE and EXPENSE workflow steps

src/test/java/                unit and integration tests
```

---

## Future ideas

- An admin API to add or change workflow steps without SQL
- Email notifications for pending approvals
- A page for approvers showing only their pending work
- A permanent database such as PostgreSQL

---

## Author

Raghav Pai - https://github.com/Raghavpai00