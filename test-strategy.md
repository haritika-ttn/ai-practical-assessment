# Test Strategy — Support Ticket Management System

**Project:** AI Practical Assessment  
**Platform:** AEM as a Cloud Service (AEMaaCS)  
**Document type:** Test strategy (not a test-results report)  
**Source documents:** [requirements-analysis.md](requirements-analysis.md), [acceptance-criteria.md](acceptance-criteria.md), [design-notes.md](design-notes.md), [api-contract.md](api-contract.md), [data-model.md](data-model.md), [ui-flow.md](ui-flow.md), [implementation-plan.md](implementation-plan.md)  
**Document version:** 1.0  
**Status:** Reflects implemented codebase and automated test suite as of project completion

---

## 1. Test objectives

The quality goals for this application are:

| Goal | Rationale |
|------|-----------|
| **Correct ticket lifecycle** | Tickets must be created in `OPEN`, updated without bypassing the state machine, and transitioned only along allowed paths. |
| **Durable JCR persistence** | Tickets and comments must survive repository reads/writes through `TicketRepository` under `/content/support-tickets/tickets`. |
| **Backend validation authority** | All create, update, status, and comment operations must be rejected at the Core layer when input is invalid — the UI is not the source of truth. |
| **Predictable API behaviour** | `/bin/support-tickets` endpoints must return documented HTTP status codes and structured JSON error bodies (`VALIDATION_ERROR`, `NOT_FOUND`, `INVALID_TRANSITION`, etc.). |
| **Search and filter correctness** | List operations must support keyword search (title/description, case-insensitive) and status filtering, including combined AND logic. |
| **Regression safety for the state machine** | Valid and invalid transitions must be covered by automated tests because incorrect transitions corrupt workflow integrity. |
| **Safe failure modes** | Missing tickets, invalid transitions, and validation failures must not mutate persisted state. |

This strategy describes **what** is tested, **why**, and **at which tier**. It does not claim execution results or production readiness.

---

## 2. Test scope

Scope is limited to **implemented Core functionality** in this repository.

### In scope (implemented)

| Area | Implementation | Primary test focus |
|------|----------------|-------------------|
| **Ticket creation** | `TicketListEndpoint.doPost`, `TicketRepository.create` | Valid create forces `OPEN`; required fields; unknown user references |
| **Ticket listing** | `TicketListEndpoint.doGet`, `TicketSearchService` | Unfiltered list, keyword search, status filter, combined filters |
| **Ticket detail** | `TicketDetailEndpoint.doGet`, `TicketRepository.findById` | Field retrieval, `allowedTransitions`, comments on detail |
| **Ticket updates** | `TicketDetailEndpoint.doPut`, `TicketRepository.update` | Title, description, priority, assignee, unassign |
| **Ticket reassignment** | `assignedTo` on create/update | Assign and clear assignee |
| **Comment creation** | `TicketCommentEndpoint.doPost`, `TicketRepository.addComment` | Persist comment, sort order, parent `updatedAt` |
| **Search** | `TicketSearchServiceImpl` | Title, description, case-insensitivity, no-match empty list |
| **Status filtering** | `TicketSearchServiceImpl` | Per-status filter and combined with keyword |
| **Status transitions** | `TicketStatusEndpoint.doPatch`, `TicketStateMachineService`, `TicketRepository.updateStatus` | Five valid transitions; rejection of invalid transitions |
| **Backend validation** | `TicketValidatorImpl` | Required fields, enum parsing, forbidden PUT fields, user existence |
| **Error handling** | `SupportTicketsApiServlet`, `ApiErrorMapper` | 400 / 404 / 409 / 415 responses with JSON error bodies |
| **Persistence** | JCR nodes under `/content/support-tickets/tickets/{uuid}` | Create, read, update, status change, comments, no orphan writes on validation failure |
| **API routing** | `ApiPathParser`, `SupportTicketsApiResourceProvider` | Path parsing for list, users, detail, status, comments |
| **Page configuration model** | `SupportAppPageModel` | `apiBase`, `csrfTokenUrl`, page URLs exposed to HTL |
| **User listing (API contract)** | `UserListEndpoint`, `UserLookupServiceImpl` | HTTP 200 array response; UserManager-based lookup logic (unit level) |

### UI in scope (manual only)

The following exist in `ui.apps` clientlibs (`clientlib-support-app`) but **have no automated UI or E2E tests** in this project:

- Ticket list screen (`list.js`)
- Create ticket screen (`create.js`)
- Ticket detail screen (`detail.js`)
- Client-side validation, alerts, CSRF token fetch (`api.js`, `csrf.js`, `utils.js`)

### Out of scope for automated tests in this repository

- Authentication and authorization (API is intentionally open per spec)
- User management UI (AC-121)
- Publish/Dispatcher deployment verification
- AEM restart durability (AC-090)
- Oak Lucene index behaviour at production scale
- Replication, CDN, Universal Editor

