````md
# URL Shortener Service

A backend URL shortener built using Java Spring Boot, PostgreSQL, and Redis.

It supports:
- short-link creation
- redirection to original URLs
- optional expiry validation
- click analytics
- Redis-based redirect caching
- Redis-based rate limiting for URL creation
- input validation and centralized exception handling
- unit tests for service and controller layers

---

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Redis
- Maven
- Docker Compose
- JUnit 5
- Mockito

---

## Features

- Create short URLs via REST API
- Redirect short URLs using HTTP 302
- Store URL mappings in PostgreSQL
- Cache redirect lookups in Redis
- Enforce optional expiry on short links
- Track `clickCount` and `lastAccessedAt`
- Apply Redis-based rate limiting on create API
- Handle invalid input and common error cases with global exception handling
- Run PostgreSQL and Redis locally using Docker Compose
- Test core service and controller logic using JUnit and Mockito

---

## High-Level Flow

### 1. Create Short URL
- Client sends a long URL and optional expiry
- Backend validates request
- Generates a unique 6-character short code
- Saves mapping in PostgreSQL
- Returns the short code and full short URL

### 2. Redirect
- Client hits `/{shortCode}`
- Backend checks Redis cache first
- If cache hit, returns redirect quickly and updates analytics in DB
- If cache miss, fetches from PostgreSQL
- Validates expiry
- Updates `clickCount` and `lastAccessedAt`
- Caches the original URL in Redis
- Returns `302 Found` with `Location` header

### 3. Analytics
- Client requests analytics for a short code
- Backend fetches metadata from PostgreSQL
- Returns click count, created time, expiry, and last access time

### 4. Rate Limiting
- Create API uses a Redis counter per client
- If request count exceeds the configured threshold in the time window, backend returns `429 Too Many Requests`

---

## Project Structure

```text
src
├── main
│   ├── java/com/example
│   │   ├── controller
│   │   │   ├── RedirectController.java
│   │   │   └── ShortUrlController.java
│   │   ├── dto
│   │   │   ├── CreateShortUrlRequest.java
│   │   │   ├── CreateShortUrlResponse.java
│   │   │   └── UrlAnalyticsResponse.java
│   │   ├── entity
│   │   │   └── ShortUrl.java
│   │   ├── exception
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── InvalidExpiryException.java
│   │   │   ├── InvalidUrlException.java
│   │   │   ├── RateLimitExceededException.java
│   │   │   ├── ShortUrlNotFoundException.java
│   │   │   └── UrlExpiredException.java
│   │   ├── repository
│   │   │   └── ShortUrlRepository.java
│   │   ├── service
│   │   │   ├── RateLimiterService.java
│   │   │   ├── ShortUrlService.java
│   │   │   └── UrlCacheService.java
│   │   ├── util
│   │   │   └── ShortCodeGenerator.java
│   │   └── UrlShortenerApplication.java
│   └── resources
│       └── application.yml
└── test
    └── java/com/example
        ├── controller
        │   ├── RedirectControllerTest.java
        │   └── ShortUrlControllerTest.java
        └── service
            ├── RateLimiterServiceTest.java
            └── ShortUrlServiceTest.java
````

---

## Architecture

The application follows a modular layered design:

* **Controller layer**
  Handles HTTP request and response mapping

* **Service layer**
  Contains business logic such as URL validation, short code generation, redirect handling, analytics update, caching, and rate limiting

* **Repository layer**
  Handles persistence using Spring Data JPA

* **Entity layer**
  Defines the database-mapped model

* **DTO layer**
  Defines request and response objects exposed through the API

* **Exception layer**
  Centralizes error handling and maps application errors to correct HTTP status codes

This separation keeps the codebase maintainable and easy to test.

---

## Database Schema

Main table: `short_urls`

### Fields

* `id` - primary key
* `short_code` - unique short token used in redirects
* `original_url` - full original URL
* `created_at` - timestamp when short URL was created
* `expires_at` - optional expiry timestamp
* `click_count` - number of redirects
* `last_accessed_at` - timestamp of the latest redirect

### Representative SQL Schema

```sql
CREATE TABLE short_urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    click_count INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP NULL
);
```

---

## Redis Usage

Redis is used for two things:

### 1. Redirect Cache

Stores:

* `url:<shortCode> -> <originalUrl>`

Example:

* `url:hNFsWc -> https://www.google.com`

### 2. Rate Limiting

Stores:

* `rate_limit:create:<clientIp> -> count`

Example:

* `rate_limit:create:0:0:0:0:0:0:0:1`

---

## API Endpoints

### 1. Create Short URL

**POST** `/api/v1/urls`

#### Request Body

```json
{
  "originalUrl": "https://www.google.com",
  "expiresAt": "2026-12-31T23:59:59"
}
```

#### Sample cURL

```bash
curl -X POST http://localhost:8080/api/v1/urls \
-H "Content-Type: application/json" \
-d '{"originalUrl":"https://www.google.com","expiresAt":"2026-12-31T23:59:59"}'
```

#### Sample Response

```json
{
  "shortCode": "hNFsWc",
  "shortUrl": "http://localhost:8080/hNFsWc",
  "originalUrl": "https://www.google.com",
  "createdAt": "2026-05-18T18:17:11.450618192",
  "expiresAt": "2026-12-31T23:59:59"
}
```

