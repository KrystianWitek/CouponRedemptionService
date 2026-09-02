# One feature slice, ports and adapters, beans wired by hand

## Context

The service has a single capability — create a coupon, redeem a coupon — with one external dependency
(a GeoIP provider) and one store (PostgreSQL). Two structures were plausible: the conventional
technical layering (`controller` / `service` / `repository` / `entity` packages at the top level), or
a feature slice with an explicit dependency direction.

The choice matters here more than the size of the service suggests, because the correctness of the
redemption flow rests on the persistence adapter being free to issue hand-written conditional SQL
([the database is the arbiter](2026-09-02-concurrency-the-database-is-the-arbiter.md)). An
architecture in which the domain model *is* the JPA entity makes that awkward: the same class is then
both the thing the business rules talk about and the thing Hibernate dirty-checks and flushes.

## Decision

One package per feature (`coupon`), internally arranged as ports and adapters with the dependency
direction `api → application → domain ← infrastructure`:

- **`domain/`** — `Coupon`, `CouponRedemption`, the value objects, the repository ports
  (`CouponRepository`, `CouponRedemptionRepository`) and the GeoIP port (`GeoIpProvider`,
  `GeoIpLookupException`). No Spring, Jakarta or Hibernate imports at all.
- **`application/`** — one file per use case, each declaring a public `interface` and an
  `internal class Default*` implementation, with its `*Command` data class in a file of its own.
  Business failures are typed exceptions (`CouponExceptions.kt`) carrying value objects, not
  formatted strings.
- **`infrastructure/`** — the adapters: JPA entities and their mappers, the `@Repository` adapter
  classes co-located with their `Jpa*Repository` interfaces, the GeoIP `RestClient` and its adapter.
- **`api/`** — the controller, the request and response DTOs with their Jakarta constraints in
  `dto/`, the mappers that convert request → command (calling the domain `from()` factories) and
  domain → response, and the `@RestControllerAdvice` that owns the error contract.

**JPA entities are separate classes from the domain models.** They share their names, so imports
alias them (`import ...entity.Coupon as CouponEntity`), and `mapper/CouponMappers.kt` translates.
The cost is a mapper to write and keep in step. What it buys is that the domain model is shaped by
the business — `Coupon` is an immutable `data class` with an `init` invariant and an `isExhausted`
property, holding value objects rather than `String`s — while the entity is shaped by JPA (a
no-arg-constructible class with a `var` for the counter and plain column types). And it leaves the
adapter free to bypass the entity entirely and issue native statements, which is exactly what the
concurrency design needs.

**Application beans are wired by hand.** `DefaultCouponCreationService` and
`DefaultCouponRedemptionService` carry no `@Service`; they are constructed in `CouponConfiguration`:

```kotlin
@Configuration
internal class CouponConfiguration {
    @Bean
    fun couponRedemptionService(...): CouponRedemptionService =
        DefaultCouponRedemptionService(...)
}
```

Three reasons:

1. **The application layer stays free of Spring annotations.** Its unit tests build the service with
   a plain constructor and hand-written fakes (`InMemoryCouponRepository`, `FakeTransactionOperations`)
   — no context, no component scan, no proxying, tests that run in milliseconds.
2. **The wiring of the slice is readable in one file.** What depends on what is a list of constructor
   calls rather than something to be reconstructed by searching for annotations. A new use case means
   a new `@Bean` in the same place.
3. **Only the interfaces are public.** The implementations are `internal`, so the seam is enforced by
   the compiler rather than by convention: nothing outside the module can depend on
   `DefaultCouponRedemptionService`.

The only component-scanned beans in the slice are the `@Repository` adapters, the `@Configuration`
classes, and the `@RestController` / `@RestControllerAdvice`.

## Consequences and Risks

- The dependency rule is checkable by reading imports, and it holds: the single framework import in
  `domain/` and `application/` combined is `TransactionOperations`, which is
  [a deliberate exception](2026-09-02-redemption-is-inserted-before-the-counter-is-incremented.md).
- Swapping an adapter is a local change. The GeoIP provider is one class behind one port; replacing
  `ipwho.is`, or adding a cache or circuit breaker in front of it, touches `infrastructure/geoip` and
  nothing else.
- **Deliberate trade-off: the whole JPA stack currently serves one derived query, `findByCode`.**
  Every write goes through native SQL, so Spring Data's repository machinery is barely used, and a
  smaller stack (Spring Data JDBC, or `JdbcClient`) would cover today's needs. JPA is kept because
  the mapped entities are what makes Hibernate's `ddl-auto: validate` meaningful: on every startup
  the entity definitions are checked against the Flyway-owned schema, so a migration that drifts from
  the code fails the application at boot rather than at the first query. The entities earn their
  place as a schema guard rather than as a query engine.
- Manual wiring costs a few lines per bean and a constructor signature that has to be updated when a
  dependency is added — Spring will not silently discover the new collaborator. For a slice this size
  that explicitness is worth more than the saved typing; a codebase with dozens of use cases per
  slice might reasonably decide otherwise.
- A second capability would be added as a sibling package with the same internal shape, not by
  growing shared `service`/`repository` packages. The entity name aliasing convention
  (`as CouponEntity`) is the one local habit a newcomer has to notice.

## Status

ACCEPTED

## Authors

Krystian Witek