---

## 3. Test levels / tiers

The following tiers **actually exist** in the project.

### 3.1 Unit tests (JUnit 5 + Mockito)

**Location:** `core/src/test/java`  
**Runner:** Maven Surefire (`mvn -pl core test`)

| Test class | Purpose |
|------------|---------|
| `TicketStateMachineServiceImplTest` | Exhaustive transition matrix for `TicketStateMachineServiceImpl` (valid, invalid, allowed-transition sets) |
| `TicketValidatorImplTest` | Isolated validator rules with mocked `UserLookupService` |
| `UserLookupServiceImplTest` | UserManager search behaviour with mocked `Authorizable` nodes |
| `ApiPathParserTest` | URL/route parsing for all API endpoints |

**Why:** Fast feedback on pure business logic without JCR or servlet container overhead.

### 3.2 AEM context / repository tests (wcm.io AEM Mock)

**Location:** `core/src/test/java/.../repository/impl/TicketRepositoryTest.java`  
**Framework:** `AemContext` via `AppAemContext` (`ResourceResolverType.RESOURCERESOLVER_MOCK`)

| Focus |
|-------|
| JCR node creation under `TicketConstants.TICKETS_PATH` |
| Status delegation to state machine |
| Comment persistence and ordering |
| Invalid transition leaves ticket unchanged |
| Path traversal guard on `findById` |

**Why:** Verifies the sole write path (`TicketRepositoryImpl`) against an in-memory JCR mock without a running AEM instance.

### 3.3 Integration tests (AEM Mock + wired OSGi services)

**Location:** `core/src/test/java/.../integration/*`  
**Base class:** `SupportTicketsIntegrationTestBase`

These tests register real implementations (`TicketRepositoryImpl`, `TicketValidatorImpl`, `TicketSearchServiceImpl`, `TicketStateMachineServiceImpl`) into AEM Mock and exercise them through a shared `ResourceResolver`.

| Test class | Focus |
|------------|-------|
| `TicketPersistenceIntegrationTest` | Create, read, update, reassign, immutability guards |
| `TicketStateMachineIntegrationTest` | Valid transitions persist; invalid transitions throw and do not change `updatedAt` |
| `TicketValidationIntegrationTest` | Validation rules end-to-end through `TicketValidator` |
| `TicketSearchIntegrationTest` | Search/filter service behaviour |
| `TicketCommentIntegrationTest` | Comment CRUD effects on ticket detail |
| `SupportTicketsApiIntegrationTest` | Servlet-level HTTP status codes and JSON bodies |

**Important test harness notes:**

- `UserLookupService` is **mocked** in the integration base (not the live repoinit users).
- `QueryBuilder` is **mocked**; search tests exercise the **repository traversal fallback** in `TicketSearchServiceImpl`, not live Oak QueryBuilder predicates.

### 3.4 API-level servlet tests (in-process)

**Location:** `SupportTicketsApiIntegrationTest`  
**Approach:** Instantiates `SupportTicketsApiServlet` and endpoint classes with manual dependency injection; uses `MockSlingHttpServletResponse` and mocked `SlingHttpServletRequest`.

**Covers:** HTTP method routing, status codes, JSON error `code` fields, and response serialization — **not** full Sling filter chain, CSRF filter, or Dispatcher.

### 3.5 Sling Model tests

**Location:** `SupportAppPageModelTest`  
**Covers:** Static API/page URL configuration exposed to HTL components.

### 3.6 AEM Integration Tests module (`it.tests`)

**Location:** `it.tests/src/main/java/com/supporttickets/it/tests/`  
**Framework:** Adobe AEM Testing Clients (`CQAuthorClassRule`, `CQRule`, `Page`)

| Class | Purpose |
|-------|---------|
| `CreatePageIT` | Archetype sample — verifies a test page exists on Author |
| `GetPageIT` | Archetype sample — page retrieval |

**Not support-ticket-specific.** Executed against a **running** AEM Author instance (Cloud Manager _Custom Functional Testing_ step), not during `mvn -pl core test`.

### 3.7 UI / E2E tests (`ui.tests`)

**Location:** `ui.tests/test-module/cypress/e2e/`  
**Framework:** Cypress

Existing specs are archetype samples only:

- `login.cy.js`
- `basic.cy.js`
- `assets.cy.js`
- `console_error.cy.js`

**No Cypress tests** cover support-ticket list, create, detail, or API flows.

### 3.8 Tiers not present

| Tier | Status |
|------|--------|
| Frontend unit tests (Jest/Mocha for clientlibs) | Not implemented |
| Support-ticket E2E (Cypress/Playwright) | Not implemented |
| Live AEM API integration tests for `/bin/support-tickets` | Not implemented in `it.tests` |
| Performance / load tests | Not implemented |
| Security penetration tests | Not implemented |

### Automated test inventory (Core module)

Running `mvn -pl core test` executes **131** tests, including:

