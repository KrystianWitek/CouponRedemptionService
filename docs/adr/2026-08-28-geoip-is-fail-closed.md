# GeoIP is fail-closed

## Context

A coupon is restricted to one country, and the caller's country is derived from the client IP
address through a free third-party GeoIP service. That makes a business rule depend on an external
system which can be slow, wrong, rate-limited or simply down, and which can answer "I don't know" for
ordinary inputs such as private or loopback addresses. If the country cannot be established, is the
redemption allowed or refused?

The address itself has two candidate sources: the peer address of the TCP connection as observed by
the server, and the `X-Forwarded-For` / `Forwarded` headers, a claim made by whoever sent the
request.

## Decision

**The redemption is refused.** Every failure mode collapses into a single lookup exception, mapped to
`503 Service Unavailable` with `errorCode: GEO_IP_LOOKUP_FAILED`. Failure means an address on the
configured exclusion list (refused before any request leaves), a connect or read timeout, a DNS
failure, an HTTP error or an empty body, a `"success": false` answer, or a country code that is absent
or not an ISO 3166-1 alpha-2 country. There is no fallback country, no "unknown" country that matches
everything, and no configuration switch that turns the check off. The port has no nullable "unknown"
result: a caller cannot turn "no country" into "any country". The connect and read timeouts are
explicit and have no defaults, so a hanging provider becomes a prompt `503` rather than a held
request thread.

**The client IP is the peer address (`remoteAddr`); forwarded headers are ignored.** Trusting
`X-Forwarded-For` without a configured list of trusted proxies makes the country restriction
spoofable: any client could send `X-Forwarded-For: <address in the coupon's country>`, and the one
rule the GeoIP lookup exists to enforce would be decided by the party being restricted. The deployment
topology of this service is not known, so the default is the peer address, the one address the server
observes for itself.

### Rejected alternative

**Fail-open,** proceeding when the provider is unavailable, or falling back to the coupon's own
country. It makes the service look more available, but it converts a provider outage into a business
loophole: a coupon restricted to Poland becomes redeemable from anywhere for as long as the outage
lasts, and the loophole is trivially triggerable by anyone who can make the provider fail (or wait
for its rate limit). A country restriction that is suspended under load is not a restriction. `503`
is also the accurate status code: the request was not refused on its merits, the service just cannot
evaluate it right now, and the caller may retry.

## Consequences and Risks

- If the provider is down, redemption is down: the accepted price of the rule.
- `503` is distinguishable from `403 COUPON_COUNTRY_MISMATCH`. A client can tell "you are in the
  wrong country" from "we could not tell where you are", and only the latter is worth retrying.
- **Behind a load balancer the restriction stops working as intended.** Every request carries the
  balancer's address, so every caller is geolocated to wherever the infrastructure sits, and all
  redemptions of a coupon pass or fail together. Fixing it is configuration, not code: turn on
  forwarded-header handling *and* configure the trusted-proxy allow-list to match the actual ingress,
  so that headers arriving from anywhere else are discarded. Both halves are required; forwarded-header
  handling alone reintroduces the spoofing hole.
- **No retry.** A transient blip fails the request; retrying inside it would multiply the worst-case
  latency by the attempt count while the caller waits, so the retry is left to the caller.
- **No per-IP cache, and the HTTP client is not pooled.** Every redemption attempt costs one provider
  round-trip on a fresh connection, including repeat attempts by a user who already redeemed the
  coupon, because the country check runs before the duplicate is detected. An IP-to-country mapping
  is close to static, so a short-TTL cache is the cheapest available improvement and would also blunt
  the provider's rate limit.
- No credentials or API key are involved, which is why the free provider was chosen; the flip side is
  an unspecified rate limit that this service does nothing to manage.

## Status

ACCEPTED, 2026-08-28. The source of the client address was settled on 2026-08-29.

## Authors

Krystian Witek
