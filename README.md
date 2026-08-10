# PostgreSQL JPA Query Diagnostics

This repository contains two related artifacts:

- `postgres-jpa-query-diagnostics/` — a Codex skill for diagnosing PostgreSQL, pgJDBC, Hibernate, and Spring Data query failures and writing minimal, type-safe fixes.
- `postgres-jpa-query-lab/` — a Kotlin/Spring Boot integration-test lab that provides the evidence behind the verified recipes.

The skill keeps day-to-day implementation guidance separate from the experimental evidence. A recipe is included in `verified-recipes.md` only when its failure and fix are covered by executable integration tests against PostgreSQL 18.4.

## Run the verification suite

Requirements: Java 21 and a Docker-compatible runtime.

```shell
./postgres-jpa-query-lab/gradlew -p postgres-jpa-query-lab clean verifyDiagnostics
```

See `postgres-jpa-query-lab/docs/findings.md` for resolved dependency versions and findings, and `postgres-jpa-query-lab/docs/test-matrix.md` for scenario coverage.