- **~126** support-ticket domain tests (integration, repository, state machine, validator, API, user lookup, path parser, page model)
- **5** AEM archetype boilerplate tests (`HelloWorldModelTest`, `SimpleServletTest`, `LoggingFilterTest`, `SimpleResourceListenerTest`, `SimpleScheduledTaskTest`)

Parameterized tests (e.g. all invalid transition pairs, all `TicketStatus` filter values) increase method count beyond one-per-scenario.

---

## 4. State-machine testing

### 4.1 Why integration testing matters

The ticket state machine is a **domain invariant**. Allowing skip-level or backward transitions would:

- Expose invalid `allowedTransitions` in the API and UI
- Permit updates to terminal tickets
- Break audit expectations for workflow progression

Integration tests verify that `TicketRepository.updateStatus` **persists only allowed transitions** and that invalid attempts leave JCR `status` and `updatedAt` unchanged.

### 4.2 Valid transitions (must succeed)

| From | To | Automated coverage |
|------|-----|-------------------|
| `OPEN` | `IN_PROGRESS` | `TicketStateMachineServiceImplTest`, `TicketStateMachineIntegrationTest.ac040_*`, `SupportTicketsApiIntegrationTest.ac040_*` |
| `IN_PROGRESS` | `RESOLVED` | `TicketStateMachineServiceImplTest`, `TicketStateMachineIntegrationTest.ac041_*` |
| `RESOLVED` | `CLOSED` | `TicketStateMachineServiceImplTest`, `TicketStateMachineIntegrationTest.ac042_*` |
| `OPEN` | `CANCELLED` | `TicketStateMachineServiceImplTest`, `TicketStateMachineIntegrationTest.ac043_*` |
| `IN_PROGRESS` | `CANCELLED` | `TicketStateMachineServiceImplTest`, `TicketStateMachineIntegrationTest.ac044_*` |

### 4.3 Invalid transitions (must be rejected)

| Scenario | Example | Expected behaviour | Automated coverage |
|----------|---------|-------------------|-------------------|
| Skip-level from OPEN | `OPEN → RESOLVED`, `OPEN → CLOSED` | `InvalidTransitionException` / HTTP 409 | Parameterized integration (`AC-050`, `AC-051`); exhaustive unit matrix |
| Backward | `IN_PROGRESS → OPEN`, `RESOLVED → IN_PROGRESS` | Rejected | Parameterized integration (`AC-052`, `AC-053`) |
| Cancel from RESOLVED | `RESOLVED → CANCELLED` | Rejected | `AC-054` in parameterized integration |
| Any from terminal | `CLOSED → *`, `CANCELLED → *` | Rejected | `AC-055`, `AC-056` in parameterized integration |
| No-op / same status | `OPEN → OPEN` | Rejected | Parameterized integration (`AC-noop`) |
| Skip IN_PROGRESS | `IN_PROGRESS → CLOSED` | Rejected | Parameterized integration (`AC-skip`) |
| Unknown enum | `PATCH` with `INVALID_STATUS` | HTTP 400 before state machine | `SupportTicketsApiIntegrationTest.ac057_*`, `TicketValidationIntegrationTest` |

### 4.4 Additional state-machine assertions

| Assertion | Test |
|-----------|------|
| `allowedTransitions` populated correctly after transition | `TicketStateMachineIntegrationTest` (e.g. OPEN → IN_PROGRESS exposes RESOLVED, CANCELLED) |
| Valid transition updates `updatedAt` | `TicketStateMachineIntegrationTest.validTransitionUpdatesTimestamp` |
| Invalid transition does not change `updatedAt` | Parameterized `invalidTransitionsAreRejected` |
| PUT cannot change `status` | `SupportTicketsApiIntegrationTest.ac034_*`, `TicketValidationIntegrationTest.ac034_*` |

### 4.5 Positive and negative examples

**Positive:** Ticket in `OPEN` → `PATCH /status.json` with `IN_PROGRESS` → persisted status `IN_PROGRESS`, HTTP 200 at servlet layer.

**Negative:** Ticket in `OPEN` → `PATCH` with `CLOSED` → HTTP 409 `INVALID_TRANSITION` with `currentStatus` / `requestedStatus` in `details`; JCR node remains `OPEN`.

---

## 5. Validation testing

Validation is enforced by `TicketValidatorImpl` before repository writes.

### 5.1 Create ticket

| Scenario | Field(s) | Expected | Automated test |
|----------|----------|----------|----------------|
| Missing/blank `title` | `title` | `ValidationException` / HTTP 400 | `TicketValidationIntegrationTest.ac003_*`, `TicketValidatorImplTest`, `SupportTicketsApiIntegrationTest.ac003_*` |
| Missing `priority` | `priority` | Rejected | `TicketValidationIntegrationTest.ac004_*` |
| Unknown `createdBy` | `createdBy` | Rejected | `TicketValidationIntegrationTest.ac005_*`, `TicketValidatorImplTest` |
| Unknown `assignedTo` | `assignedTo` | Rejected | `TicketValidationIntegrationTest.ac005_rejectUnknownAssignedTo` |
| Invalid create does not persist | — | Ticket count unchanged | `TicketPersistenceIntegrationTest.ac003_invalidCreateDoesNotPersist` |

