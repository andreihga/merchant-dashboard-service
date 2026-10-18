---
name: add-test
description: Add or change a test for this merchant dashboard service. Use this skill whenever a task adds a test, a Flyway check, a service unit test, a security case, a period or percent unit test, a MockMvc API test, or an end-to-end dashboard call, even if the user does not say "skill".
---

# Add a test

Read the named task in `docs/EPIC.md` and test only what that task asks for.

Inspect the production code, `src/main/resources/openapi/openapi.yaml`, and existing tests before adding one. Decide the boundary first. Pure business logic is a unit test. A status, response body, or query result is an API test on H2.

Assert observable behavior: a status, a body, a window, a count, or a percent. Cover the success, failure, and edge cases the task's acceptance criteria name. Do not assert private structure, and do not add cases the task does not ask for.

## Naming

Name every `@Test` and `@ParameterizedTest` method in full snake_case as `given_x_when_y_then_z`. Example: `given_no_token_when_get_revenue_trend_then_returns_401`. Do not mix camelCase inside the segments. `@BeforeEach` and private helpers stay descriptive and do not use that form.

## Service unit tests

Use JUnit 5. Period, percent, and other pure logic tests do not start the application context. Pass a fixed `Clock` so "today" does not move between runs. Do not call `Clock.systemUTC()` inside the assertion path.

Mock a collaborator only when the test is about business logic and that collaborator is not what the assertion proves. Do not mock the database or a repository to stand in for a query. A result that comes from stored rows belongs in an API test.

## Creating objects

When a test needs a Java object, create it with Instancio. Set every field the assertion or the query depends on, such as id, status, amount, date, or merchant. Leave the other fields to Instancio so the test stays deterministic. A test that creates no object, such as a Flyway check, does not need Instancio.

## Shared helpers

Reusable test code lives in `src/test/java/com/awin/merchantdashboard/helpers/TestHelpers.java`. Read it before writing a test.

- `user(username)`, `user(username, passwordHash)`, `merchant(id, name)`, and `link(userId, merchantId)` return unsaved Instancio objects. Save them with the repository in the test.
- `bearer(jwtEncoder, userId)` returns a ready `Authorization` header value. Autowire the `JwtEncoder` bean.
- Do not copy `user`, `merchant`, `link`, or `bearer` into a test class. Add a new `public static` helper to `TestHelpers` only when a second test class needs it. Until then, keep it private in the one class that uses it.

## API integration tests

API and persistence tests use `@SpringBootTest` and MockMvc. The database is H2 in PostgreSQL mode (`MODE=PostgreSQL`). Flyway applies V1–V3 from `src/main/resources/db/migration`, the same scripts production uses.

The test profile does not load `db/seed`. Tables start empty. Insert the rows the test needs in the test itself. A test that depends on the local demo user or merchant 1001 is wrong, because production and CI do not load that seed.

Assert the status and the response body. When the task is about a bad request, assert `application/problem+json`. When the response is computed from stored rows, the test must go through H2, not a mock.

When the case calls the API, send a Bearer JWT from `TestHelpers.bearer(...)`. Do not call `POST /api/v1/auth/token` to get one. Only the login tests in `AuthSecurityTest` exercise that endpoint. A missing or invalid token is 401. A known merchant the caller is not linked to is 403. An unknown `merchantId` is 404.

When the case depends on a window, inject a fixed `Clock`.

## After writing it

Run the targeted test and report the actual result. Do not claim it passed unless the run succeeded. Say what the task left untested, and any environment limit that blocked a run.

Hand-written code under `src/main` stays at or above 90% line coverage. JaCoCo enforces that, and `build/generated` is excluded. Report the coverage result. If the build does not have JaCoCo yet, say so. A change that drops coverage below 90% is not done.
