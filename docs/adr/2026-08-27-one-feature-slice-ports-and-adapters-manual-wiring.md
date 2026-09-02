# One feature slice, ports and adapters, beans wired by hand

## Context

The service has a single capability — create a coupon, redeem a coupon — with one external dependency
(a GeoIP provider) and one store (PostgreSQL). Two structures were plausible: the conventional
technical layering (`controller` / `service` / `repository` / `entity` packages at the top level), or
a feature slice with an explicit dependency direction.

The choice matters more than the size of the service suggests, because the correctness of the
redemption flow rests on the persistence adapter being free to issue hand-written conditional SQL
([the database is the arbiter](2026-08-28-concurrency-the-database-is-the-arbiter.md)). An
architecture in which the domain model *is* the JPA entity makes that awkward: the same class is then
both the thing the business rules talk about and the thing Hibernate dirty-checks and flushes.

## Decision

One package per feature (`coupon`), arranged as ports and adapters with the dependency direction
`api → application → domain ← infrastructure`. `domain/` has no Spring, Jakarta or Hibernate imports.

**JPA entities are separate classes from the domain models.** They share their names, so imports
alias them (`import ...entity.Coupon as CouponEntity`), and `mapper/CouponMappers.kt` translates.
The cost is a mapper to write and keep in step. What it buys is that the domain model is shaped by
the business — `Coupon` is an immutable `data class` with an `init` invariant and an `isExhausted`
property, holding value objects rather than `String`s — while the entity is shaped by JPA (a
no-arg-constructible class with a `var` for the counter and plain column types), and the adapter is
free to bypass the entity and issue the native statements the concurrency design rests on.

**Application beans are wired by hand.** `DefaultCouponCreationService` and
`DefaultCouponRedemptionService` carry no `@Service`; they are `internal` and constructed in
`CouponConfiguration`, so the application layer has no Spring annotations, can be built with a plain
constructor and hand-written stand-ins behind its ports, and exposes only its interfaces outside the
module. The only component-scanned beans in the slice are the `@Repository` adapters, the
`@Configuration` classes, and the `@RestController` / `@RestControllerAdvice`.

## Consequences and Risks

- **Trade-off: the whole JPA stack currently serves one derived query, `findByCode`.** Every write
  goes through native SQL, so Spring Data's repository machinery is barely used, and a smaller stack
  (Spring Data JDBC, or `JdbcClient`) would cover today's needs. JPA is kept because the mapped
  entities are what makes Hibernate's `ddl-auto: validate` meaningful: on every startup the entity
  definitions are checked against the Flyway-owned schema, so a migration that drifts from the code
  fails the application at boot rather than at the first query. The entities earn their place as a
  schema guard rather than as a query engine.
- Manual wiring costs a few lines per bean and a constructor signature that has to be updated when a
  dependency is added — Spring will not silently discover the new collaborator. For a slice this size
  that explicitness is worth it; a codebase with dozens of use cases per slice might decide otherwise.
- A second capability would be added as a sibling package with the same internal shape, not by
  growing shared `service`/`repository` packages.

## Status

ACCEPTED

## Authors

Krystian Witek
