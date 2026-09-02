# One feature slice, ports and adapters, beans wired by hand

## Context

The service has two operations (create a coupon, redeem a coupon), one external dependency (a GeoIP
provider) and one store (PostgreSQL). Two structures were plausible: conventional technical layering
(`controller` / `service` / `repository` / `entity` packages at the top level), or a feature slice
with an explicit dependency direction.

The choice matters more than the size of the service suggests, because the redemption flow is only
correct if the persistence adapter can issue hand-written conditional SQL ([the database is the
arbiter](2026-08-28-concurrency-the-database-is-the-arbiter.md)). An architecture in which the domain
model *is* the JPA entity makes that awkward: the same class is then both the thing the business rules
talk about and the thing Hibernate dirty-checks and flushes.

## Decision

One package per feature, arranged as ports and adapters with the dependency direction
`api → application → domain ← infrastructure`. The domain has no Spring, Jakarta or Hibernate imports.

**JPA entities are separate classes from the domain models,** so the adapter is free to bypass the
entity and issue the native statements the concurrency design rests on, and the domain model is
shaped by the business rather than by what Hibernate can map. The cost is a mapper to keep in step.

**Application beans are wired by hand,** so the application layer carries no Spring annotations: it
can be built with a plain constructor, and only its interfaces are visible outside the slice. Its one
concession to the framework is the injected transaction boundary.

## Consequences and Risks

- **Trade-off: the whole JPA stack serves one derived query, the lookup by code.** Every write goes
  through native SQL, so a smaller stack (Spring Data JDBC, or `JdbcClient`) would cover today's
  needs. JPA is kept because the mapped entities are what makes `ddl-auto: validate` meaningful: on
  every startup they are checked against the Flyway-owned schema, so a migration that drifts from the
  code fails the application at boot rather than at the first query.
- Manual wiring costs a few lines per bean, and a constructor signature that has to be updated when a
  dependency is added: Spring will not silently discover the new collaborator. For a slice this size
  that explicitness is worth it; a codebase with dozens of use cases per slice might decide otherwise.
- A second capability would be added as a sibling package with the same internal shape.

## Status

ACCEPTED, 2026-08-27.

## Authors

Krystian Witek
