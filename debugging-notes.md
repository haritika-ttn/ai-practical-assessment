# Debugging Notes — Support Ticket Management System

**Project:** AI Practical Assessment (AEMaaCS)  
**Document type:** Issues encountered and resolved during development  
**Last updated:** 2026-08-31  
**Related:** [review-fixes.md](review-fixes.md) (RF-001–RF-006), [tool-workflow.md](tool-workflow.md) §7, [code-review-notes.md](code-review-notes.md)

This document records **debugging incidents** with symptoms, root cause, fix, and validation. Only issues evidenced in project artifacts and session history are included. Formal code-review findings not yet fixed are listed separately as **open**.

---

## Summary

| ID | Category | Symptom | Root cause | Status |
|----|----------|---------|------------|--------|
| DBG-01 | Build / scaffold | Partial archetype output; nested `support-tickets/` folder | Archetype v57 failed at dispatcher symlinks on Windows | **Resolved** — manual completion + flat repo root |
| DBG-02 | Build / Maven | Reactor fails: `dispatcher.cloud` module missing | Duplicate `<module>dispatcher.cloud</module>` in parent POM | **Resolved** |
| DBG-03 | Build / Maven | Dispatcher assembly may fail | Missing `enabled_vhosts/default.vhost` (symlink failure) | **Resolved** — file copied |
| DBG-04 | Build / Java | PATH default JDK 11 vs project Java 21 | `JAVA_HOME` not set to JDK 21 | **Resolved** — session `JAVA_HOME` + POM alignment |
| DBG-05 | Build / ui.apps | `mvn clean install` compile failure | HTL references Forms/CIF Java APIs not on classpath | **Resolved** — remove archetype sample components |
| DBG-06 | Runtime / API | `GET /bin/support-tickets/users.json` → 404 | Path servlet does not resolve nested `/bin` URLs | **Resolved** — `SupportTicketsApiResourceProvider` |
| DBG-06a | Runtime / API | 404 persisted after filter added | REQUEST filter runs after Sling resource resolution | **Abandoned** — filter removed |
| DBG-07 | Runtime / API | `GET /users.json` returns `[]` | `Resource.getChildren()` on Oak authorizable path | **Resolved** — `UserManager` + JCR-SQL2 (RF-003) |
| DBG-07a | Runtime / API | Still `[]` after repoinit ACLs | ACLs necessary but insufficient for listing users | **Resolved** by RF-003 |
| DBG-08 | Runtime / UI | Create ticket: "Unable to reach server"; no POST in Network tab | Invalid header `:cq_csrf_token` → sync `fetch()` TypeError | **Resolved** — `CSRF-Token` only (RF-001) |
| DBG-09 | Runtime / UI | No navigation after successful create | Missing redirect in `create.js` success handler | **Resolved** (RF-002) |
| DBG-10 | Build / test | Compilation error in `UserLookupServiceImpl` | Wrong JCR import package (`javax.jcr` vs `javax.jcr.query`) | **Resolved** (RF-005) |
| DBG-11 | Build / test | `UnfinishedStubbingException` in user lookup test | Nested Mockito `when()` in helper | **Resolved** (RF-006) |

