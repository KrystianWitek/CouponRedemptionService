# Value objects with a private constructor and a `from()` factory

## Context

Three pieces of data travel through every layer of the service and every one of them has rules
attached: a coupon code (non-blank, case-insensitive), a country code (ISO 3166-1 alpha-2), and a
user identifier (non-blank). Modelled as `String`, all three are interchangeable at every call site
— nothing stops a user id being passed where a coupon code is expected — and each rule has to be
re-applied, or forgotten, at every boundary that handles them.

Case-insensitivity of the coupon code is the interesting one. It is a business rule ("`WIOSNA` and
`wiosna` are the same coupon"), and it can be satisfied in several places: in the database
(a functional `UNIQUE` index on `LOWER(code)`, or the `citext` type), in every query, or once at the
edge of the domain.

## Decision

`CouponCode`, `CountryCode` and `UserId` are `@JvmInline value class`es with a **private
constructor** and a `from()` factory that normalises first and validates second:

```kotlin
@JvmInline
value class CouponCode private constructor(
    val value: String,
) {
    companion object {
        fun from(value: String): CouponCode {
            val normalizedValue = value.trim().uppercase()
            require(normalizedValue.isNotEmpty()) { "Coupon code must not be blank" }

            return CouponCode(normalizedValue)
        }
    }
}
```

- `CouponCode.from` trims and upper-cases,
- `UserId.from` trims,
- `CountryCode.from` trims, upper-cases, requires two letters, and then requires membership of
  `Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2)`.

The private constructor is the load-bearing part: there is no way to obtain an instance that skipped
normalisation or validation, so "is this value valid?" is answered once, by the type. `CouponId` and
`CouponRedemptionId` are value classes too, but with public constructors — they wrap a `UUID` and
have nothing to validate.

Conversion happens exactly at the API boundary, in `CouponApiMappers.kt`, where a request DTO is
turned into a command. Everything downstream — service, domain, repository ports, mappers, native
SQL parameters — carries the already-normalised type.

## Consequences and Risks

- **Case-insensitivity is free, and it is free at exactly one place.** Because a `CouponCode` cannot
  exist un-upper-cased, the value that reaches the database on write and on lookup is already
  canonical. A plain `UNIQUE` on `coupon.code` is therefore sufficient: no functional index on
  `LOWER(code)`, no `citext` extension, no risk of a query path that forgot to normalise and so
  silently missed an existing coupon or created a near-duplicate. The same normalisation serves
  `POST /api/v1/coupons` and `POST /api/v1/coupons/redeem` without either endpoint mentioning it.
- **`CountryCode` moves a whole class of failure to the edge.** `XX` is a well-formed two-letter
  string but not a country, and a coupon created for `XX` could never be redeemed by anyone. Because
  the ISO check lives in the factory, the request is rejected with `400 INVALID_COUNTRY_CODE`
  instead of persisting a coupon that is dead on arrival. The DTO's own
  `@Pattern(regexp = "[A-Za-z]{2}")` catches shape; the value object catches meaning.
- The same factory is reused to interpret the GeoIP provider's answer. A provider response carrying a
  country code the JDK does not recognise fails `CountryCode.from`, and the adapter turns that into
  a lookup failure rather than letting an unknown country reach the comparison.
- `require` failures are `IllegalArgumentException`, which is a domain-level signal rather than an
  HTTP concern. The API layer translates the case that a client can actually cause —
  `InvalidCountryCodeException` → `400` — while a blank code or blank user id is already rejected
  earlier by `@NotBlank` on the DTO, so those `require`s act as a second line of defence rather than
  as the primary contract.
- `@JvmInline` keeps the abstraction close to free at runtime: the wrapper is erased to the
  underlying `String` in most positions, so type safety here costs allocations only where boxing is
  unavoidable.
- The cost is ceremony: `CouponCode.from(code)` at the boundary and `.value` when handing the raw
  string to SQL or to a response DTO. It also means the constructors must never be made public and
  new call sites must go through `from()` — a convention the compiler enforces for the constructor
  but not for the habit of using the type in the first place.

## Status

ACCEPTED

## Authors

Krystian Witek
