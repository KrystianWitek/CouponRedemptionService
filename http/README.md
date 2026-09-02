# HTTP requests

[`coupons.http`](coupons.http) holds runnable requests for both endpoints. Open it in IntelliJ IDEA
(HTTP Client) or in VS Code (REST Client) and run them against a locally started application. Two
variables at the top of the file control where the requests go and which coupon they use:

| Variable      | Default                 | Description                                  |
|---------------|-------------------------|----------------------------------------------|
| `@baseUrl`    | `http://localhost:8080` | Address of the running application            |
| `@couponCode` | `WELCOME10`             | Coupon code shared by both requests           |

Coupon codes are trimmed and upper-cased before they are stored or looked up, so `welcome10` and
`WELCOME10` address the same coupon. Both endpoints answer with `201 Created` on success; the error
contract is described in the [main README](../README.md#errors).

## Create a coupon

```http
POST {{baseUrl}}/api/v1/coupons
Content-Type: application/json

{
  "code": "WELCOME10",
  "maxUsageCount": 100,
  "countryCode": "PL"
}
```

```json
{
  "id": "0f2b8f1e-6a5c-4f7b-9d3e-2c1a5b8e7d40",
  "code": "WELCOME10",
  "createdAt": "2026-09-02T10:15:30.123456Z",
  "maxUsageCount": 100,
  "currentUsageCount": 0,
  "countryCode": "PL"
}
```

Running it a second time with the same code answers `409 Conflict` with `COUPON_ALREADY_EXISTS`.

## Redeem a coupon

The caller's country is resolved from the client IP address of the request; no country is taken from
the payload.

```http
POST {{baseUrl}}/api/v1/coupons/redeem
Content-Type: application/json

{
  "code": "WELCOME10",
  "userId": "user-123"
}
```

```json
{
  "id": "6d4c9a02-1f77-4a58-8f0b-9c3d21e4b5a6",
  "code": "WELCOME10",
  "userId": "user-123",
  "redeemedAt": "2026-09-02T10:16:04.987654Z"
}
```

Run from the same machine as the application, this request answers `503 Service Unavailable` with
`GEO_IP_LOOKUP_FAILED`: the public GeoIP provider cannot resolve local and private addresses.
Repeating it for a `userId` that already redeemed the coupon answers `409 Conflict` with
`COUPON_ALREADY_REDEEMED`.
