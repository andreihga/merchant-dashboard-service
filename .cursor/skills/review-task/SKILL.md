---
name: review-task
description: Reviews one finished task against docs/EPIC.md and the git diff. Use when the user asks to review a task, the implementation, or the diff. Does not edit files.
disable-model-invocation: true
---

# Review one task

Review only. Do not edit files, commit, or start the next task.

## Scope

1. Read the named task in `docs/EPIC.md`, plus the business rules it depends on.
2. Read `git status` and `git diff`. Review that diff only.
3. Ignore generated files under `build/generated`.

## Check

- The diff does that task and does not start the next one.
- Behavior matches the epic, including 401, 403, 404, and 400 where that task includes them.
- Merchant data is returned only when a `user_merchant` row exists for the caller.
- Logs do not contain the password, the token, or the response body.
- Committed YAML and Compose do not contain a database password, an HMAC or JWT signing key, or another credential. Local secrets stay in `.env`.
- Tests cover the behavior this task added. They use H2, the V1–V3 Flyway scripts, and their own rows. They do not load `db/seed`.
- New or renamed `@Test` / `@ParameterizedTest` methods use full snake_case `given_x_when_y_then_z` naming.
- Tests outside `AuthSecurityTest` do not call `POST /api/v1/auth/token`. They use `TestHelpers.bearer(...)`, and they reuse `TestHelpers` helpers instead of copying them.
- The task checkbox matches what landed.
- JaCoCo line coverage of hand-written `src/main` is at or above 90%. See "Coverage" below.

## Coverage

Run `./gradlew test jacocoTestCoverageVerification`. This writes only under `build/`, so it does not break the review-only rule. `build/generated` and `com/awin/merchantdashboard/api/**` are excluded by `build.gradle.kts`.

- Report the actual result: pass or fail, and the overall line coverage from `build/reports/jacoco/test/html/index.html` or `build/reports/jacoco/test/jacocoTestReport.xml`.
- If the gate fails, that is a high-severity finding. Name the classes with the lowest line coverage that this diff added or changed.
- If a class this diff added or changed has no tests for its new branches (for example 401, 403, 404, 400), flag it even when the overall gate passes.
- If the run cannot happen (build error, missing JDK, no network), say so. Do not claim coverage passed.

## Reply

Lead with findings, ordered by severity. For each finding, name the file and what is wrong. Include the JaCoCo result (command run, pass or fail, line coverage). If nothing is wrong, say the task matches the epic and list what you checked.
