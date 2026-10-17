---
name: add-endpoint
description: Add or change a merchant dashboard HTTP endpoint. Use this skill whenever a task adds a path, a delegate, or a response field, even if the user does not say "skill".
---

# Add an endpoint

Read the named task in `docs/EPIC.md` and implement only that path.

## Contract first

Edit `src/main/resources/openapi/openapi.yaml`. That file is the contract, including the bearer scheme and `application/problem+json` errors. Then run the OpenAPI generator. It writes models, `*ApiController`, and `*ApiDelegate` into `build/generated`.

Implement the generated `*ApiDelegate` in the feature package under `com.awin.merchantdashboard`. Do not edit anything in `build/generated`, including the generated controller.

## Merchant access

On a route under `/merchants/{merchantId}`, call `MerchantAccessGuard` before reading that merchant's data.

- A missing or invalid token is 401.
- An unknown `merchantId` is 404.
- A known merchant with no `user_merchant` row for the caller is 403.

`GET /merchants` has no merchant id in the path. Return only the merchants linked to the caller.
