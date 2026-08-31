# Review Fixes — Support Ticket Management System

**Project:** AI Practical Assessment  
**Related:** [code-review-notes.md](code-review-notes.md)  
**Document type:** Fix log and proposed-fix plan  
**Status:** Findings documented; most review recommendations **not yet implemented**  
**Last updated:** 2026-08-31

---

## Purpose

This document records:

1. **Fixes already implemented** — changes made during development/debugging (including pre-review), with validation evidence.
2. **Proposed fixes from formal code review** — decisions and planned actions for remaining findings.

Per review instructions: **no code changes were made as part of the formal review itself.** Only previously implemented fixes are marked as **Fix** with validation results.

---

## Part A — Fixes already implemented

These issues were discovered during implementation and manual testing, fixed in source, and validated before or during the formal review.

---

### RF-001 — Invalid CSRF header breaks all mutating API calls

| Field | Detail |
|-------|--------|
| **Finding reference** | Related to CR-007 (documentation drift); root cause predates formal review |
| **Original issue** | `api.js` set `headers[':cq_csrf_token']`, an invalid HTTP header name. `fetch()` threw `TypeError` synchronously before the network request was sent. UI showed "Unable to reach server. Check connection and try again." with no POST visible in Network tab. |
| **Decision** | **Fix** |
| **Reason** | Blocking defect for all create/update/status/comment operations from the browser. |
| **Files changed** | `ui.apps/src/main/content/jcr_root/apps/supporttickets/clientlibs/clientlib-support-app/js/api.js` |
| **Change made** | Removed `:cq_csrf_token` header assignment. Mutating requests now send only `CSRF-Token` (valid Granite CSRF header). |
| **Validation performed** | Manual verification on AEM Author `:4502` reported by developer: `GET /libs/granite/csrf/token.json` returns 200; POST create ticket visible in Network tab after fix. Automated: Core build/tests unaffected (frontend not in Surefire scope). |
| **Actual result** | **Fixed** — mutating `fetch()` calls proceed to the network layer. [Manual UI re-test result not recorded in project artifacts beyond developer confirmation in session history.] |

---

### RF-002 — No redirect after successful ticket creation

| Field | Detail |
|-------|--------|
| **Finding reference** | AC-001 UI expectation (redirect to detail or list) |
| **Original issue** | `create.js` success handler did not navigate after `api.createTicket()` resolved. User remained on create page with no clear success feedback. |
| **Decision** | **Fix** |
| **Reason** | AC-001 expects redirect to detail or list; poor UX without navigation. |
| **Files changed** | `ui.apps/src/main/content/jcr_root/apps/supporttickets/clientlibs/clientlib-support-app/js/create.js` |
| **Change made** | On successful create, set `window.location.href` to detail page URL with `?id={ticketId}&created=1`. |
| **Validation performed** | Code inspection; manual Author test [result not recorded in test-results.md]. |
| **Actual result** | **Fixed** in source. [End-to-end UI redirect not evidenced in automated test artifacts.] |

---

### RF-003 — `GET /users.json` returns empty array

| Field | Detail |
|-------|--------|
| **Finding reference** | AC-120 / AC-121; relates to CR-008 |
| **Original issue** | `UserLookupServiceImpl.listSeededUsers()` used `Resource.getChildren()` on `/home/users/support`. Oak authorizable folders do not enumerate users as child resources, so the method returned `[]` despite correct service-user ACLs. |
| **Decision** | **Fix** |
| **Reason** | Create-ticket and detail forms depend on user dropdown population; empty list blocks ticket creation UI. |
| **Files changed** | `core/src/main/java/com/supporttickets/core/service/impl/UserLookupServiceImpl.java`; `core/src/test/java/com/supporttickets/core/service/impl/UserLookupServiceImplTest.java` |
| **Change made** | Replaced folder traversal with `UserManager.findAuthorizables("profile/role", ...)` for AGENT and SUPERVISOR roles, plus JCR-SQL2 fallback query on `[rep:User]` under `/home/users/support`. Added unit tests. |
| **Validation performed** | `mvn -B -pl core test` — BUILD SUCCESS, 131 tests passed including `UserLookupServiceImplTest`. Manual: users.json populated on Author after deploy [per session history]. |
| **Actual result** | **Fixed** — automated unit tests pass; manual API verification reported successful in development session. |

