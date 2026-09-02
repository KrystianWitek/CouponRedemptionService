# GeoIP is fail-closed

## Context

A coupon is restricted to one country, and the caller's country is derived from the client IP
address through a free third-party GeoIP service (`https://ipwho.is`). That makes a business rule
depend on an external system which can be slow, wrong, rate-limited or simply down, and which can
answer "I don't know" for perfectly ordinary inputs such as private or loopback addresses. If the
country cannot be established, is the redemption allowed or refused?

The address itself has two candidate sources: `HttpServletRequest.remoteAddr`, the peer address of
the TCP connection as observed by the server, and the `X-Forwarded-For` / `Forwarded` headers, a
claim made by whoever sent the request. Which of them is "the caller" was decided on 2026-08-29.

## Decision

**Refused.** Every failure mode collapses into a single `GeoIpLookupException`, which
`CouponExceptionHandler` maps to `503 Service Unavailable` with `errorCode: GEO_IP_LOOKUP_FAILED`.
There is no fallback country, no "unknown" country that matches everything, and no configuration
switch that turns the check off.

| Situation | Where it becomes `GeoIpLookupException` |
|---|---|
| The address is in `GEO_IP_EXCLUDED_ADDRESSES` | `GeoIpProviderAdapter`, before any HTTP call |
| Connect or read timeout, DNS failure, HTTP 4xx/5xx, empty response body | `GeoIpProviderAdapter`, wrapping `RestClientException` |
| `"success": false` (provider could not resolve the address) | `IpWhoIsResponse`, carrying the provider's own `message` |
| `country_code` absent, or not an ISO 3166-1 alpha-2 country | `IpWhoIsResponse`, via `CountryCode.from` |

The provider is reached through a dedicated `RestClient` built in `GeoIpConfiguration` with explicit
connect and read timeouts (`GEO_IP_CONNECT_TIMEOUT`, `GEO_IP_READ_TIMEOUT`), and the request asks for
the three fields it uses: `GET {baseUrl}/{ip}?fields=success,country_code,message`.

The port itself, `GeoIpProvider`, lives in `domain/geoip` and returns a `CountryCode` or throws — it
has no nullable "unknown" result, so a caller cannot treat "no country" as "any country".

**The client IP is `remoteAddr`.** `CouponController.redeemCoupon` reads `httpRequest.remoteAddr`;
no `ForwardedHeaderFilter` is registered, `server.forward-headers-strategy` is not configured, and no
forwarded header is read anywhere in the codebase. Trusting `X-Forwarded-For` without a configured
list of trusted proxies makes the country restriction spoofable: any client could send
`X-Forwarded-For: <address in the coupon's country>`, and the one rule the GeoIP lookup exists to
enforce would be decided by the party being restricted. The deployment topology of this service is
not known, so the peer address — the one address the server observes rather than is told — is the
default.

### Rejected alternative

**Fail-open** — proceeding when the provider is unavailable, or falling back to the coupon's own
country. It makes the service look more available, but it converts a provider outage into a business
loophole: a coupon restricted to Poland becomes redeemable from anywhere for as long as the outage
lasts, and the loophole is trivially triggerable by anyone who can make the provider fail (or wait
for its rate limit). A country restriction that is suspended under load is not a restriction. `503`
is also the accurate status code: the request was not refused on its merits, the service just cannot
evaluate it right now, and the caller may retry.

## Consequences and Risks

- Provider availability is directly the availability of the redemption endpoint. That is the accepted
  price of the rule.
- `503` is distinguishable from `403 COUPON_COUNTRY_MISMATCH`. A client can tell "you are in the
  wrong country" from "we could not tell where you are", and only the latter is worth retrying.
- **Running locally, redemption is expected to fail.** With the application and the client on the
  same machine, `remoteAddr` is `127.0.0.1` or `::1`, both of which are listed in
  `GEO_IP_EXCLUDED_ADDRESSES` in `compose.yml`, so the adapter throws *before* issuing any request.
  Under `docker compose` the address seen by the application is the Docker network gateway rather
  than loopback, so that request does leave, and the provider answers `"success": false` for the
  private address. Either way the answer is `503 GEO_IP_LOOKUP_FAILED`.
- **Behind a load balancer the restriction stops working as intended.** Every request carries the
  balancer's address, so every caller is geolocated to wherever the infrastructure sits, and all
  redemptions of a coupon pass or fail together. This is the main deployment limitation; fixing it is
  configuration, not code: register `ForwardedHeaderFilter` (or set Boot's
  `forward-headers-strategy`) *and* configure the trusted-proxy allow-list to match the actual
  ingress, so that headers arriving from anywhere else are discarded. Both halves are required; the
  filter alone reintroduces the spoofing hole.
- **No retry.** A transient blip fails the request; retrying inside it would multiply the worst-case
  latency by the attempt count while the caller waits, so the retry is left to the caller.
- **No per-IP cache.** Every redemption attempt costs one provider round-trip, including repeat
  attempts by a user who already redeemed the coupon, because the country check runs before the
  duplicate is detected. An IP-to-country mapping is close to static, so a short-TTL cache is the
  cheapest available improvement and would also blunt the provider's rate limit.
- **The HTTP client is not pooled.** `GeoIpConfiguration` uses `SimpleClientHttpRequestFactory`: one
  connection per request, blocking the servlet thread for the whole round-trip.
- No credentials or API key are involved, which is why the free provider was chosen; the flip side is
  an unspecified rate limit that this service does nothing to manage.

## Status

ACCEPTED

## Authors

Krystian Witek
