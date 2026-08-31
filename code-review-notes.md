# Code Review Notes — Support Ticket Management System

**Project:** AI Practical Assessment  
**Platform:** AEM as a Cloud Service (AEMaaCS)  
**Reviewer perspective:** Senior AEM engineer  
**Review date:** 2026-08-31  
**Build evidence:** `mvn -B clean install` — BUILD SUCCESS; Core module 131/131 tests passed ([test-results.md](test-results.md))  
**Source documents:** [requirements-analysis.md](requirements-analysis.md), [acceptance-criteria.md](acceptance-criteria.md), [design-notes.md](design-notes.md), [api-contract.md](api-contract.md), [data-model.md](data-model.md), [ui-flow.md](ui-flow.md), [implementation-plan.md](implementation-plan.md), [test-strategy.md](test-strategy.md)

---

## 1. Review scope

This review covers the **implemented Core** Support Ticket Management System:

- **Backend:** `core` OSGi bundle — servlet API, services, repository, validation, state machine, DTOs, error handling
- **Configuration:** `ui.config` — repoinit, service-user mapping
- **Frontend:** `ui.apps` — HTL components, `clientlib-support-app` JavaScript
- **Content:** `ui.content` — support-app pages and templates
- **Tests:** `core/src/test` automated suite; `it.tests` and `ui.tests` scaffolding
- **Build:** Maven reactor, AEM SDK API dependency, Dispatcher module presence

**Out of scope for this review:** Stretch features (auth, user CRUD, notifications), live AEM manual verification, production Cloud Manager pipeline execution.

**Review method:** Static analysis of source, configuration, tests, and design documents; cross-check against acceptance criteria and API contract. No new code changes were made as part of this review.

---

## 2. Files/modules reviewed

| Module | Key artifacts reviewed |
|--------|------------------------|
| `core` | `SupportTicketsApiServlet`, endpoint classes, `TicketRepositoryImpl`, `TicketStateMachineServiceImpl`, `TicketSearchServiceImpl`, `UserLookupServiceImpl`, `TicketValidatorImpl`, `ResourceResolverProviderImpl`, `SupportTicketsApiResourceProvider`, `ApiPathParser`, `ApiErrorMapper`, DTOs, exceptions, `SupportAppPageModel` |
| `core/src/test` | Integration tests (`SupportTicketsApiIntegrationTest`, persistence, state machine, search, validation, comments), unit tests (state machine, validator, repository, path parser, user lookup) |
| `ui.config` | `RepositoryInitializer~supporttickets.cfg.json`, `ServiceUserMapperImpl.amended~supporttickets.cfg.json` |
| `ui.apps` | `clientlib-support-app/js` (`api.js`, `csrf.js`, `create.js`, `list.js`, `detail.js`, `utils.js`), support-app HTL components |
| `ui.content` | Support-app page structure [structure verified via `SupportAppPageModel` URLs and clientlib references] |
| `dispatcher` | `default_filters.any` — `/bin/*` rule state |
| `it.tests` | Archetype `CreatePageIT`, `GetPageIT` only — no support-ticket API tests |
| `ui.tests` | Cypress scaffolding [not executed per test-results.md] |
| Docs | `api-contract.md`, `design-notes.md`, `ui-flow.md` — CSRF header documentation |

---

## 3. Architecture review

**Strengths**

- Clear layering: servlet → endpoint handlers → validator → repository/service, with domain exceptions mapped centrally in `SupportTicketsApiServlet`.
- `TicketRepositoryImpl` is the sole JCR writer, keeping persistence logic in one place.
- `TicketStateMachineServiceImpl` is a pure, stateless OSGi service with an explicit transition map — easy to test and reason about.
- `ResourceResolverProviderImpl` correctly acquires a service-user `ResourceResolver` per request and closes it via try-with-resources in the servlet.
- `SupportTicketsApiResourceProvider` solves the Sling nested-path servlet binding problem for `/bin/support-tickets/{id}/...` routes.
- Endpoint classes (`TicketListEndpoint`, `TicketDetailEndpoint`, etc.) decompose the monolithic servlet without over-abstracting.

**Observations**

- Endpoint helper methods (`isJsonRequest`, `parseJsonObject`, `requireValidTicketId`) are duplicated across four endpoint classes. Acceptable for assessment scope but increases maintenance cost.
- `TicketSearchService` depends on AEM `QueryBuilder` with a traversal fallback — pragmatic for local dev and AEM Mock, but two code paths must stay behaviourally aligned.
- `SupportAppPageModel` hardcodes page URLs and API base path rather than reading from OSGi or content policy — fine for a fixed internal app, less flexible for multi-site.
- Archetype boilerplate remains (`HelloWorldModel`, `SimpleServlet`, `LoggingFilter`, `SimpleScheduledTask`, `SimpleResourceListener`) — unrelated to support tickets, adds noise to the bundle.

