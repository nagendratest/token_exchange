# Token Cache Service

## Run

```
docker compose up --build
```

The Spring Boot API will be available on `http://localhost:8081` and Keycloak on `http://localhost:8080`.

## Example Request

```
curl -X POST http://localhost:8081/token \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"ECM","clientId":"token-cache-client","clientSecret":"token-cache-secret"}'
```

## Caching Behavior

- Token URL and optional scope are resolved **only** from `application.yml` by `tenantId`.
- Cache key: `tenantId|tokenUrl|clientId` (client secret is never stored or used in the cache key).
- Cached token is returned when `now + skewSeconds < expiresAtEpochMillis`.
- When the token is expired or near expiry, the service refreshes from Keycloak using a single-flight per key to avoid stampedes.
- Failures are not cached.