---

### 2. Redirect to Original URL

**GET** `/{shortCode}`

#### Sample cURL

```bash
curl -i http://localhost:8080/hNFsWc
```

#### Sample Response

```http
HTTP/1.1 302
Location: https://www.google.com
Content-Length: 0
```

---

### 3. Get Analytics

**GET** `/api/v1/urls/{shortCode}/analytics`

#### Sample cURL

```bash
curl http://localhost:8080/api/v1/urls/hNFsWc/analytics
```

#### Sample Response

```json
{
  "shortCode": "hNFsWc",
  "originalUrl": "https://www.google.com",
  "clickCount": 1,
  "createdAt": "2026-05-18T18:17:11.450618",
  "expiresAt": "2026-12-31T23:59:59",
  "lastAccessedAt": "2026-05-18T18:18:38.842544"
}
```

---

## Error Handling

The application uses centralized exception handling with `@RestControllerAdvice`.

### Common Error Cases

#### Invalid URL

```json
{
  "timestamp": "2026-05-18T18:20:10.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid URL format"
}
```

#### Expiry in the Past

```json
{
  "timestamp": "2026-05-18T18:20:15.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Expiry time cannot be in the past"
}
```

#### Short URL Not Found

```json
{
  "timestamp": "2026-05-18T18:20:20.123",
  "status": 404,
  "error": "Not Found",
  "message": "Short URL not found"
}
```

#### Short URL Expired

```json
{
  "timestamp": "2026-05-18T18:20:25.123",
  "status": 410,
  "error": "Gone",
  "message": "Short URL has expired"
}
```

#### Rate Limit Exceeded

```json
{
  "timestamp": "2026-05-18T19:12:54.623",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

---

## Rate Limiting Configuration

Configured in `application.yml`:

```yaml
app:
  rate-limit:
    create-url:
      max-requests: 5
      window-minutes: 1
```

Meaning:

* maximum 5 create requests
* per client
* within 1 minute

---

## Application Configuration

Example `application.yml` structure:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/url_shortener_db
    username: url_shortener_usr
    password: your_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  data:
    redis:
      host: localhost
      port: 6379

app:
  base-url: http://localhost:8080
  rate-limit:
    create-url:
      max-requests: 5
      window-minutes: 1
```

---

## Running the Project

### 1. Start PostgreSQL and Redis

```bash
docker compose up -d
```

### 2. Verify containers

```bash
docker ps
```

### 3. Run the Spring Boot application

```bash
./mvnw spring-boot:run
```

### 4. Run tests

```bash
./mvnw test
```

---

## Manual Verification Steps

### Verify a short URL is stored in PostgreSQL

```bash
docker exec -it postgres_db psql -U url_shortener_usr -d url_shortener_db
```

Inside `psql`:

```sql
select * from short_urls;
```

### Verify Redis cache keys

```bash
docker exec -it redis_cache redis-cli
```

Inside Redis CLI:

```bash
KEYS *
GET url:hNFsWc
```

---

## Testing Summary

The test suite includes:

* service layer tests for short URL creation, expiry validation, cache hit and cache miss flows, analytics retrieval, and missing and expired short code handling
* controller layer tests for create API, analytics API, and redirect response behavior
* rate limiter tests for allowed requests and blocked requests when limit is exceeded

---

## Sample Working Flow

### Step 1: Create a short URL

```bash
curl -X POST http://localhost:8080/api/v1/urls \
-H "Content-Type: application/json" \
-d '{"originalUrl":"https://www.google.com","expiresAt":"2026-12-31T23:59:59"}'
```

Response:

```json
{
  "shortCode": "hNFsWc",
  "shortUrl": "http://localhost:8080/hNFsWc",
  "originalUrl": "https://www.google.com",
  "createdAt": "2026-05-18T18:17:11.450618192",
  "expiresAt": "2026-12-31T23:59:59"
}
```

### Step 2: Redirect using short code

```bash
curl -i http://localhost:8080/hNFsWc
```

Response:

```http
HTTP/1.1 302
Location: https://www.google.com
```

### Step 3: Fetch analytics

```bash
curl http://localhost:8080/api/v1/urls/hNFsWc/analytics
```

Response:

```json
{
  "shortCode": "hNFsWc",
  "originalUrl": "https://www.google.com",
  "clickCount": 1,
  "createdAt": "2026-05-18T18:17:11.450618",
  "expiresAt": "2026-12-31T23:59:59",
  "lastAccessedAt": "2026-05-18T18:18:38.842544"
}
```

---

## Design Choices

### Why PostgreSQL?

* durable persistent storage
* supports indexing and structured schema
* ideal as the source of truth for URL mappings and analytics

### Why Redis?

* fast in-memory lookups for high-read redirect flow
* ideal for request counters in rate limiting
* reduces load on PostgreSQL for repeated redirects

### Why controller-service-repository architecture?

* separates HTTP handling, business logic, and data access
* improves readability and maintainability
* makes testing easier

### Why random 6-character short codes?

* compact and user-friendly
* large Base62 code space
* simple to implement with collision checks in DB



```
```