**Verdict:** Architecture is sound and appropriate for the assessment scope. Separation of concerns is good; minor duplication and archetype residue are the main maintainability concerns.

---

## 4. AEM/JCR review

**Strengths**

- Ticket nodes use `nt:unstructured` with explicit `sling:resourceType` (`support-tickets/components/ticket`) — aligns with AEM component-oriented content model.
- UUID node names under `/content/support-tickets/tickets` avoid path collisions and match API contract.
- Repoinit provisions content paths, service user, seeded users, and ACLs in `ui.config`.
- Service-user mapping `support-tickets.core:support-tickets-service=[support-tickets-service]` is correctly configured.
- `resolver.commit()` used consistently after mutations; `PersistenceException` wrapped in `InternalServiceException`.
- `DateTimeUtil.nowUtcAfter()` prevents `updatedAt` from going backwards on rapid updates.
- `JcrPathUtil.isUnderSupportUserBase()` blocks `..` path traversal in user references.

**Observations**

- `ensureTicketsRootExists()` uses raw `Session` API while the rest of the repository uses `ResourceResolver` — works, but mixes JCR access styles.
- `findComments()` iterates `commentsFolder.getChildren()` — acceptable for assessment scale; would not scale to high comment volume without query/pagination.
- `UserLookupServiceImpl` correctly uses `UserManager.findAuthorizables()` with JCR-SQL2 fallback instead of `Resource.getChildren()` on authorizable folders (Oak limitation).
- Repoinit ACLs on `/home/users` and `/home/users/support` are necessary but insufficient alone for user listing — the UserManager fix was the actual resolution.
- Oak index configuration for ticket search: [Not verifiable from available project artifacts] — relies on default property indexing.

**Verdict:** JCR usage is correct and AEMaaCS-compatible. User lookup fix demonstrates good Oak/Jackrabbit awareness.

---

## 5. API review

**Strengths**

- Single servlet entry point with regex-based `ApiPathParser` — predictable routing for all documented endpoints.
- HTTP methods and status codes align with `api-contract.md`: 201 create, 200 read/update, 409 invalid transition, 404 not found, 400 validation, 415 unsupported media type, 405 method not allowed.
- Structured error envelope via `ApiErrorResponse` with `code`, `message`, `fields`, `details`.
- `PUT` rejects `status` and `createdBy` via `validateForbiddenUpdateFields` — enforces status-via-PATCH rule (AC-034).
- `Cache-Control: no-store` and `X-Content-Type-Options: nosniff` set on JSON responses.
- Content-Type validation on mutating endpoints.

**Observations**

- Invalid ticket UUID in URL paths returns **400** (`ValidationException` in endpoints); repository layer returns **404** (`TicketNotFoundException`) for invalid UUID — consistent at HTTP layer because endpoints validate first.
- `ApiErrorMapper.malformedTicketId()` and `malformedJson()` are defined but never called — dead code; endpoints throw `ValidationException` instead.
- No `401`/`403` by design (Core spec) — documented in API contract.
- List endpoint returns a bare JSON array (not wrapped in `{ "items": [...] }`) — matches contract.
- `TicketDetail` includes `allowedTransitions` on every read — good API ergonomics for UI.

**Verdict:** API design is clean, contract-aligned, and well-tested at the servlet integration level.

---

## 6. Business-rule review

**Strengths**

- State machine in `TicketStateMachineServiceImpl` exactly matches requirements:
  - OPEN → IN_PROGRESS, CANCELLED
  - IN_PROGRESS → RESOLVED, CANCELLED
  - RESOLVED → CLOSED
  - CLOSED, CANCELLED → terminal (no outbound transitions)
- `TicketRepository.updateStatus()` always calls `stateMachineService.applyTransition()` before persisting — business rule enforced at persistence boundary.
- New tickets always created with `OPEN` status regardless of request body.
- User references validated against seeded users under `/home/users/support/`.
- Field length limits enforced: title 200, description 5000, message 2000, keyword 200.

**Observations**

- Priority and status enums parsed via `fromString()` — invalid values surface as validation errors with field mapping.
- `assignedTo` can be cleared on update (null removes property) — matches AC for unassign.
- No optimistic locking / ETag on concurrent updates — acceptable for assessment; concurrent PUT/PATCH could last-write-wins.

**Verdict:** Business rules are correctly implemented and comprehensively tested.