**Note:** Invalid priority **enum string** on create is handled in `TicketListEndpoint.parsePriority` ( servlet ), not covered by a dedicated automated test — only **missing** priority is tested at validator level.

### 5.2 Update ticket (PUT)

| Scenario | Expected | Automated test |
|----------|----------|----------------|
| Blank `title` on update | Rejected | `TicketValidationIntegrationTest.rejectBlankTitleOnUpdate` |
| `status` in PUT body | Rejected; message references PATCH | `TicketValidationIntegrationTest.ac034_*`, `SupportTicketsApiIntegrationTest.ac034_*` |
| `createdBy` in PUT body | Rejected (immutable) | `TicketValidationIntegrationTest.ac007_*` |
| Unassign (`assignedTo: ""`) | Allowed | `TicketValidatorImplTest.validateUpdateAllowsUnassign`, `TicketPersistenceIntegrationTest.ac033_unassignTicket` |

### 5.3 Status change (PATCH)

| Scenario | Expected | Automated test |
|----------|----------|----------------|
| Unknown status value | `ValidationException` / HTTP 400 | `TicketValidationIntegrationTest.ac057_*`, `SupportTicketsApiIntegrationTest.ac057_*` |
| Missing/blank status | Rejected | `TicketValidationIntegrationTest.ac057_rejectMissingStatusValue` |
| Valid enum parsing (`in_progress`) | Accepted | `TicketValidationIntegrationTest.ac057_acceptValidStatusEnum` |

### 5.4 Comments

| Scenario | Expected | Automated test |
|----------|----------|----------------|
| Blank `message` | Rejected | `TicketCommentIntegrationTest.blankCommentMessageRejected`, `TicketValidatorImplTest` |
| Unknown `createdBy` | Rejected | `TicketValidationIntegrationTest.rejectCommentWithUnknownAuthor` |

### 5.5 List query parameters

| Scenario | Expected | Automated test |
|----------|----------|----------------|
| Invalid `status` query value | `ValidationException` | `TicketSearchIntegrationTest.invalidStatusFilterRejected` |

---

## 6. Persistence testing

### 6.1 JCR model under test

| Path | Node type / resource type | Writer |
|------|---------------------------|--------|
| `/content/support-tickets/tickets` | `sling:Folder` | `TicketRepositoryImpl.ensureTicketsRootExists` |
| `/content/support-tickets/tickets/{uuid}` | `nt:unstructured`, `support-tickets/components/ticket` | `TicketRepository.create` / `update` |
| `/content/support-tickets/tickets/{uuid}/comments/{uuid}` | comment nodes | `TicketRepository.addComment` |

User references store **paths** (e.g. `/home/users/support/agent1`), not embedded user nodes.

### 6.2 How persistence is verified

| Verification | Mechanism | Test examples |
|--------------|-----------|---------------|
| Ticket created with UUID node name | `create` → `findById` | `TicketPersistenceIntegrationTest.ac002_*`, `TicketRepositoryTest.createTicketPersistsOpenStatus` |
| Properties persisted (`title`, `description`, `priority`, `status`, `createdBy`, `assignedTo`, timestamps) | Reload from JCR | `TicketPersistenceIntegrationTest` |
| Status forced to `OPEN` on create | Repository enforces | `TicketPersistenceIntegrationTest.ac006_*` |
| Update mutates fields without status change | `update` + reload | `ac030_*`, `ac031_*`, `ac032_*`, `ac033_*` |
| Status transition writes new `status` and `updatedAt` | `updateStatus` + reload | `TicketStateMachineIntegrationTest` |
| Invalid transition leaves JCR unchanged | Compare before/after | `TicketRepositoryTest.updateStatusInvalidTransitionDoesNotModifyTicket`, parameterized integration |
| Validation failure does not create nodes | `countTicketNodes()` | `TicketPersistenceIntegrationTest.ac003_invalidCreateDoesNotPersist` |
| Comments stored as children, returned on detail | `findById` comment list | `TicketCommentIntegrationTest`, `TicketRepositoryTest.addCommentUpdatesTicketTimestampAndSortsComments` |
| Missing ticket | `TicketNotFoundException` / HTTP 404 | `TicketPersistenceIntegrationTest.ac022_*`, `SupportTicketsApiIntegrationTest.ac022_*` |
| Malformed ticket ID rejected | Path guard | `TicketRepositoryTest.findByIdThrowsForInvalidId` |

### 6.3 What persistence tests do **not** verify