---

### RF-004 — Repoinit ACLs for service user on user paths

| Field | Detail |
|-------|--------|
| **Finding reference** | Supporting fix for RF-003 |
| **Original issue** | `support-tickets-service` lacked explicit `jcr:read` on `/home/users` and `/home/users/support`. |
| **Decision** | **Fix** (necessary but not sufficient alone) |
| **Reason** | Service user must read authorizable nodes for `UserManager` operations. |
| **Files changed** | `ui.config/src/main/content/jcr_root/apps/supporttickets/osgiconfig/config/org.apache.sling.jcr.repoinit.RepositoryInitializer~supporttickets.cfg.json` |
| **Change made** | Added repoinit ACL blocks granting `jcr:read` to `support-tickets-service` on `/home/users` and `/home/users/support`. |
| **Validation performed** | Deployed to local Author; ACLs visible in repository. Empty-array issue persisted until RF-003 code change — confirming ACL alone was insufficient. |
| **Actual result** | **Fixed** as prerequisite. User listing resolved by RF-003. |

---

### RF-005 — JCR query import compilation error in `UserLookupServiceImpl`

| Field | Detail |
|-------|--------|
| **Finding reference** | Build break introduced during RF-003 |
| **Original issue** | `javax.jcr.Query` and `QueryManager` imported from wrong package; compilation failed. |
| **Decision** | **Fix** |
| **Reason** | Build must compile. |
| **Files changed** | `core/src/main/java/com/supporttickets/core/service/impl/UserLookupServiceImpl.java` |
| **Change made** | Corrected imports to `javax.jcr.query.Query` and `javax.jcr.query.QueryManager`. |
| **Validation performed** | `mvn -B -pl core test` — BUILD SUCCESS. |
| **Actual result** | **Fixed**. |

---

### RF-006 — Mockito stubbing error in `UserLookupServiceImplTest`

| Field | Detail |
|-------|--------|
| **Finding reference** | Test failure introduced during RF-003 |
| **Original issue** | `UnfinishedStubbingException` from nested `when()` calls in `mockUser` helper. |
| **Decision** | **Fix** |
| **Reason** | Test suite must pass. |
| **Files changed** | `core/src/test/java/com/supporttickets/core/service/impl/UserLookupServiceImplTest.java` |
| **Change made** | Refactored `mockUser` to set up `Value` mocks directly without nested stubbing. |
| **Validation performed** | `mvn -B -pl core test` — BUILD SUCCESS, 0 failures. |
| **Actual result** | **Fixed**. |

---

## Part B — Proposed fixes from formal code review

Remaining findings from [code-review-notes.md](code-review-notes.md). **None of these have been implemented yet** unless explicitly noted.

---

### CR-001 — Dispatcher may block `/bin/support-tickets` on Publish

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-001 |
| **Original issue** | `/bin/*` allow filter commented out in `dispatcher/src/conf.dispatcher.d/filters/default_filters.any`. |
| **Decision** | **Fix** (recommended before Publish deployment) |
| **Reason** | UI cannot function on Publish through Dispatcher without explicit allow rule. |
| **Files to change** | `dispatcher/src/conf.dispatcher.d/filters/` — add project filter for `/bin/support-tickets` and `/libs/granite/csrf/token.json` |
| **Proposed change** | Add allow rules; run `dispatcher/bin/validate.sh src`. |
| **Validation plan** | Dispatcher SDK validation; manual POST through Dispatcher on Publish. |
| **Actual result** | **[Not implemented]** |

---

### CR-002 — No API authorization

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-002 |
| **Original issue** | Open API by design per Core requirements. |
| **Decision** | **Accept** |
| **Reason** | Explicitly out of scope for Core (FR-S03 is Stretch). Documented in requirements and API contract. |
| **Files changed** | None |
| **Actual result** | **Accepted** — no change planned for Core. |

---