**Open (documented, not fixed in code):** Dispatcher `/bin/*` filter gap (CR-001), API doc CSRF drift (CR-007) — see [§Open issues](#open-issues-documented-not-fixed).

---

## Diagnostic patterns learned

| Observation | Interpretation |
|-------------|----------------|
| Network tab shows **no** request | Likely client-side `fetch()` failure before network (invalid headers, sync throw) |
| Endpoint 404 on nested `/bin/...` path | Path servlet registration may not cover sub-paths; check ResourceProvider |
| REQUEST filter does not fix 404 | Sling resolves resource before filter; filter cannot create missing resource |
| API returns `[]` but repoinit users exist | ACLs ≠ correct lookup API; Oak authorizables are not `Resource` children |
| AEM Mock tests pass; Author fails | Repoinit, authorizable semantics, CSRF, servlet routing need live Quickstart |

---

## Build and foundation

### DBG-01 — Partial AEM archetype scaffold

| Field | Detail |
|-------|--------|
| **When** | Aug 27, 2026 (foundation phase) |
| **Symptom** | `mvn archetype:generate` produced output under `support-tickets/` subfolder; generation incomplete (dispatcher symlinks) |
| **Root cause** | Adobe AEM Project Archetype v57 partial failure on Windows environment |
| **Fix** | Complete foundation manually per implementation plan; **developer-directed** move of all modules to flat repo root (`d:\ai-practical-assessment\`) |
| **Validation** | Project structure matches AEM multi-module layout; `mvn validate` succeeds after DBG-02/03 fixes |

### DBG-02 — Duplicate dispatcher module in parent POM

| Field | Detail |
|-------|--------|
| **Symptom** | `Child module D:\ai-practical-assessment\dispatcher.cloud does not exist` |
| **Root cause** | Parent `pom.xml` listed both `<module>dispatcher</module>` and `<module>dispatcher.cloud</module>`; only `dispatcher/` exists (artifact `support-tickets.dispatcher.cloud`) |
| **Fix** | Remove duplicate `<module>dispatcher.cloud</module>` |
| **Validation** | Maven reactor loads all modules |

### DBG-03 — Missing Dispatcher vhost symlink

| Field | Detail |
|-------|--------|
| **Symptom** | Potential dispatcher assembly failure during `package` |
| **Root cause** | `dispatcher/src/conf.d/enabled_vhosts/default.vhost` missing — Windows symlink step failed during archetype |
| **Fix** | Copy `default.vhost` into `enabled_vhosts/` |
| **Validation** | Dispatcher module builds in full reactor ([test-results.md](test-results.md)) |

### DBG-04 — Java version mismatch

| Field | Detail |
|-------|--------|
| **Symptom** | Build environment used JDK 11 via default `JAVA_HOME`; project targets Java 21 (`.cloudmanager/java-version`) |
| **Root cause** | PATH/`JAVA_HOME` pointed to `jdk-11.0.16.1` while JDK 21.0.9 installed separately |
| **Fix** | Set session `JAVA_HOME` to JDK 21.0.9; update parent POM compiler `<release>` to 21 |
| **Validation** | `java --version` / `mvn -version` report 21.0.9; `mvn -B clean install` BUILD SUCCESS |

### DBG-05 — ui.apps HTL compile failure (Forms / Commerce samples)

| Field | Detail |
|-------|--------|
| **Symptom** | `mvn clean install` fails at `ui.apps` compile: `package com.adobe.cq.forms.core.components.models.form does not exist` (and Commerce `Header` model) |
| **Root cause** | Archetype generated adaptiveForm and commerce sample HTL despite `includeForms=n` / `includeCif=n`. `htl-maven-plugin` generates Java from HTL; Forms/CIF APIs are not on `ui.apps` classpath (`aem-sdk-api` only) |
| **Offending files** | `adaptiveForm/page/customheaderlibs.html`, `customfooterlibs.html`, `commerce/logo/logo.html` |
| **Fix** | Remove unused Forms/Commerce component trees, related `ui.config`/`ui.content` artifacts, and `ui.frontend.react.forms.af` module (per foundation plan) — not add product dependencies |
| **Validation** | Full reactor BUILD SUCCESS; HTL validation passes for remaining 8 scripts |

---

## Runtime — API routing (DBG-06)

### Symptom

On AEM Author `:4502`:

```text
Resource at '/bin/support-tickets/users.json' not found
```

List endpoint `GET /bin/support-tickets.json` worked; nested paths (`/users.json`, `/{id}.json`, `/status.json`, `/comments.json`) returned **404**.

### Investigation (3 cycles)

| Attempt | Approach | Result |
|---------|----------|--------|
| 1 | Assume single path servlet covers all URLs | **Fail** — nested paths not dispatched |
| 2 | Add `SupportTicketsApiFilter` to forward nested requests | **Fail** — still 404; filter runs **after** resource resolution |
| 3 | Add `SupportTicketsApiResourceProvider` + servlet bound to synthetic resource type; remove filter | **Pass** |

### Root cause

Sling path servlet registration matched only `/bin/support-tickets.json`. Nested URLs require a **resource** for Sling to dispatch. REQUEST-scope filters cannot substitute for missing resources.

### Fix

- **Added:** `SupportTicketsApiResourceProvider` — supplies synthetic resources for nested API paths  
- **Removed:** `SupportTicketsApiFilter` (ineffective)  
- **Files:** `core/.../SupportTicketsApiResourceProvider.java`, servlet OSGi registration

### Validation

- Author smoke: nested endpoints reachable  
- `mvn -B -pl core test` — 131 tests PASS  
- `SupportTicketsApiIntegrationTest` includes `ac120_getUsersReturns200`

---

## Runtime — empty user list (DBG-07)

### Symptom

`GET /bin/support-tickets/users.json` returns HTTP 200 with body `[]`. Create-ticket dropdown empty. Repoinit users (`agent1`, `agent2`, `supervisor1`) present in repository.

### Investigation (2 cycles)

| Attempt | Approach | Result |
|---------|----------|--------|
| 1 | Add repoinit ACLs: `jcr:read` for `support-tickets-service` on `/home/users` and `/home/users/support` | **Fail** — still `[]` (developer confirmed ACLs applied) |
| 2 | Replace `Resource.getChildren()` with `UserManager.findAuthorizables()` + JCR-SQL2 fallback | **Pass** |

### Root cause

Oak authorizable folders under `/home/users/support/` do **not** expose users as listable child `Resource` nodes. ACLs allow read access but the lookup algorithm was wrong.

### Fix (RF-003, RF-004)

| Change | File |
|--------|------|
| ACL grants (prerequisite) | `ui.config/.../RepositoryInitializer~supporttickets.cfg.json` |
| UserManager + JCR-SQL2 lookup | `UserLookupServiceImpl.java` |
| Unit tests | `UserLookupServiceImplTest.java` |

### Follow-on build breaks (same fix branch)

- **RF-005:** Wrong imports `javax.jcr.Query` → `javax.jcr.query.Query`  
- **RF-006:** Mockito nested stubbing in test helper → refactored `mockUser`

### Validation

- `mvn -B -pl core test` — BUILD SUCCESS, 131 tests including `UserLookupServiceImplTest`  
- Manual Author: `users.json` populated [session history; not in test-results.md]

---

## Runtime — frontend (DBG-08, DBG-09)

### DBG-08 — CSRF header blocks all mutating calls (RF-001)

| Field | Detail |
|-------|--------|
| **Symptom** | `api.createTicket()` fails with "Unable to reach server. Check connection and try again." **No POST** in browser Network tab. `GET /libs/granite/csrf/token.json` returns **200**. |
| **Root cause** | `api.js` set `headers[':cq_csrf_token']` — colon-prefixed name is **invalid** for `fetch()` → synchronous `TypeError` before HTTP layer |
| **Fix** | Remove `:cq_csrf_token` assignment; send only valid Granite header `CSRF-Token` |
| **File** | `ui.apps/.../clientlib-support-app/js/api.js` |
| **Validation** | Developer: POST visible in Network tab on Author `:4502` [not recorded in test-results.md] |

> **Note:** `api-contract.md` still documents `:cq_csrf_token` (CR-007). Code is correct; docs lag.

### DBG-09 — Missing post-create redirect (RF-002)

| Field | Detail |
|-------|--------|
| **Symptom** | Ticket created successfully but user remains on create page (AC-001 UI expectation) |
| **Root cause** | `create.js` success handler did not navigate |
| **Fix** | `window.location.href` to `/content/support-app/ticket.html?id={ticketId}&created=1` |
| **File** | `ui.apps/.../clientlib-support-app/js/create.js` |
| **Validation** | Code inspection; manual Author test [not in test-results.md] |

**Developer approval:** Fixes applied after explicit *"implement 1 and 2"* instruction (CSRF + redirect).

---

## What AEM Mock did not catch

| Issue | Why Mock missed it |
|-------|-------------------|
| Nested `/bin` 404 | Servlet routing and ResourceProvider behaviour on live Sling |
| Empty `users.json` | Oak authorizable folder semantics; repoinit + UserManager |
| CSRF `fetch()` failure | Browser header validation; Granite token endpoint |
| Dispatcher blocking (CR-001) | No Dispatcher in Mock or recorded validate run |

Integration test base **mocks** `UserLookupService` and `QueryBuilder` — real implementations validated by unit tests + manual AEM only (CR-008).

---

## Open issues (documented, not fixed)

These were identified during debugging or formal review but **not resolved** in source at time of writing:

| ID | Issue | Symptom / risk | Reference |
|----|-------|----------------|-----------|
| CR-001 | Dispatcher may block `/bin/support-tickets` on Publish | 403 or silent failure through Dispatcher; generic `/bin/*` allow commented out | [code-review-notes.md](code-review-notes.md) |
| CR-007 | API contract CSRF header drift | Docs say `:cq_csrf_token`; code uses `CSRF-Token` | [code-review-notes.md](code-review-notes.md) |
| — | Publish/Dispatcher end-to-end | Not validated in artifacts | [test-results.md](test-results.md) §2.4 |
| — | Formal manual UI test log | CSRF/redirect fixes confirmed in session only | [test-results.md](test-results.md) |

---

## Fix index (RF-001 – RF-006)

| RF | DBG | Summary |
|----|-----|---------|
| RF-001 | DBG-08 | CSRF header fix in `api.js` |
| RF-002 | DBG-09 | Create redirect in `create.js` |
| RF-003 | DBG-07 | UserManager user lookup |
| RF-004 | DBG-07 | Repoinit ACLs (prerequisite) |
| RF-005 | DBG-10 | JCR query import fix |
| RF-006 | DBG-11 | Mockito test fix |

Full fix details: [review-fixes.md](review-fixes.md) Part A.

---

## Related documents

| Document | Content |
|----------|---------|
| [review-fixes.md](review-fixes.md) | Implemented fixes + proposed review fixes |
| [tool-workflow.md](tool-workflow.md) | Debugging workflow and iterative cycles |
| [tool-specific/cursor-workflow/cursor-workflow.md](tool-specific/cursor-workflow/cursor-workflow.md) | How Cursor Agent was used during debugging |
| [reflection.md](reflection.md) | First-person lessons from debug episodes |
| [test-results.md](test-results.md) | Automated test evidence (131/131 PASS) |