- Real `support-tickets-service` service-user ACLs on Author
- Repoinit script execution
- Oak index / QueryBuilder predicate correctness (QueryBuilder is mocked)
- Replication to Publish
- Survival across AEM process restart (AC-090)

---

## 7. Search/filter testing

**Implementation:** `TicketSearchServiceImpl` — QueryBuilder primary path with **repository traversal fallback** when QueryBuilder is unavailable or returns no hits.

**Test environment note:** Integration tests mock `QueryBuilder`, so automated search tests exercise the **traversal fallback** path against AEM Mock JCR nodes.

### 7.1 Keyword search

| Scenario | Expected | Automated test |
|----------|----------|----------------|
| Match title | Ticket returned | `TicketSearchIntegrationTest.ac070_*` |
| Match description | Ticket returned | `ac071_*` |
| Case-insensitive | Lowercase query matches mixed-case title | `ac072_*` |
| No matches | Empty list (not error) | `ac073_*`, `SupportTicketsApiIntegrationTest.ac073_*` (API GET) |

### 7.2 Status filter

| Scenario | Expected | Automated test |
|----------|----------|----------------|
| Filter `OPEN` excludes other statuses | Only matching tickets | `ac080_*` |
| Each `TicketStatus` enum value | All results match filter | `ac081_*` (parameterized) |

### 7.3 Combined search + filter

| Scenario | Expected | Automated test |
|----------|----------|----------------|
| Keyword AND status | Only tickets matching both | `ac082_*` |

### 7.4 List without filters

| Scenario | Expected | Automated test |
|----------|----------|----------------|
| All persisted tickets returned | Both IDs present | `ac010_listAllTickets` |

---

## 8. Error-handling testing

### 8.1 Invalid input

| Condition | HTTP (servlet tests) | Error code | Tests |
|-----------|---------------------|------------|-------|
| Create validation failure | 400 | `VALIDATION_ERROR` + `fields` | `SupportTicketsApiIntegrationTest.ac003_*` |
| PUT with `status` field | 400 | `VALIDATION_ERROR` | `ac034_*` |
| PATCH unknown status | 400 | `VALIDATION_ERROR` | `ac057_*` |
| Invalid list `status` query | Exception at service layer | `ValidationException` | `TicketSearchIntegrationTest.invalidStatusFilterRejected` |

### 8.2 Missing ticket

| Condition | HTTP | Error code | Tests |
|-----------|------|------------|-------|
| GET unknown UUID | 404 | `NOT_FOUND` | `SupportTicketsApiIntegrationTest.ac022_getMissingTicketReturns404` |
| Repository `findById` | Exception | `TicketNotFoundException` | `TicketPersistenceIntegrationTest.ac022_*` |
| Comment on missing ticket | Exception | `TicketNotFoundException` | `TicketCommentIntegrationTest.ac062_commentOnMissingTicketFails` |

### 8.3 Invalid status transition

| Condition | HTTP | Error code | Tests |
|-----------|------|------------|-------|
| OPEN → CLOSED via PATCH | 409 | `INVALID_TRANSITION` with `details` | `SupportTicketsApiIntegrationTest.ac050_*` |
| Repository-level invalid transition | Exception | `InvalidTransitionException` | State machine integration + repository tests |

### 8.4 Missing / invalid user

| Condition | Layer | Tests |
|-----------|-------|-------|
| Unknown `createdBy` on create | Validator | `TicketValidationIntegrationTest.ac005_*` |
| Unknown `assignedTo` on create | Validator | `ac005_rejectUnknownAssignedTo` |
| Unknown comment author | Validator | `rejectCommentWithUnknownAuthor` |

**Gap:** Servlet-level POST with unknown user (HTTP 400 through full stack) is not dedicated-tested; validator integration covers the rule.

### 8.5 Persistence failures

`InternalServiceException` maps to HTTP 500 via `SupportTicketsApiServlet`, but **no automated test** simulates JCR `PersistenceException` or service-resolver login failure.

### 8.6 Error response shape

| Requirement | Coverage |
|-------------|----------|
| Structured JSON errors | Asserted in `SupportTicketsApiIntegrationTest` (`code`, `fields`, `details`) |
| No stack traces in API responses | **Not** automatically asserted (AC-101 — manual / code review) |
| Gson serialization of `TicketDetail` | `SupportTicketsApiIntegrationTest.ticketDetailSerializesForApiResponses` |

### 8.7 UI error handling

Client-side handling in `utils.js` (`handleApiError`) is **not** covered by automated tests (AC-110, AC-111 — manual).

---

## 9. Test data

### 9.1 Automated test data (AEM Mock)

Created per test in `SupportTicketsIntegrationTestBase`:

| Artifact | Value |
|----------|-------|
| Tickets root | `/content/support-tickets/tickets` (`sling:Folder`) |
| Mock user folder | `/home/users/support` |
| Agent 1 | `/home/users/support/agent1` (`rep:User` resource) |
| Agent 2 | `/home/users/support/agent2` (`rep:User` resource) |
| Unknown user | `/home/users/support/nonexistent` (mocked as non-existent) |

