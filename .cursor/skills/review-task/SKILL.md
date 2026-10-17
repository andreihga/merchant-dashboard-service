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
- The task checkbox matches what landed.

## Reply

Lead with findings, ordered by severity. For each finding, name the file and what is wrong. If nothing is wrong, say the task matches the epic and list what you checked.
