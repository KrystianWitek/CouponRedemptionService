# Client IP comes from `remoteAddr`, with no forwarded-header handling

## Context

The country restriction is evaluated against the caller's IP address, so the service must decide
which address it treats as "the caller". A servlet request offers two sources:

- `HttpServletRequest.remoteAddr` — the peer address of the TCP connection, observed by the server
  itself,
- `X-Forwarded-For` / `Forwarded` — a header, i.e. a claim made by whoever sent the request.

Behind a reverse proxy or load balancer the peer address is the proxy, and the header is the only way
to see the original client. In front of no proxy, the header is arbitrary attacker-supplied text.

## Decision

`CouponController.redeemCoupon` reads `httpRequest.remoteAddr` and passes it into
`RedeemCouponCommand`. No `ForwardedHeaderFilter` is registered, `server.forward-headers-strategy` is
not configured, and no forwarded header is read anywhere in the codebase.

The reason is that trusting `X-Forwarded-For` without a configured list of trusted proxies makes the
country restriction spoofable by the caller. Any client could send
`X-Forwarded-For: <address in the coupon's country>` and redeem a coupon it is not entitled to — the
one rule the GeoIP lookup exists to enforce would be decided by the party being restricted. A
forwarded header is only trustworthy when the request provably passed through a proxy that
overwrites it, and that requires knowing the deployment topology: which hops exist, which of them
append and which overwrite, and how many entries from the right must be discarded. That topology is
not known for this service, and guessing it produces a configuration that looks correct and is not.

With the topology unknown, the peer address is the honest default: it is the one address the server
observes rather than is told, so the restriction can only ever be wrong in a direction the caller
does not control.

`GEO_IP_EXCLUDED_ADDRESSES` complements this. Loopback addresses (`127.0.0.1,::1` in `compose.yml`)
are rejected in `GeoIpProviderAdapter` before any HTTP call, so a request made from the same machine
as the application fails fast and locally instead of spending a provider round-trip and its rate
limit on an address no provider can geolocate. It is a fast-fail, not a security control — see
[GeoIP is fail-closed](2026-09-02-geoip-is-fail-closed.md).

## Consequences and Risks

- **Behind a load balancer the restriction stops working as intended.** Every request would carry the
  balancer's address, so every caller would be geolocated to wherever the infrastructure sits: all
  redemptions would then either pass or fail together, depending on the coupon's country. This is the
  single most important limitation of the current deployment story, and it is a configuration gap
  rather than a code gap.
- The production shape of this decision is not "read the header" but "read the header from trusted
  hops only": register `ForwardedHeaderFilter` (or set Boot's `forward-headers-strategy`) *and*
  configure the trusted-proxy allow-list to match the actual ingress, so that headers arriving from
  anywhere else are discarded. Both halves are required; the filter alone reintroduces the spoofing
  hole.
- Running the service locally, redemption cannot succeed. That is expected and documented in the
  project README and in `http/README.md`: `127.0.0.1`/`::1` are excluded outright, and under
  `docker compose` the address the application sees is the Docker network gateway — a private address
  the provider answers with `"success": false`. Either way the answer is `503 GEO_IP_LOOKUP_FAILED`.
- Reading the raw peer address also keeps the flow simple: there is one source of the client IP, in
  one line of the controller, and no precedence rules to reason about.

## Status

ACCEPTED

## Authors

Krystian Witek