---

## 7. Security review

**Strengths**

- Service-user pattern for all repository access — request user's JCR session is not used for ticket persistence.
- CSRF token fetched client-side and sent as `CSRF-Token` header on mutating requests (valid HTTP header name).
- UI renders user/ticket data via `textContent` in `list.js` and `detail.js` — mitigates XSS when displaying stored ticket title/description/comments.
- `LikeEscapeUtil` escapes SQL LIKE wildcards in search predicates.
- No hardcoded credentials in source [verified in core and ui.apps support-app clientlibs].
- Input validation on all write paths before persistence.

**Observations**

- **No API authentication or authorization** — intentional per Core requirements, but any authenticated AEM user (or anonymous on Publish if exposed) can call the API. Documented risk for production deployment.
- Dispatcher `default_filters.any` has `/bin/*` allow rule **commented out** (`# /005 { /type "allow" /url "/bin/*" }`) — API may be unreachable on Publish through Dispatcher without explicit filter addition.
- CSRF protection depends on AEM Granite filter — not enforced in application code (correct for AEM pattern).
- `list.js` builds `row.innerHTML` with static markup then sets dynamic values via `textContent` — safe pattern; `detailUrl` uses `encodeURIComponent(ticket.id)`.
- Seeded user emails in repoinit are example.com — acceptable for dev/assessment.

**Verdict:** Security posture is appropriate for an internal assessment app with no auth requirement. Dispatcher filter gap and open API are the main deployment considerations.

---

## 8. Test review

**Strengths**

- 131 Core tests, 0 failures ([test-results.md](test-results.md)).
- Dedicated integration test classes for persistence, state machine (valid + invalid transitions), search/filter, validation, comments, and servlet HTTP status codes.
- `SupportTicketsApiIntegrationTest` maps tests to acceptance criteria (e.g. AC-002, AC-003) with `@DisplayName` annotations.
- State machine has both unit tests (`TicketStateMachineServiceImplTest`) and integration tests.
- `ApiPathParserTest` covers all route patterns including nested paths.
- `UserLookupServiceImplTest` added for UserManager-based lookup.

**Gaps (documented, not failures)**

- **No live AEM integration tests** for support-ticket API — `it.tests` contains only archetype page tests; requires `-Plocal` and running instance.
- **No Cypress E2E execution** — `ui.tests` lint ran during build; no spec results recorded.
- **No manual UI verification** recorded in project artifacts.
- Integration tests mock `UserLookupService` rather than exercising real UserManager with repoinit users.
- `TicketSearchServiceImpl` QueryBuilder path not integration-tested with real Oak queries (QueryBuilder mocked in test base).
- Code coverage not measured.

**Verdict:** Automated backend test coverage is strong for Core business logic. Gaps are in live AEM, UI, and Dispatcher verification.

---

## 9. Code-quality review

**Strengths**

- Consistent naming aligned with domain (`TicketStatus`, `TicketRepository`, `TicketValidator`).
- SLF4J logging at appropriate levels (warn for validation/transition failures, error for unexpected).
- Small focused classes; no unnecessary frameworks beyond Gson and AEM APIs.
- `TicketConstants` centralizes paths and property names.
- Java 21 compatible; uses modern constructs (`List.of`, switch expressions in `ApiErrorMapper`).

**Observations**

- Duplicated private helpers across endpoint classes (~15 lines each × 4).
- `JcrPathUtil` imports `java.time.Instant` and `ChronoUnit` but does not use them — minor lint issue.
- Archetype components (`helloworld`, `SimpleServlet`) remain in `core` and `ui.apps`.
- Documentation drift: `api-contract.md`, `design-notes.md`, and `ui-flow.md` still reference `:cq_csrf_token` header, but `api.js` now sends only `CSRF-Token` (correct fix; docs not updated).

**Verdict:** Code quality is good. Main issues are documentation drift, archetype residue, and endpoint duplication.

---

## 10. Findings

Findings are numbered **CR-NNN** for traceability to [review-fixes.md](review-fixes.md).

### Critical

*No critical findings. The application builds, Core tests pass, and Core functional requirements are implemented.*

---

### High

#### CR-001 — Dispatcher may block `/bin/support-tickets` on Publish

| Field | Detail |
|-------|--------|
| **Severity** | High |
| **File/path** | `dispatcher/src/conf.dispatcher.d/filters/default_filters.any` |
| **Finding** | The generic `/bin/*` allow filter is commented out. Unless a project-specific allow rule exists elsewhere, browser API calls routed through Dispatcher on Publish may be denied. |
| **Why it matters** | UI on Publish relies on `/bin/support-tickets` for all CRUD operations. Blocked filters produce silent failures or 403s in production-like topology. |
| **Recommendation** | Add an explicit Dispatcher filter rule allowing `/bin/support-tickets` (and CSRF token endpoint if needed). Validate with `dispatcher/bin/validate.sh` and a Publish smoke test. |