### CR-003 — Live AEM / E2E tests not executed

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-003, CR-014 |
| **Original issue** | No support-ticket ITs or Cypress runs recorded. |
| **Decision** | **Defer** (recommended for post-assessment hardening) |
| **Reason** | Requires running AEM instance and test environment setup; not blocking given 131 Core tests pass. |
| **Files to change** | New tests in `it.tests/`; Cypress specs in `ui.tests/`; update `test-results.md` after execution |
| **Proposed change** | Add API smoke IT (`POST` create, `GET` list, `PATCH` status); run Cypress create→detail flow. |
| **Validation plan** | `mvn -B clean install -Plocal` with AEM running; `npm run test` in `ui.tests`. |
| **Actual result** | **[Not implemented]** |

---

### CR-004 — QueryBuilder keyword search case-insensitivity

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-004 |
| **Original issue** | QueryBuilder LIKE path lowercases keyword but not stored values; may diverge from traversal fallback. |
| **Decision** | **Defer** (or **Fix** if mixed-case search fails in manual QA) |
| **Reason** | Traversal fallback handles case-insensitivity correctly; QueryBuilder path may only activate in indexed environments. Low ticket volume in assessment. |
| **Files to change** | `core/.../TicketSearchServiceImpl.java`; add test in `TicketSearchIntegrationTest` |
| **Proposed change** | Align both paths to case-insensitive matching; add test with `MiXeD` case title. |
| **Validation plan** | `mvn -B -pl core test`; manual search on Author. |
| **Actual result** | **[Not implemented]** |

---

### CR-005 — Archetype boilerplate removal

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-005 |
| **Original issue** | HelloWorld, SimpleServlet, LoggingFilter, scheduler, listener remain from archetype. |
| **Decision** | **Defer** |
| **Reason** | No functional impact; cleanup is hygiene, not blocking. Risk of breaking archetype tests if removed carelessly. |
| **Files to change** | `core/src/main/java/.../HelloWorldModel.java`, `SimpleServlet.java`, etc.; `ui.apps/.../helloworld/`; associated tests |
| **Proposed change** | Remove unused components and their tests in a dedicated cleanup PR. |
| **Validation plan** | `mvn -B clean install` after removal. |
| **Actual result** | **[Not implemented]** |

---

### CR-006 — Consolidate duplicated endpoint helpers

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-006 |
| **Original issue** | `isJsonRequest`, `parseJsonObject`, `requireValidTicketId` duplicated in four endpoint classes. |
| **Decision** | **Defer** |
| **Reason** | Code works and is tested; refactor is maintainability improvement only. |
| **Files to change** | New `JsonRequestSupport` utility; four endpoint classes |
| **Proposed change** | Extract shared static helpers. |
| **Validation plan** | `mvn -B -pl core test` — all integration tests must still pass. |
| **Actual result** | **[Not implemented]** |

---

### CR-007 — Documentation drift on CSRF header

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-007 |
| **Original issue** | `api-contract.md`, `design-notes.md`, `ui-flow.md` reference `:cq_csrf_token`; code uses `CSRF-Token` only. |
| **Decision** | **Fix** (documentation only) |
| **Reason** | Prevents reintroduction of RF-001 bug. Low effort, high value. |
| **Files to change** | `api-contract.md`, `design-notes.md`, `ui-flow.md` |
| **Proposed change** | Replace `:cq_csrf_token` references with `CSRF-Token`; note that `:cq_csrf_token` is invalid in `fetch()` and must not be used. |
| **Validation plan** | Doc review; cross-check against `api.js`. |
| **Actual result** | **[Not implemented]** |

---

### CR-008 — Integration-test `UserLookupService` with real implementation

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-008 |
| **Original issue** | `SupportTicketsIntegrationTestBase` mocks `UserLookupService`, so RF-003 class of bugs would not be caught in integration tests. |
| **Decision** | **Fix** (recommended) |
| **Reason** | RF-003 demonstrated the gap; real implementation test would regress-proof user listing. |
| **Files to change** | `SupportTicketsIntegrationTestBase.java`; possibly new `UserLookupIntegrationTest.java` |
| **Proposed change** | Register `UserLookupServiceImpl` with AEM Mock user nodes matching repoinit structure; add test for `listSeededUsers()`. |
| **Validation plan** | `mvn -B -pl core test`. |
| **Actual result** | **[Not implemented]** — `UserLookupServiceImplTest` provides unit coverage only. |

---

