# Customer API

Spring Boot REST API for customer data, secured with Keycloak (OAuth2 Resource Server, JWT).

## Tech stack

- Java 21, Spring Boot 3.5
- Spring Web, Spring Data JPA, H2 (in-memory)
- Spring Security OAuth2 Resource Server (JWT)
- Keycloak 26 via Docker Compose

## Running

```bash
docker compose up -d                                  # Keycloak on http://localhost:8180 (admin console: admin / admin)
mvn spring-boot:run -Dspring-boot.run.profiles=demo   # API + demo page on http://localhost:8080
```

Or with the Makefile (`make` lists all targets):

```bash
make run                         # start Keycloak, wait for it, then run the app with the demo profile
make test                        # run the tests
make token KC_USER=sally.sales   # print an access token (default user: ada.admin)
make reset                       # recreate Keycloak after changing customer-realm.json
```

### Without JDK or Maven (only Docker)

```bash
make docker-run    # builds the app image and starts Keycloak + app (demo profile) on the same ports
make docker-test   # runs the tests inside a container
make down          # stops both
```

Without `make`: `docker compose --profile app up --build`. The `app` service sits behind a compose
profile, so plain `docker compose up -d` still starts only Keycloak.

Inside the container the app expects tokens issued by `http://localhost:8180` (where the browser and
`curl` get them) but fetches Keycloak's signing keys from `http://keycloak:8080` over the Docker
network; see the `app` service in `docker-compose.yml`.

On startup Keycloak imports `keycloak/customer-realm.json` with the demo users.

The `demo` profile (`application-demo.yml`) adds two things on top of `application.yml`:

- **Seed data:** the database is filled from `src/main/resources/data/customers.json`.
  `CustomerDataLoader` only runs when `customer-api.seed-file` is set.
- **Demo page:** see below.

Without the profile (`mvn spring-boot:run`) the API starts with an empty database and no demo page.

Use `http/customer-api.http` (IntelliJ HTTP Client / VS Code REST Client) to walk through all
verification scenarios. It uses the seeded customer ids, so run the app with the `demo` profile.

### Demo page

With the `demo` profile running, open http://localhost:8080. The page (`src/main/resources/static/index.html`) logs in through
Keycloak's login screen (Authorization Code + PKCE, client `customer-api-demo-ui`), shows the
token's roles and `department` claim, and calls the API with the access token as a Bearer header.
The left side lists the demo users with their password, department and role. Picking one there or
in the dropdown next to "Log in" pre-fills the username on Keycloak's login screen. A checkbox
leaves the token out, which shows the 401.

The page is controlled by the `customer-api.demo-ui.enabled` property (off by default, on in the
`demo` profile). When it's on, `DemoUiConfig` (`@ConditionalOnProperty`) adds a separate filter
chain that makes `/` and `/index.html` public. `/api/**` keeps the same rules either way. When it's
off, the main security config blocks the page with 401.

## Testing

```bash
mvn test        # no Keycloak needed
```

- **`CustomerApiSecurityTest`:** MockMvc tests using spring-security-test's `jwt()` to build tokens
  with a given role and `department`. Covers 401/403, the department filter, reader write attempts,
  admin CRUD, 404, 409 and validation errors.
- **`JwtFilterChainTest`:** `jwt()` skips the bearer token filter, so this test sends a real
  `Authorization: Bearer` header through the full filter chain with only the `JwtDecoder` mocked.
  It proves `SecurityConfig` actually maps Keycloak's `realm_access.roles`.
- **`KeycloakRealmRoleConverterTest`:** unit test for the role mapping.
- **`EmailConstraintTest`:** forces the service's duplicate check to miss, so the database's unique
  constraint has to catch the duplicate email (the concurrent-request case).
- **`DemoUiEnabledTest` / `DemoUiDisabledTest`:** the demo page is public with the property on and
  blocked without it, and the API stays protected either way.

Against the real Keycloak, the scenarios are checked with `http/customer-api.http` and the demo page.

## Endpoints

| Method | Path                  | customer-reader             | customer-admin |
|--------|-----------------------|-----------------------------|----------------|
| GET    | `/api/customers`      | ✅ own department only      | ✅ all         |
| GET    | `/api/customers/{id}` | ✅ own department, else 403 | ✅             |
| POST   | `/api/customers`      | ❌ 403                      | ✅ 201         |
| PUT    | `/api/customers/{id}` | ❌ 403                      | ✅ 200         |
| DELETE | `/api/customers/{id}` | ❌ 403                      | ✅ 204         |

Customer fields: `id`, `firstName`, `lastName`, `email`, `status` (`ACTIVE` / `INACTIVE`), `department`.
All fields except `id` are required, and `email` must be a valid, unique address.