#### CR-002 — No API authorization (accepted design risk)

| Field | Detail |
|-------|--------|
| **Severity** | High (deployment risk; accepted for Core) |
| **File/path** | `core/.../SupportTicketsApiServlet.java`, all endpoints |
| **Finding** | All API endpoints are callable without application-level auth. Service-user resolver is used for persistence, but caller identity is not checked. |
| **Why it matters** | On an exposed Publish instance, any visitor could create, modify, or transition tickets. Acceptable per assessment spec; unacceptable for production without additional controls. |
| **Recommendation** | **Accept** for Core scope. Document as known limitation. For production, add Stretch auth (FR-S03) or AEM permission checks. |

#### CR-003 — Live AEM / E2E tests not executed

| Field | Detail |
|-------|--------|
| **Severity** | High (test gap, not code defect) |
| **File/path** | `it.tests/`, `ui.tests/`, [test-results.md](test-results.md) |
| **Finding** | No support-ticket-specific integration tests run against a live AEM instance. Cypress specs not executed. |
| **Why it matters** | AEM Mock cannot catch repoinit, indexing, CSRF filter, or Dispatcher issues discovered during manual testing (e.g. empty users list, CSRF header failure). |
| **Recommendation** | Add `it.tests` for key API flows; run Cypress smoke on Author; record results. |

---

### Medium

#### CR-004 — QueryBuilder keyword search may not be case-insensitive

| Field | Detail |
|-------|--------|
| **Severity** | Medium |
| **File/path** | `core/.../TicketSearchServiceImpl.java` (lines 107–115) |
| **Finding** | Keyword is lowercased before the LIKE predicate, but stored `title`/`description` property values are not normalized. Oak/JCR LIKE comparison is typically case-sensitive. Traversal fallback does case-insensitive matching, so behaviour differs between code paths. |
| **Why it matters** | AC-070 requires case-insensitive keyword search. QueryBuilder path may return fewer results than traversal fallback for mixed-case titles. |
| **Recommendation** | Use `fn:lower-case()` in JCR-SQL2 if switching to query API, add Oak index with normalized fields, or always use traversal fallback for keyword search at assessment scale. Add integration test with mixed-case title. |

#### CR-005 — Archetype boilerplate remains in bundle

| Field | Detail |
|-------|--------|
| **Severity** | Medium |
| **File/path** | `core/.../HelloWorldModel.java`, `SimpleServlet.java`, `LoggingFilter.java`, `SimpleScheduledTask.java`, `SimpleResourceListener.java`; `ui.apps/.../components/helloworld/` |
| **Finding** | AEM archetype sample code ships alongside support-ticket implementation. |
| **Why it matters** | Increases bundle size, confuses reviewers, and registers unused OSGi components (filter, scheduler, listener). |
| **Recommendation** | Remove unused archetype artifacts in a cleanup pass. |

#### CR-006 — Duplicated endpoint parsing/validation helpers

| Field | Detail |
|-------|--------|
| **Severity** | Medium |
| **File/path** | `TicketListEndpoint`, `TicketDetailEndpoint`, `TicketStatusEndpoint`, `TicketCommentEndpoint` |
| **Finding** | `isJsonRequest`, `parseJsonObject`, and `requireValidTicketId` are copy-pasted across four classes. |
| **Why it matters** | Bug fixes (e.g. content-type handling) must be applied in four places. |
| **Recommendation** | Extract a small `JsonRequestSupport` utility or base class. Optional for assessment; recommended before team scaling. |

#### CR-007 — Documentation drift on CSRF header

| Field | Detail |
|-------|--------|
| **Severity** | Medium |
| **File/path** | `api-contract.md`, `design-notes.md`, `ui-flow.md` vs `ui.apps/.../api.js` |
| **Finding** | Docs specify `:cq_csrf_token` header; implementation correctly uses `CSRF-Token` only. Invalid `:cq_csrf_token` header previously broke `fetch()` entirely. |
| **Why it matters** | Future developers following docs would reintroduce the bug. |
| **Recommendation** | Update design docs to match implementation. **Note:** `api.js` fix already applied; docs still stale. |

#### CR-008 — `UserLookupService` not integration-tested end-to-end

