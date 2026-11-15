# Flight API

REST API for searching flights and managing the flight catalogue: flights, airports, airlines, aircraft types
and cities, plus user registration and login. Built with Spring Boot 4 on Java 21, PostgreSQL in production and
H2 for local development.

- Anyone can search flights by route and date and see a flight's details.
- Registered users can browse the catalogue.
- Admins manage the catalogue (create, update, delete).

The [front-end](https://github.com/AlexandruOriean/flightAPI_frontend) is a separate React app.

![Index page of the app](/img/index-page.png "Index page")

## Requirements

- Java 21 (the Maven wrapper `./mvnw` downloads Maven itself)
- Docker, only for the PostgreSQL integration tests and for building the container image

## Run locally

The default Maven profile `dev` runs the app with a file-based H2 database, the H2 console and Swagger UI.
Migrations (Flyway) create the schema and some seed data on first start.

```sh
export JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')

# optional: an admin account, needed for every write endpoint
export APP_ADMIN_EMAIL=admin@example.com
export APP_ADMIN_PASSWORD='choose-12-or-more-characters'

./mvnw spring-boot:run
```

| What | Where |
|---|---|
| API | http://localhost:8080/api/v1/... |
| Swagger UI (dev only) | http://localhost:8080/swagger-ui.html |
| OpenAPI document (dev only) | http://localhost:8080/v3/api-docs |
| H2 console (dev only) | http://localhost:8080/h2 (JDBC URL `jdbc:h2:file:./data/flightDB`, user `sa`, no password) |
| Health | http://localhost:8080/actuator/health |

The H2 database lives in `./data/` (ignored by git); set `DB` to use another file. Requests from the React
dev server at `http://localhost:3000` are allowed by CORS.

## Profiles

| | `dev` (default) | `prod` |
|---|---|---|
| Maven | `./mvnw ...` | `./mvnw -Pprod ...` |
| Database | H2 file (`./data/flightDB`) | PostgreSQL from `DB_URL` |
| H2 console, Swagger UI | on | not in the artifact |
| SQL logging | on | off |
| CORS default | `http://localhost:3000` | none (set `CORS_ALLOWED_ORIGINS`) |

The `prod` artifact contains no H2 and no Swagger UI; its defaults are production-safe and every secret comes
from the environment.

## Configuration

| Variable | Needed | Default | Meaning |
|---|---|---|---|
| `JWT_SECRET` | always | – | Base64 HMAC key of at least 64 bytes (HS512): `openssl rand -base64 64`. Startup fails if it is shorter. |
| `DB_URL` | prod | – | e.g. `jdbc:postgresql://db:5432/flights` |
| `DB_USERNAME`, `DB_PASSWORD` | prod | – | Database credentials |
| `CORS_ALLOWED_ORIGINS` | prod, with a browser front-end | none | Comma-separated exact origins, e.g. `https://flights.example.com` |
| `APP_ADMIN_USERNAME` | no | `admin` | Admin account created at startup |
| `APP_ADMIN_EMAIL` | with `APP_ADMIN_PASSWORD` | – | |
| `APP_ADMIN_PASSWORD` | no | – | At least 12 characters. Without it no admin is created. Changing it later does not change an existing admin's password. |
| `JWT_EXPIRATION_MS` | no | `900000` (15 min) | Access-token lifetime |
| `REFRESH_TOKEN_LIFETIME` | no | `7d` | Refresh-token lifetime, renewed on every refresh |
| `DB` | no (dev only) | `./data/flightDB` | H2 database file |

Other settings can be overridden like any Spring Boot property, e.g. the rate limits
`app.rate-limit.login.requests` / `.period` (default 10 per minute) and `app.rate-limit.register.*`
(10 per hour), as `APP_RATELIMIT_LOGIN_REQUESTS=20` in the environment.

Behind a reverse proxy or load balancer that terminates TLS, the app takes scheme, host and client IP from
`X-Forwarded-*` headers sent by internal addresses (private ranges, loopback); set
`server.tomcat.remoteip.internal-proxies` if the proxy has another address.

## Using the API

Full, always-current documentation: Swagger UI in the `dev` profile. Errors are
[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) `application/problem+json`.

### Authentication

1. **Register**: `POST /api/v1/auth/register` with `{"username", "email", "password", "name"}` → `201`.
   Every self-registered account is a plain user; admins only come from the `APP_ADMIN_*` settings.
2. **Log in**: `POST /api/v1/auth/login` with `{"username", "password"}` → `200`:
   ```json
   {
     "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 900,
     "refreshToken": "q3Zr...", "refreshExpiresIn": 604800,
     "id": 1, "username": "alice", "email": "alice@example.com", "roles": ["ROLE_USER"]
   }
   ```
3. **Call the API** with `Authorization: Bearer <accessToken>`.
4. **Refresh** before or when the access token expires (`401`): `POST /api/v1/auth/refresh` with
   `{"refreshToken": "..."}` → a new pair, same shape as login. Each refresh token works **once**; using one
   twice ends the login, so refresh from one place in the client.
5. **Log out**: `POST /api/v1/auth/logout` with `{"refreshToken": "..."}` → `204`; the refresh token stops
   working, the access token expires within its lifetime.

Login and registration are rate-limited per client IP; over the limit the API answers `429` with
`Retry-After`.

### Endpoints

| Endpoint | Who |
|---|---|
| `GET /api/v1/flights?from=OTP&to=LHR&date=2026-01-15` – search by IATA code or part of an airport/city name (at least 3 characters), departure date in the airport's local time | anyone |
| `GET /api/v1/flights/{id}` | anyone |
| `GET /api/v1/flights?page=0&size=20` – all flights, paged | admin |
| `GET /api/v1/{airports,airlines,aircraft-types,cities}` (paged), `/{id}`, `GET /api/v1/airports/iata/{code}`, `GET /api/v1/airports/names` | signed-in users |
| `POST`, `PUT /{id}`, `DELETE /{id}` on `/api/v1/{flights,airports,airlines,aircraft-types,cities}` | admin |

Lists and the flight search are paged with `?page=0&size=20` (maximum 100) and return
`{"content": [...], "page": {...}}`.
Flight times are ISO date-times in each airport's local time, prices `{"amount": 35.00, "currency": "GBP"}`.
Flights reference airports by IATA code (`OTP`), airlines by ICAO code (`ROT`) and, optionally, an aircraft type
by its ICAO type designator (`A320`, `B738`); an aircraft type cannot be deleted while flights use it.

## Build and test

```sh
./mvnw verify             # dev build: unit + integration tests, coverage gate, formatting check
./mvnw -Pprod verify      # production build, as in CI
./mvnw spotless:apply     # format the code (palantir-java-format)
```

- Integration tests run against H2; `PostgresMigrationIT` also runs every migration on a real PostgreSQL via
  Testcontainers and is skipped when Docker is not available.
- The build fails below 90 % line or 85 % branch coverage (JaCoCo, unit + integration tests merged,
  MapStruct-generated mappers excluded); the report is in `target/site/jacoco/`.
- Controllers have `@WebMvcTest` slices (status codes, validation, problem+json); access rules and the
  full auth flow are tested against the whole application (`AccessRulesIT`, `AuthenticationFlowIT`).
- The compiler runs Error Prone with warnings as errors.

## Deploy

```sh
./mvnw -Pprod package                    # target/flight-api-<version>.jar
java -jar target/flight-api-0.1.0-SNAPSHOT.jar

./mvnw -Pprod spring-boot:build-image    # OCI image ghcr.io/alexandruoriean/flight-api:<version>
```

Set the [configuration](#configuration) variables in the environment. The schema is migrated automatically on
startup (Flyway); Hibernate only validates it. Health probes: `/actuator/health/liveness` and
`/actuator/health/readiness`.

### Admin accounts

The only admin is the account from `APP_ADMIN_*`; self-registration always creates plain users. Earlier
versions let a registration request ask for `ROLE_ADMIN`, so a database from those versions may hold admins
nobody intended. At every startup the app logs a warning naming each account with `ROLE_ADMIN` other than the
configured admin. It changes nothing by itself. Check each named account; to remove the role from one that
should not have it:

```sql
DELETE FROM user_roles
WHERE role_id = (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')
  AND user_id = (SELECT id FROM users WHERE username = 'the-username');
```

The same list by hand: `SELECT u.username FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r
ON r.id = ur.role_id WHERE r.name = 'ROLE_ADMIN';`. Tokens already issued to such an account keep their roles
until they expire (access tokens: 15 minutes); its refresh tokens pick up the change at the next refresh.

## CI and code quality

- **CI** (GitHub Actions): `./mvnw -Pprod verify` on every pull request and push to `main`, then a
  [SonarQube Cloud](https://sonarcloud.io/project/overview?id=AlexandruOriean_flightAPI_backend) analysis.
  Both are required to merge into `main`.
- **Security scan**: OWASP dependency-check on every pull request, every push to `main` and daily; fails on
  CVSS ≥ 7 and is required to merge into `main`. The scan reads an NVD database that the *NVD database update*
  workflow downloads daily (from NVD's JSON feed files) into the Actions cache, so NVD outages never fail a
  pull request. On a new repository, or after the cache expired, run that workflow once by hand. Confirmed
  false positives go into `dependency-check-suppressions.xml` with a reason and an expiry date.
  Locally: `./mvnw -Pprod,security-scan -DskipTests verify` (the first run downloads ~250 MB).
- **Dependabot** keeps Maven dependencies and GitHub Actions up to date.
