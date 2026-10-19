---
name: add-endpoint
description: Add or change a merchant dashboard HTTP endpoint. Use this skill whenever a task adds a path, a controller, or a response field, even if the user does not say "skill".
---

# Add an endpoint

Read the named task in `docs/EPIC.md` and implement only that path.

## Contract first

Edit `src/main/resources/openapi/openapi.yaml`. That file is the contract, including the bearer scheme and `application/problem+json` errors. Then run the OpenAPI generator.

The generator emits models and `*Api` interfaces only (`interfaceOnly`, no delegate). A hand-written `@RestController` in the domain's `controller` package implements the `*Api` interface and calls the service in the same domain: `auth.controller`, `merchant.controller`, `kpi.controller`, `trend.controller`, or `todo.controller`. It adds no mapping annotations. Do not edit `build/generated`.

The controller implements `*Api` and carries no mapping annotations of its own (`@RequestMapping`, `@PostMapping`, `@GetMapping`, `@Valid`). The `*Api` interface supplies them.

Name the service method the controller calls as verb + noun (`getClicks`, `getRevenue`, `listMerchants`, `issueToken`). Do not use a bare noun (`clicks`, `revenue`).

## Merchant access

On a route under `/merchants/{merchantId}`, call `MerchantAccessGuard` in `merchant.service` before reading that merchant's data.

- A missing or invalid token is 401.
- An unknown `merchantId` is 404.
- A known merchant with no `user_merchant` row for the caller is 403.

`GET /merchants` has no merchant id in the path. Return only the merchants linked to the caller.
