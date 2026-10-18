---
name: add-endpoint
description: Add or change a merchant dashboard HTTP endpoint. Use this skill whenever a task adds a path, a delegate, or a response field, even if the user does not say "skill".
---

# Add an endpoint

Read the named task in `docs/EPIC.md` and implement only that path.

## Contract first

Edit `src/main/resources/openapi/openapi.yaml`. That file is the contract, including the bearer scheme and `application/problem+json` errors. Then run the OpenAPI generator. It writes models, `*ApiController`, and `*ApiDelegate` into `build/generated`.

The hand-written class implements the generated `*ApiDelegate` and lives in that domain's `controller` package: `auth.controller`, `merchant.controller`, `kpi.controller`, `trend.controller`, or `todo.controller`. It calls the service in the same domain. It does not subclass the generated `*ApiController`, because that controller is already a Spring bean and a subclass would register the same routes twice. Do not edit anything in `build/generated`.

## Merchant access

On a route under `/merchants/{merchantId}`, call `MerchantAccessGuard` in `merchant.service` before reading that merchant's data.

- A missing or invalid token is 401.
- An unknown `merchantId` is 404.
- A known merchant with no `user_merchant` row for the caller is 403.

`GET /merchants` has no merchant id in the path. Return only the merchants linked to the caller.