`UserLookupService` mock returns `userExists=true` for AGENT1/AGENT2 and `false` for UNKNOWN_USER.

Tickets are created via `createValidatedTicket(...)` with realistic titles, priorities, and descriptions. Search tests use distinctive keywords (`ZEBRA-UNIQUE`, `PANDA-UNIQUE`, `XYZNOMATCH999`).

### 9.2 Runtime seed data (repoinit — manual / setup verification)

Configured in `ui.config/.../RepositoryInitializer~supporttickets.cfg.json`:

| User | Path | Role |
|------|------|------|
| agent1 | `/home/users/support/agent1` | AGENT |
| agent2 | `/home/users/support/agent2` | AGENT |
| supervisor1 | `/home/users/support/supervisor1` | SUPERVISOR |

Service user: `support-tickets-service` with ACLs on `/content/support-tickets` and `/home/users/support`.

**Not exercised by automated Core tests** against a live repository.

### 9.3 UI content

Sample pages under `/content/support-app` (from `ui.content`) — verified manually, not by Cypress in this project.

---

## 10. Test environment

| Component | Value |
|-----------|-------|
| **Platform** | AEM as a Cloud Service SDK |
| **AEM SDK API version** | `2026.8.27673.20260811T193135Z-260700` (parent `pom.xml`) |
| **Java** | 21 (Maven compiler `release=21`; Cloud Manager `.cloudmanager/java-version` = `21`) |
| **Build tool** | Maven (`mvn clean install`) |
| **Core unit/integration tests** | `mvn -pl core test` (no running AEM required) |
| **Local Author (manual / IT)** | `localhost:4502` (default in `pom.xml`) |
| **Local Publish (manual)** | `localhost:4503` |
| **Deploy to local Author** | `mvn clean install -PautoInstallSinglePackage` |
| **AEM Mock** | wcm.io `AemContext` with `ResourceResolverType.RESOURCERESOLVER_MOCK` |
| **Unit test frameworks** | JUnit 5, Mockito, AEM Mock, Sling Mock |
| **Cloud Manager IT** | `it.tests` module — requires running Author + AEM Testing Clients |
| **UI tests** | Cypress in `ui.tests` — requires running AEM; no support-ticket specs |

---

## 11. Acceptance-criteria coverage

**Legend — Coverage status**

| Status | Meaning |
|--------|---------|
| **Automated** | At least one automated test in `core` targets this criterion |
| **Partial** | Behaviour covered at a lower layer (e.g. repository) but not full HTTP/UI path |
| **Manual** | Specified as manual/setup; no dedicated automated test |
| **Not covered** | No automated test; manual verification not documented in repo |

This table maps **acceptance criteria to test evidence in the repository**. It does **not** state that tests were executed or passed in a specific environment.