| Field | Detail |
|-------|--------|
| **Severity** | Medium |
| **File/path** | `core/src/test/.../SupportTicketsIntegrationTestBase.java` |
| **Finding** | Integration tests mock `UserLookupService` instead of using `UserLookupServiceImpl` with AEM Mock user nodes. |
| **Why it matters** | The production bug (empty users array) was in `UserLookupServiceImpl` and would not have been caught by integration tests. Unit test was added after fix. |
| **Recommendation** | Add integration test wiring real `UserLookupServiceImpl` with repoinit-like user structure in AEM Mock. |

#### CR-009 — Unused `ApiErrorMapper` helpers

| Field | Detail |
|-------|--------|
| **Severity** | Medium (Low if treated as dead code only) |
| **File/path** | `core/.../ApiErrorMapper.java` — `malformedTicketId()`, `malformedJson()` |
| **Finding** | Methods exist but are never referenced; endpoints throw `ValidationException` instead. |
| **Why it matters** | Dead code misleads maintainers about actual error paths. |
| **Recommendation** | Remove unused methods or refactor endpoints to use them consistently. |

---

### Low

#### CR-010 — Hardcoded URLs in `SupportAppPageModel`

| Field | Detail |
|-------|--------|
| **Severity** | Low |
| **File/path** | `core/.../SupportAppPageModel.java` |
| **Finding** | Page URLs and API base are compile-time constants. |
| **Why it matters** | Content path changes require code redeploy. |
| **Recommendation** | Accept for assessment. Optionally externalize via OSGi config or page properties. |

#### CR-011 — Unused imports in `JcrPathUtil`

| Field | Detail |
|-------|--------|
| **Severity** | Low |
| **File/path** | `core/.../JcrPathUtil.java` |
| **Finding** | `Instant` and `ChronoUnit` imported but unused. |
| **Why it matters** | Minor compile-warning noise. |
| **Recommendation** | Remove unused imports. |

#### CR-012 — `ensureTicketsRootExists` uses dual JCR APIs

| Field | Detail |
|-------|--------|
| **Severity** | Low |
| **File/path** | `core/.../TicketRepositoryImpl.java` |
| **Finding** | Folder bootstrap uses `Session` while CRUD uses `ResourceResolver`. |
| **Why it matters** | Slightly harder to test and reason about session lifecycle. |
| **Recommendation** | Accept or refactor to `ResourceResolver`/`ResourceUtil` only. |

#### CR-013 — No pagination on ticket list

| Field | Detail |
|-------|--------|
| **Severity** | Low |
| **File/path** | `TicketSearchServiceImpl`, `TicketListEndpoint` |
| **Finding** | List endpoint returns all matching tickets. |
| **Why it matters** | Performance degrades with large datasets. Not required by Core spec. |
| **Recommendation** | Defer; document as known scale limitation. |

#### CR-014 — `it.tests` lacks support-ticket coverage

| Field | Detail |
|-------|--------|
| **Severity** | Low |
| **File/path** | `it.tests/src/main/java/com/supporttickets/it/tests/` |
| **Finding** | Only archetype `CreatePageIT` and `GetPageIT` exist. |
| **Why it matters** | Missed opportunity for contract verification on real AEM. |
| **Recommendation** | Add API smoke ITs when local AEM is available. |

---

### Positive findings (no action required)

| ID | Area | Observation |
|----|------|-------------|
| CR-P01 | State machine | Correct transition map; 409 on invalid transitions; `allowedTransitions` in API responses |
| CR-P02 | Persistence | Single repository writer; UUID paths; `updatedAt` monotonic guard |
| CR-P03 | API routing | `SupportTicketsApiResourceProvider` + `ApiPathParser` solve nested `/bin` paths cleanly |
| CR-P04 | Validation | Comprehensive field, enum, and user-existence checks with structured 400 responses |
| CR-P05 | Frontend fix | `CSRF-Token` header and post-create redirect implemented in `api.js` / `create.js` |
| CR-P06 | User lookup | `UserManager` + JCR-SQL2 fallback fixes empty users list on Oak |
| CR-P07 | Test suite | 131 automated Core tests with AC traceability in servlet integration tests |

---

## Summary

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High | 3 |
| Medium | 6 |
| Low | 5 |
| Positive | 7 |

**Overall assessment:** The Core implementation is **well-structured, contract-aligned, and adequately tested** at the AEM Mock / unit integration level. No blocking code defects were found in static review given passing build and tests. The highest-priority follow-ups are **Dispatcher filter configuration for Publish**, **documentation sync for CSRF**, and **closing the live-AEM / UI test gap**. Several fixes for issues found during development (CSRF header, user lookup, create redirect) are already implemented — see [review-fixes.md](review-fixes.md).