### CR-009 — Remove unused `ApiErrorMapper` methods

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-009 |
| **Original issue** | `malformedTicketId()` and `malformedJson()` are never called. |
| **Decision** | **Defer** |
| **Reason** | Dead code; no runtime impact. |
| **Files to change** | `core/.../ApiErrorMapper.java` |
| **Proposed change** | Remove unused methods or wire endpoints to use them. |
| **Validation plan** | `mvn -B -pl core test`. |
| **Actual result** | **[Not implemented]** |

---

### CR-010 — Hardcoded URLs in `SupportAppPageModel`

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-010 |
| **Original issue** | Page URLs are compile-time constants. |
| **Decision** | **Accept** |
| **Reason** | Appropriate for fixed-path internal assessment app. |
| **Actual result** | **Accepted**. |

---

### CR-011 — Unused imports in `JcrPathUtil`

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-011 |
| **Original issue** | Unused `Instant`, `ChronoUnit` imports. |
| **Decision** | **Fix** (trivial) |
| **Reason** | One-line cleanup. |
| **Files to change** | `core/.../JcrPathUtil.java` |
| **Proposed change** | Remove unused imports. |
| **Validation plan** | `mvn -B -pl core compile`. |
| **Actual result** | **[Not implemented]** — [Verify at implementation time; imports may have been removed already.] |

---

### CR-012 — Dual JCR APIs in `ensureTicketsRootExists`

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-012 |
| **Original issue** | Session API used for bootstrap; ResourceResolver elsewhere. |
| **Decision** | **Accept** |
| **Reason** | Works correctly; repoinit also creates paths. Low risk. |
| **Actual result** | **Accepted**. |

---

### CR-013 — No list pagination

| Field | Detail |
|-------|--------|
| **Finding reference** | CR-013 |
| **Original issue** | All tickets returned in one response. |
| **Decision** | **Accept** |
| **Reason** | Not required by Core spec; assessment data volume is small. |
| **Actual result** | **Accepted**. |

---

## Summary

### Implemented fixes (Part A)

| ID | Issue | Decision | Validated |
|----|-------|----------|-----------|
| RF-001 | Invalid `:cq_csrf_token` header | Fix | Manual + code inspection |
| RF-002 | Missing create redirect | Fix | Code inspection [manual UI not in test-results.md] |
| RF-003 | Empty users.json (UserManager lookup) | Fix | Unit tests + manual |
| RF-004 | Repoinit user-path ACLs | Fix | Manual ACL verification |
| RF-005 | JCR import compilation error | Fix | `mvn -pl core test` PASS |
| RF-006 | Mockito stubbing in user lookup test | Fix | `mvn -pl core test` PASS |

### Proposed review fixes (Part B)

| ID | Issue | Decision | Status |
|----|-------|----------|--------|
| CR-001 | Dispatcher `/bin` filter | Fix | Not implemented |
| CR-002 | No API auth | Accept | N/A |
| CR-003 | Live IT / E2E gap | Defer | Not implemented |
| CR-004 | Case-insensitive search (QueryBuilder) | Defer | Not implemented |
| CR-005 | Archetype cleanup | Defer | Not implemented |
| CR-006 | Endpoint helper dedup | Defer | Not implemented |
| CR-007 | CSRF doc drift | Fix | Not implemented |
| CR-008 | Real UserLookup in integration tests | Fix (recommended) | Not implemented |
| CR-009 | Dead ApiErrorMapper methods | Defer | Not implemented |
| CR-010 | Hardcoded URLs | Accept | N/A |
| CR-011 | Unused imports | Fix (trivial) | Not implemented |
| CR-012 | Dual JCR APIs | Accept | N/A |
| CR-013 | No pagination | Accept | N/A |

---

## Recommended implementation order (if proceeding)

1. **CR-007** — Update CSRF documentation (no code risk, prevents regression of RF-001).
2. **CR-001** — Dispatcher allow rules (required for Publish).
3. **CR-008** — UserLookup integration test (closes test gap exposed by RF-003).
4. **CR-011** — Remove unused imports (trivial).
5. **CR-004** — Search case-insensitivity (if manual QA finds inconsistency).
6. **CR-003** — Live IT and Cypress (post-assessment hardening).
7. **CR-005, CR-006, CR-009** — Cleanup refactors when time permits.

**No code changes have been made for Part B items.** Awaiting explicit approval before implementation.