| AC | Requirement summary | Test scenario (representative) | Test level | Auto / Manual | Coverage status |
|----|---------------------|-------------------------------|------------|---------------|-----------------|
| AC-001 | Create ticket via UI | Fill create form, submit | — | Manual | Manual |
| AC-002 | Create ticket via API | POST valid JSON → 201 OPEN | Servlet integration + persistence integration | Automated | Automated |
| AC-003 | Reject missing title | Blank/missing title → 400 / ValidationException | Validator + servlet + persistence | Automated | Automated |
| AC-004 | Reject invalid priority | Missing priority on create | Validator integration | Automated | Partial (missing priority; invalid enum string not dedicated-tested) |
| AC-005 | Reject unknown createdBy | Unknown user path on create | Validator integration | Automated | Automated |
| AC-006 | Force OPEN on create | Created ticket always OPEN | Persistence integration | Automated | Automated |
| AC-007 | createdBy immutable | PUT with createdBy rejected | Validator integration | Automated | Partial (validator; not servlet PUT test) |
| AC-010 | List all tickets | search(null, null) returns all | Search integration | Automated | Partial (service layer; not dedicated GET list servlet test) |
| AC-011 | New ticket in list | Create then findById | Persistence integration | Automated | Partial (by ID, not list endpoint) |
| AC-020 | View ticket detail | findById returns fields + transitions | Persistence integration | Automated | Partial (repository; not GET servlet) |
| AC-021 | Detail includes comments | Two comments on detail | Comment integration | Automated | Automated |
| AC-022 | allowedTransitions on detail | OPEN ticket exposes IN_PROGRESS, CANCELLED | Persistence integration | Automated | Partial (repository; not GET servlet) |
| AC-023 | 404 unknown ticket | GET unknown UUID | Servlet integration + persistence | Automated | Automated |
| AC-030 | Update title | Repository update title | Persistence integration | Automated | Partial (repository; not PUT servlet) |
| AC-031 | Update description | Repository update description | Persistence integration | Automated | Partial |
| AC-032 | Update priority | Repository update priority | Persistence integration | Automated | Partial |
| AC-033 | Reassign ticket | Update assignee / unassign | Persistence integration | Automated | Partial |
| AC-034 | Reject status on PUT | PUT with status → 400 | Servlet + validator integration | Automated | Automated |
| AC-040 | OPEN → IN_PROGRESS | Valid transition persists | State machine unit + integration + servlet | Automated | Automated |
| AC-041 | IN_PROGRESS → RESOLVED | Valid transition persists | State machine integration | Automated | Automated |
| AC-042 | RESOLVED → CLOSED | Terminal state | State machine integration | Automated | Automated |
| AC-043 | OPEN → CANCELLED | Valid transition | State machine integration | Automated | Automated |
| AC-044 | IN_PROGRESS → CANCELLED | Valid transition | State machine integration | Automated | Automated |
| AC-050 | Reject OPEN → RESOLVED | Invalid transition | Parameterized integration + servlet 409 | Automated | Automated |
| AC-051 | Reject OPEN → CLOSED | Invalid transition | Parameterized integration | Automated | Automated |
| AC-052 | Reject IN_PROGRESS → OPEN | Backward transition | Parameterized integration | Automated | Automated |
| AC-053 | Reject RESOLVED → IN_PROGRESS | Backward transition | Parameterized integration | Automated | Automated |
| AC-054 | Reject RESOLVED → CANCELLED | Invalid transition | Parameterized integration | Automated | Automated |
| AC-055 | Reject from CLOSED | Terminal guard | Parameterized integration | Automated | Automated |
| AC-056 | Reject from CANCELLED | Terminal guard | Parameterized integration | Automated | Automated |
| AC-057 | Reject invalid status value | PATCH invalid enum → 400 | Servlet + validator integration | Automated | Automated |
| AC-060 | Add comment via UI | Submit comment form | — | Manual | Manual |
| AC-061 | Add comment via API | POST comment → 201 | Servlet + comment integration | Automated | Automated |
| AC-062 | Reject empty comment | Blank message rejected | Comment integration + validator | Automated | Partial (not servlet POST 400 assertion) |
| AC-063 | Comment updates updatedAt | Parent timestamp refreshed | Comment integration | Automated | Automated |
| AC-070 | Search matches title | Keyword in title | Search integration | Automated | Automated |
| AC-071 | Search matches description | Keyword in description | Search integration | Automated | Automated |
| AC-072 | Case-insensitive search | Mixed-case title | Search integration | Automated | Automated |
| AC-073 | No matches → empty list | Non-matching keyword | Search integration + servlet GET | Automated | Automated |
| AC-080 | Filter by status | OPEN filter | Search integration | Automated | Automated |
| AC-081 | Filter each status | Parameterized all statuses | Search integration | Automated | Automated |
| AC-082 | Combined search + filter | Keyword AND status | Search integration | Automated | Automated |
| AC-090 | Survives AEM restart | Restart Author, data remains | — | Manual | Not covered |
| AC-091 | Seed data after setup | Repoinit users/tickets path | — | Setup | Manual |
| AC-092 | JCR storage (not in-memory) | Inspect `/content/support-tickets` | — | Manual | Partial (AEM Mock is in-memory; manual on real AEM) |
| AC-093 | Setup/migration scripts | Repoinit in ui.config | — | Setup | Manual |
| AC-100 | Structured validation errors | 400 with `fields` map | Servlet integration | Automated | Partial |
| AC-101 | No stack traces in API | Inspect error responses | — | Manual | Not covered |
| AC-102 | 409 invalid transition context | 409 with transition details | Servlet integration | Automated | Automated |
| AC-110 | UI shows API errors | Trigger API failure in browser | — | Manual | Manual |
| AC-111 | UI not sole validator | Disable JS, submit invalid payload | — | Manual | Manual |
| AC-120 | Seeded users with fields | GET users.json | Servlet integration (mocked users) | Automated | Partial (HTTP 200 only; not live repoinit users) |
| AC-121 | No user-management UI | Navigate app | — | Manual | Manual |
| AC-130 | Frontend present | Pages at `/content/support-app` | — | Manual | Manual |
| AC-131 | Backend API present | `/bin/support-tickets` endpoints | Servlet/path parser tests | Automated | Partial |
| AC-132 | Backend validation | Validator + integration suite | Multiple integration tests | Automated | Automated |
| AC-133 | README setup | Follow README | — | Setup | Manual |
| AC-134 | No secrets in repo | Code review | — | Code review | Manual |
| AC-135 | Lifecycle artifacts | Files present | — | Code review | Manual |
| AC-136 | Prompt history | Artifact present | — | Code review | Manual |
| AC-150 | Runs on AEMaaCS SDK | Local SDK Author | — | Setup | Manual |
| AC-151 | Publish tier | Deploy to Publish | — | Manual | Not covered |
| AC-152 | Dispatcher | Publish via Dispatcher | — | Manual | Not covered |
| AC-160 | State machine integration tests exist | `TicketStateMachineIntegrationTest` | Integration | Automated | Automated |
| AC-161 | Five valid transitions in tests | ac040–ac044 tests | Integration + unit | Automated | Automated |
| AC-162 | Invalid transitions in tests | Parameterized + exhaustive unit | Integration + unit | Automated | Automated |

