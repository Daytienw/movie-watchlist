# Movie Watchlist

Track the films you want to watch. Register, search [OMDb](https://www.omdbapi.com/),
and keep a private, per-user watchlist.

- **Backend** — Java 21, Spring Boot 4.1, Spring Security (JWT), Spring Data JPA, PostgreSQL
- **Frontend** — Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4, in [`frontend/`](frontend)

## Requirements

- JDK 21
- Node.js 20+ (for the frontend)
- PostgreSQL running locally
- An OMDb API key ([free tier](https://www.omdbapi.com/apikey.aspx))

## Setup

**1. Create the database**

```bash
createdb movie-watchlist-db
```

The connection settings live in `src/main/resources/application.properties`. Adjust
`spring.datasource.username` / `password` if your local Postgres differs.

**2. Provide the secrets**

The app reads two values that are deliberately kept out of version control:

| Property | Purpose |
| --- | --- |
| `OMDB_API_KEY` | Your OMDb API key |
| `JWT_SECRET_KEY` | Base64-encoded HMAC key used to sign JWTs (256-bit minimum) |

The `local` profile is active by default, so the simplest option is to put them in
`src/main/resources/application-local.properties`, which is gitignored:

```properties
omdb.api.key=your-omdb-key
JWT_SECRET_KEY=your-base64-secret
```

Generate a suitable signing key with:

```bash
openssl rand -base64 32
```

Alternatively, export them as real environment variables and run with
`SPRING_PROFILES_ACTIVE=` unset to something other than `local`.

**3. Run the backend**

```bash
./gradlew bootRun      # starts on http://localhost:8080
./gradlew test         # unit + slice tests
./gradlew build        # full build
```

Schema is managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update`), so tables are
created on first boot. See [Schema changes](#schema-changes) for the caveat.

**4. Run the frontend**

In a second terminal, with the backend already running:

```bash
cd frontend
npm install
npm run dev            # starts on http://localhost:3000
```

Then open <http://localhost:3000> and sign up.

The frontend reads the API location from `NEXT_PUBLIC_API_URL` in `frontend/.env.local`,
which defaults to `http://localhost:8080`. The backend in turn only accepts browser
requests from the origins listed in `cors.allowed-origins`, which defaults to
`http://localhost:3000` — if you move either side, update both.

Other frontend commands:

```bash
npm run build          # production build
npm run lint           # eslint (note: `next lint` was removed in Next.js 16)
```

## Authentication

Every endpoint except `/api/auth/**` requires a bearer token:

```
Authorization: Bearer <token>
```

Log in to get one; it expires after one hour (`security.jwt.expiration-time`).

## Endpoints

### Auth

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | public | Register. Returns the created user (never the password hash). |
| `POST` | `/api/auth/login` | public | Exchange credentials for a JWT. |

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Jane Doe","email":"jane@example.com","password":"P@ssw0rd123"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"P@ssw0rd123"}'
# => {"token":"eyJhbGci...","expiresIn":3600000}
```

### Users

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | required | The authenticated user's profile. |

### Movies

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/movies/suggestions` | required | A different dozen films each call, for the add screen. |
| `GET` | `/api/movies/search?q=inception` | required | Search OMDb by title. Returns a list of matches. |
| `GET` | `/api/movie?title=Inception` | required | Exact-title lookup. Returns one film with its plot. |

All three are authenticated on purpose — each call spends the server's OMDb quota.

Search is restricted to `type=movie`, so series and games stay out of a film watchlist.
No matches is an empty array and a `200`, not a `404`. Results keep OMDb's original
capitalisation (`Title`, `Year`, `Poster`, `imdbID`).

### How the suggestions stay random without burning quota

OMDb has no random, popular or discover endpoint — only title search and lookup by id.
So variety is built from the one primitive available. `/api/movies/suggestions` draws 18
seed words at random from `MovieService.DISCOVERY_TERMS`, and takes **one** film from
each, chosen at random from that word's matches.

Taking one film per word is the whole trick. Filling the grid from one or two words
returns visibly related titles — search "black" and you get Black Widow, Black Panther,
Black Adam, Men in Black — which reads as an algorithm rather than a shelf of films.
Because each card comes from a different word, nothing on screen shares a thread. There's
a test (`getSuggestions_returnsNoTwoFilmsFromTheSameSeedTerm`) that holds this property in
place. Films without artwork are skipped, since a poster-less card looks broken.

Common words work well here because OMDb orders matches by popularity, so they surface
recognisable films rather than obscure ones.

Each seed word's results are cached, so the whole feature costs at most one OMDb request
per term for the life of the process, however many times the page is refreshed — the
randomness comes from which words get drawn and which film each yields, not from
re-querying. After warm-up a page load is about 38ms.

Widen `DISCOVERY_TERMS` for more variety, or raise `TERMS_PER_REQUEST` to draw from more
words per screenful.

### Watchlist

All watchlist routes operate only on the caller's own items.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/watchlist/` | Add a film. |
| `GET` | `/api/watchlist/` | List items, paginated. |
| `GET` | `/api/watchlist/{id}` | Fetch one item. |
| `PUT` | `/api/watchlist/{id}` | Replace an item (full body required). |
| `DELETE` | `/api/watchlist/{id}` | Remove an item. |

Query parameters on the list endpoint: `page`, `size`, `sort`, and
`status=TO_WATCH|WATCHED`.

```bash
curl -X POST http://localhost:8080/api/watchlist/ \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"imdbId":"tt1375666","title":"Inception","year":"2010",
       "poster":"https://example.com/poster.jpg","status":"TO_WATCH"}'

curl "http://localhost:8080/api/watchlist/?status=TO_WATCH&size=5&sort=addedDate,desc" \
  -H "Authorization: Bearer $TOKEN"
```

The list response is a Spring `Page` — items are under `content`, alongside
`totalElements`, `totalPages` and friends.

## Errors

Failures come back as RFC 7807 `ProblemDetail` documents with an extra `description`
field.

| Status | When |
| --- | --- |
| `400` | Validation failed, malformed body, or a missing query parameter. |
| `401` | Wrong email or password. |
| `403` | Missing, expired, or tampered JWT. |
| `404` | No such item — also returned when the item belongs to another user, so callers can't probe for foreign ids. |
| `409` | Email already registered, or the film is already on your watchlist. |
| `502` | OMDb rejected or could not serve the request. |

Validation failures list the offending fields:

```json
{
  "type": "about:blank",
  "status": 400,
  "detail": "Request validation failed",
  "description": "One or more fields are invalid",
  "errors": {
    "email": "Email must be a valid address",
    "password": "Password must be between 8 and 72 characters"
  }
}
```

## Testing with Postman

`postman_collection.json` covers every endpoint, including cross-user ownership checks
and the 400/409 paths. Import it, run **Login** first — it stores the JWT in a collection
variable that the other requests reuse — then **Login (User B)** for the ownership tests.

## Schema changes

`ddl-auto=update` adds tables and constraints but never drops them, so changing an
existing constraint needs a manual step against your local database. For example, when
watchlist uniqueness moved from a global `UNIQUE (imdb_id)` to a per-user
`UNIQUE (user_id, imdb_id)`, the old constraint had to be dropped by hand:

```sql
ALTER TABLE watchlist_items DROP CONSTRAINT <old_constraint_name>;
```

Find the name with `\d watchlist_items` in `psql`. Adopting Flyway or Liquibase would
remove this class of manual fixup.

## Project layout

```
src/main/java/com/daytien/movie_watchlist/
├── config/       # RestTemplate, password encoder, auth manager beans
├── controller/   # REST endpoints
├── dto/          # Request/response shapes — entities are never serialized directly
├── entity/       # JPA entities
├── exception/    # Custom exceptions + RFC 7807 handler
├── repository/   # Spring Data repositories
├── security/     # JWT service, auth filter, security config
└── service/      # Business logic

frontend/src/
├── app/          # Routes: /login, /signup, /watchlist, /add
├── components/   # NavBar, Poster
└── lib/          # API client, auth context, shared types
```

The frontend keeps its JWT in `localStorage`. That is the pragmatic choice given the
API hands the token back in the response body, but it does mean any XSS on the page can
read it. Moving to an httpOnly cookie would require the backend to set the cookie at
login instead.

## CI

`.github/workflows/ci.yml` runs `./gradlew build` on every push and PR to `main`,
against a PostgreSQL 15 service container.