`POST` returns `201 Created` with a `Location` header pointing to the new customer.

### Errors

Errors use the RFC 9457 problem details format (`application/problem+json`):

| Status | When |
|--------|------|
| 400 | Invalid body (lists each failing field under `errors`), malformed JSON, non-numeric id |
| 401 | Missing, invalid or expired token (empty body, `WWW-Authenticate: Bearer` header) |
| 403 | Missing role, or customer from another department (empty body, from Spring Security) |
| 404 | Customer doesn't exist |
| 409 | Email already used by another customer |

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request body is invalid",
  "instance": "/api/customers",
  "errors": [
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```

## Keycloak configuration

Realm `customer-realm`, imported from `keycloak/customer-realm.json`.

**Client:** `customer-api-client`. A public client with *Direct Access Grants* (password grant)
enabled for demo purposes. Its protocol mapper copies the user attribute `department` into the
`department` claim of the access token.

**Client:** `customer-api-demo-ui`. A public client for the demo page, using the browser login
(Authorization Code + PKCE S256). Redirect URIs and web origins are set to `http://localhost:8080`,
and it has the same `department` mapper.

**Realm roles:** `customer-reader`, `customer-admin` (appear in the token under `realm_access.roles`).

**Access token lifespan:** 30 minutes (`accessTokenLifespan: 1800`, in seconds), so tokens fetched with
the `.http` file last through a demo.

**Users** (password for all: `password`):

| Username         | Name           | Role              | `department` claim |
|------------------|----------------|-------------------|--------------------|
| `ada.admin`      | Ada Admin      | `customer-admin`  | `it`               |
| `sally.sales`    | Sally Sales    | `customer-reader` | `sales`            |
| `mark.marketing` | Mark Marketing | `customer-reader` | `marketing`        |
| `nora.norole`    | Nora Norole    | –                 | `sales`            |

Obtain a token:

```bash
curl -s -d "grant_type=password&client_id=customer-api-client&username=sally.sales&password=password" \
  http://localhost:8180/realms/customer-realm/protocol/openid-connect/token
```

Seed customers (demo profile): ids 1–3 `sales`, 4–6 `marketing`, 7–8 `engineering`.

## Security design

- **Authentication (401):** every request needs a valid Bearer JWT. Spring validates the signature
  against Keycloak's JWKS, the `iss` claim (`spring.security.oauth2.resourceserver.jwt.issuer-uri`)
  and expiry. A missing, invalid or expired token returns `401 Unauthorized`.
- **Role mapping:** `KeycloakRealmRoleConverter` maps `realm_access.roles` to Spring authorities
  with the `ROLE_` prefix (`customer-admin` → `ROLE_customer-admin`).
- **Role-based authorization (403):** URL rules in `SecurityConfig`. `GET` requires reader or admin;
  everything else on `/api/customers/**` requires admin. Authenticated without the role → `403 Forbidden`.
- **Claim-based authorization:** `CurrentUser` reads the `department` claim. `CustomerService`
  filters the list by department for readers and throws `AccessDeniedException` (→ 403) when a
  reader requests a customer from another department. Admins skip the department check.
  A reader without a `department` claim sees nothing.

Sessions are stateless and CSRF is disabled, since the API only accepts tokens.

## Project structure

```
src/main/java/com/example/customerapi
├── config      SecurityConfig, DemoUiConfig, CustomerDataLoader (JSON seed)
├── customer
│   ├── controller   CustomerController (REST endpoints), CustomerExceptionHandler (400/404/409 as problem details)
│   ├── service      CustomerService (business logic, department check), CustomerNotFoundException,
│   │                EmailAlreadyExistsException
│   ├── repository   CustomerRepository (Spring Data JPA)
│   ├── model        Customer (JPA entity), CustomerStatus
│   └── dto          CustomerRequest, CustomerResponse
└── security    Role constants, Keycloak role converter, CurrentUser (claims)
```

## What's done

- CRUD endpoints for customers
- Basic database schema for customers (H2, JPA)
- Authentication/authorization with Keycloak: roles control which endpoints a user may call,
  the department claim gives fine-grained access to individual customer records
- Helpful error responses: proper status codes and problem details
- Tests (MockMvc with mocked JWTs, plus a test through the full security filter chain)
- Demo page and demo seed data (demo profile)

## Design decisions / improvement ideas

- A reader requesting a customer from another department gets 403. In production, 404 would usually be
  the better choice, since it doesn't reveal that the record exists (like opening a private repo on GitHub).
- The database schema is deliberately bare-bones: no soft delete, no created_at/updated_at, and no
  optimistic/pessimistic locking (concurrent updates are last-write-wins).
- No audience check yet. Any token issued by the same Keycloak realm is accepted, including
  tokens meant for other applications. 
- No automated tests against a real Keycloak