---

## 12. Known test limitations

| Limitation | Impact |
|------------|--------|
| **AEM Mock ≠ live AEM** | ACLs, repoinit, UserManager authorizables, and Oak indexing behave differently on a real Author instance. |
| **Mocked `UserLookupService` in integration base** | User-existence rules are tested; live `/home/users/support` listing is only covered by `UserLookupServiceImplTest` (mocked UserManager). |
| **Mocked `QueryBuilder`** | Search integration tests use JCR traversal fallback, not production QueryBuilder/Oak index predicates. |
| **Servlet tests bypass Sling filter chain** | CSRF, authentication, and Dispatcher rules are not exercised in `SupportTicketsApiIntegrationTest`. |
| **No PUT/PATCH/GET detail servlet coverage** for happy paths | Most update/read scenarios are repository integration tests only. |
| **No UI or E2E automation** | Clientlibs, HTL rendering, CSRF fetch from browser, and navigation flows require manual testing. |
| **`it.tests` not ticket-specific** | Cloud Manager functional testing module does not validate support-ticket API or pages. |
| **No restart/durability test** | AC-090 not automated. |
| **No performance tests** | Large ticket volumes and index latency not tested. |
| **Archetype boilerplate tests** | Five non-domain tests remain from project archetype; they do not validate ticket functionality. |
| **Error injection** | Persistence failures (500) and service-user login failures not simulated. |

---

## 13. Recommended future tests

Separated from what is **currently implemented**.

### High value (Core)

| Test | Addresses |
|------|-----------|
| Live AEM HTTP tests in `it.tests` for full `/bin/support-tickets` CRUD + transitions | Servlet + service-user + repoinit integration |
| Servlet integration tests for PUT update, GET detail, PATCH all valid transitions | Gaps in `SupportTicketsApiIntegrationTest` |
| POST comment / create with unknown user through servlet | End-to-end HTTP validation |
| `UserLookupServiceImpl` AEM Mock integration with authorizable fixtures | AC-120 against realistic user nodes |
| QueryBuilder integration test with unmocked predicate (or Oak base) | Production search path |
| Invalid priority **string** on create via servlet | AC-004 completeness |

### UI / E2E

| Test | Addresses |
|------|-----------|
| Cypress: create ticket flow | AC-001, CSRF + redirect |
| Cypress: list search/filter | AC-070–082 UI path |
| Cypress: detail edit, status change, comment | AC-030–044, AC-060–063 |
| Client-side unit tests for `api.js` / `utils.js` | Regression on fetch/CSRF/error mapping |

### Infrastructure

| Test | Addresses |
|------|-----------|
| Dispatcher filter rules for `/bin/support-tickets` | AC-152 |
| Publish-tier smoke tests | AC-151 |
| Restart durability script | AC-090 |
| CI pipeline running `mvn -pl core test` on every PR | Regression gate |

### Stretch (out of Core scope)

| Test | Addresses |
|------|-----------|
| Authentication / RBAC | Security hardening |
| Rate limiting | Abuse prevention |
| Penetration / OWASP scan | Security audit |

---

## Appendix A — Test class reference

| Class | Package | Tier |
|-------|---------|------|
| `TicketStateMachineServiceImplTest` | `...service.impl` | Unit |
| `TicketValidatorImplTest` | `...validation.impl` | Unit |
| `UserLookupServiceImplTest` | `...service.impl` | Unit |
| `ApiPathParserTest` | `...servlets.support` | Unit |
| `TicketRepositoryTest` | `...repository.impl` | AEM Mock / repository |
| `TicketPersistenceIntegrationTest` | `...integration` | Integration |
| `TicketStateMachineIntegrationTest` | `...integration` | Integration |
| `TicketValidationIntegrationTest` | `...integration` | Integration |
| `TicketSearchIntegrationTest` | `...integration` | Integration |
| `TicketCommentIntegrationTest` | `...integration` | Integration |
| `SupportTicketsApiIntegrationTest` | `...integration` | API / servlet |
| `SupportAppPageModelTest` | `...models` | Model unit |

---

## Appendix B — Verification statement

This document was verified against:

- All test classes under `core/src/test/java/com/supporttickets/core/`
- `it.tests` and `ui.tests` module contents
- Repoinit seed configuration in `ui.config`
- API surface in `SupportTicketsApiServlet` and endpoint classes
- Clientlibs in `ui.apps/.../clientlib-support-app/`

No test tier or scenario is claimed unless a corresponding test class or manual/setup criterion exists in the repository.
