# GeoIP is fail-closed

## Context

A coupon is restricted to one country, and the caller's country is derived from the client IP
address through a free third-party GeoIP service (`https://ipwho.is`). That makes a business rule
depend on an external system which can be slow, wrong, rate-limited or simply down, and which can
answer "I don't know" for perfectly ordinary inputs such as private or loopback addresses.

Every such outcome forces the same question: if the country cannot be established, is the redemption
allowed or refused?

## Decision

**Refused.** Every failure mode collapses into a single `GeoIpLookupException`, which
`CouponExceptionHandler` maps to `503 Service Unavailable` with `errorCode: GEO_IP_LOOKUP_FAILED`.
There is no fallback country, no "unknown" country that matches everything, and no configuration
switch that turns the check off.

The failure modes, all of them handled in `infrastructure/geoip`:

| Situation | Where it is turned into `GeoIpLookupException` |
|---|---|
| The address is in `GEO_IP_EXCLUDED_ADDRESSES` | `GeoIpProviderAdapter.ensureLookupIsAllowed`, before any HTTP call |
| Connect or read timeout, DNS failure, HTTP 4xx/5xx | `GeoIpProviderAdapter.fetchResponse` wraps `RestClientException` |
| Empty response body | `GeoIpProviderAdapter.fetchResponse` |
| `"success": false` (provider could not resolve the address) | `IpWhoIsResponse.ensureSuccessfulResponse`, carrying the provider's own `message` |
| `country_code` absent, or not an ISO 3166-1 alpha-2 country | `IpWhoIsResponse.parseCountryCode`, via `CountryCode.from` |

The provider is reached through a dedicated `RestClient` built in `GeoIpConfiguration` with explicit
connect and read timeouts (`GEO_IP_CONNECT_TIMEOUT`, `GEO_IP_READ_TIMEOUT`), and the request asks for
the three fields it actually uses: `GET {baseUrl}/{ip}?fields=success,country_code,message`.

The port itself, `GeoIpProvider`, lives in `domain/geoip` and returns a `CountryCode` or throws — it
has no nullable "unknown" result, so a caller cannot accidentally treat "no country" as "any
country".

### Rejected alternative

**Fail-open** — proceeding when the provider is unavailable, or falling back to the coupon's own
country. It makes the service look more available, but it converts a provider outage into a business
loophole: a coupon restricted to Poland becomes redeemable from anywhere for as long as the outage
lasts, and the loophole is trivially triggerable by anyone who can make the provider fail (or wait
for its rate limit). A country restriction that is suspended under load is not a restriction. `503`
is also the honest status code: the request was not refused on its merits, the service just cannot
evaluate it right now, and the caller may retry.

## Consequences and Risks

- Provider availability is directly the availability of the redemption endpoint. That is the accepted
  price of the rule; it is stated here so it is not discovered in production.
- `503` is deliberately distinguishable from `403 COUPON_COUNTRY_MISMATCH`. A client can tell "you
  are in the wrong country" from "we could not tell where you are", and only the latter is worth
  retrying.
- **Running locally, redemption is expected to fail.** With the application and the client on the
  same machine, `remoteAddr` is `127.0.0.1` or `::1`, both of which are listed in
  `GEO_IP_EXCLUDED_ADDRESSES` in `compose.yml`, so the adapter throws *before* issuing any request —
  no traffic reaches the provider at all. Under `docker compose` the address seen by the application
  is the Docker network gateway rather than loopback, so that request does leave, and the provider
  answers `"success": false` for the private address. Two different mechanisms, one policy, one
  response: `503 GEO_IP_LOOKUP_FAILED`.
- **No retry.** A single transient blip fails the request rather than being absorbed. Retrying inside
  the request would multiply the worst-case latency by the attempt count while the caller waits, so
  the retry is left to the caller; a proper solution belongs behind a circuit breaker rather than in
  a naive loop.
- **No per-IP cache.** Every redemption attempt costs one provider round-trip, including repeat
  attempts by a user who already redeemed the coupon — the country check runs before the duplicate
  is detected
  ([and detecting it early would require the pre-`SELECT` this design avoids](2026-09-02-redemption-is-inserted-before-the-counter-is-incremented.md)).
  An IP-to-country mapping is close to static, so a short-TTL cache is the cheapest available
  improvement and would also blunt the provider's rate limit.
- **The HTTP client is not pooled.** `GeoIpConfiguration` uses `SimpleClientHttpRequestFactory`,
  i.e. `HttpURLConnection`: one connection per request, no connection pool, and a blocking call that
  occupies the servlet thread for the whole round-trip. It is the simplest factory that honours the
  configured timeouts, and it is sufficient for the traffic this service is built for, but it is the
  obvious next thing to change — a pooled client (the JDK `HttpClient` factory, or Apache HttpClient)
  would reuse connections and stop paying TCP and TLS setup on every redemption.
- No credentials or API key are involved, which is why the free provider was chosen; the flip side is
  an unspecified rate limit that this service does nothing to manage.

## Status

ACCEPTED

## Authors

Krystian Witek
