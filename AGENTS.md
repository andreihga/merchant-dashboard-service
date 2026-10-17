# Agent notes

`docs/EPIC.md` is the source of truth. Read the named task and the business rules it depends on. Implement only that task. Do not start the next task. When it is done, tick its checkbox in `docs/EPIC.md`.

## Skills

- When a change needs a test, follow `.cursor/skills/add-test/SKILL.md`.
- When a change adds or changes an endpoint, follow `.cursor/skills/add-endpoint/SKILL.md`.

## General

- Keep changes small, focused, and consistent with existing code.
- Avoid unnecessary abstractions and dependencies.
- Do not change the OpenAPI contract or unrelated files unless the task asks for it.
- Do not commit credentials. Local secrets come from a gitignored `.env`. The rule is in `docs/EPIC.md`.

## Stack

- Gradle Kotlin DSL (`build.gradle.kts`).
- Java 25 (toolchain 25).
- Spring Boot 4.

## Generated code

Do not edit `build/generated`. The OpenAPI Generator writes models, `*ApiController`, and `*ApiDelegate` there. Hand-written code implements the delegates only. The contract is `src/main/resources/openapi/openapi.yaml`.

## Flyway

- `src/main/resources/db/migration` (V1–V3) is schema only. The scripts are ANSI SQL, so the same files run on PostgreSQL and on H2 in PostgreSQL mode, including production. Tables are empty afterward.
- Demo rows live in `db/seed`. Only the `local` profile adds that folder to Flyway locations.
- Tests run V1–V3 on empty H2 and insert their own rows. They do not load `db/seed`.

## Testing

- Add or update tests for meaningful behavior changes, including service unit tests and API integration tests when the task calls for them.
- When a test creates an object, use Instancio. Set the fields the assertion depends on.
- Hand-written `src/main` stays at or above 90% line coverage. `build/generated` is excluded.
- Run the relevant tests and report the actual result.
- Do not claim a pass unless the run succeeded.

## Workflow

- Inspect existing code before changing it.
- For a non-trivial task, state the approach first.
- Stay inside the named task and its acceptance criteria in `docs/EPIC.md`.
- Report files changed, commands run, results, and limitations.
- Do not commit or push. The user commits and pushes.
- Ask before a significant architecture change or a destructive action.

## Definition of done

- The task's acceptance criteria are met.
- Relevant tests are added or updated and executed.
- Hand-written line coverage stays at or above 90%.
- The diff is reviewed for correctness, security, and complexity.
