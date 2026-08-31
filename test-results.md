# Test Results — Support Ticket Management System

**Project:** AI Practical Assessment  
**Platform:** AEM as a Cloud Service (AEMaaCS)  
**Document type:** Test execution results (evidence-based)  
**Related:** [test-strategy.md](test-strategy.md)  
**Document version:** 1.0

---

## 1. Test execution summary

| Field | Value |
|-------|-------|
| **Primary execution date/time** | `2026-08-31T12:14:30+05:30` (from `mvn -B clean install` Maven log) |
| **Supplementary core-only run** | `2026-08-31T12:08:46+05:30` (`mvn -B -pl core test`) |
| **Host OS** | Windows 11 (`os.arch=amd64`) |
| **Java version** | 21.0.9 (Oracle JDK, `java.specification.version=21`) |
| **Maven version** | Apache Maven 3.9.14 |
| **AEM SDK API (dependency)** | `2026.8.27673.20260811T193135Z-260700` (parent `pom.xml` property `aem.sdk.api`) |
| **JUnit (core tests)** | JUnit Jupiter 5.8.2 |
| **Test runner (core)** | Maven Surefire Plugin 2.22.1 |
| **AEM Mock** | io.wcm.testing.aem-mock 5.5.4 |

### Commands executed (this session)

| Command | Result | Evidence |
|---------|--------|----------|
| `mvn -B -pl core test` | **BUILD SUCCESS** | Console output; finished `2026-08-31T12:08:46+05:30`; total time 28.275 s |
| `mvn -B clean install` | **BUILD SUCCESS** | Console output; finished `2026-08-31T12:14:30+05:30`; total time 05:21 min |

### Overall automated test result (Core module)

| Metric | Value |
|--------|-------|
| **Tests run** | 131 |
| **Failures** | 0 |
| **Errors** | 0 |
| **Skipped** | 0 |
| **Overall** | **PASS** (all 131 Core module tests) |

### Tests not executed in this session

| Suite | Status | Notes |
|-------|--------|-------|
| **Live AEM `it.tests` (`*IT.java`)** | Not executed | `mvn clean install` packaged `it.tests` only; Failsafe ITs require `-Plocal` and a running Author/Publish instance |
| **Cypress UI tests (`ui.tests`)** | Not executed | Build ran `npm ci` and `npm run lint` only; no Cypress spec run recorded |
| **Manual UI verification** | Partial (session evidence) | Author `:4502` smoke during debugging — see §4.1; not a formal AC walkthrough |
| **Dispatcher `validate.sh`** | Not executed | [Result not available in project artifacts] |
| **Code coverage (JaCoCo or similar)** | Not measured | [Result not available in project artifacts] |

---

## 2. Build validation

### 2.1 Full reactor build

```text
mvn -B clean install
```

| Field | Value |
|-------|-------|
| **Started** | During session on 2026-08-31 (exact start timestamp not printed in captured log) |
| **Finished** | `2026-08-31T12:14:30+05:30` |
| **Total time** | 05:21 min |
| **Result** | **BUILD SUCCESS** |

#### Reactor module results

| Module | Artifact | Packaging | Build result | Duration (from log) |
|--------|----------|-----------|--------------|---------------------|
| Parent | `support-tickets` | pom | SUCCESS | 1.426 s |
| Core | `support-tickets.core` | jar | SUCCESS | 29.081 s |
| Structure | `support-tickets.ui.apps.structure` | content-package | SUCCESS | 2.343 s |
| UI apps | `support-tickets.ui.apps` | content-package | SUCCESS | 4.978 s |
| UI content | `support-tickets.ui.content` | content-package | SUCCESS | 1.658 s |
| UI config | `support-tickets.ui.config` | content-package | SUCCESS | 0.475 s |
| All | `support-tickets.all` | content-package | SUCCESS | 40.349 s |
| Integration Tests | `support-tickets.it.tests` | jar | SUCCESS | 18.604 s |
| Dispatcher | `support-tickets.dispatcher.cloud` | pom (zip) | SUCCESS | 0.286 s |
| Dispatcher AMS | `support-tickets.dispatcher.ams` | pom (zip) | SUCCESS | 0.299 s |
| UI Tests | `com.adobe.cq.cloud.testing.ui.cypress.tests` | pom | SUCCESS | 01:22 min |
| UI Frontend Forms | `support-tickets.ui.frontend.react.forms.af` | pom | SUCCESS | 02:12 min |

### 2.2 Core module test command (standalone)

```text
mvn -B -pl core test
```

| Field | Value |
|-------|-------|
| **Finished** | `2026-08-31T12:08:46+05:30` |
| **Total time** | 28.275 s |
| **Tests run** | 131 |
| **Failures / Errors / Skipped** | 0 / 0 / 0 |
| **Result** | **BUILD SUCCESS** |

### 2.3 Package validation performed during build

The following validations **ran as part of** `mvn clean install` (build succeeded):

| Validation | Module(s) | Result |
|------------|-----------|--------|
| HTL script validation | `ui.apps` | Completed (no build failure) |
| FileVault package validation | `ui.apps.structure`, `ui.apps`, `ui.content`, `ui.config`, `all` | Completed (no build failure) |
| AEM Analyser (`aemanalyser-maven-plugin` 1.6.6) | `all` | Completed with **WARNING** (outdated analyser plugin version noted in log) |
| Dispatcher immutable-file checksum enforcer | `dispatcher`, `dispatcher.ams` | Completed (no build failure) |
| ESLint (`npm run lint`) | `ui.tests/test-module` | Completed (no build failure) |
| Webpack production build | `ui.frontend.react.forms.af` | Completed with **4 warnings** (see §6) |

### 2.4 Commands not executed

| Command | Status |
|---------|--------|
| `mvn -B clean install -PautoInstallSinglePackage` | [Result not available in project artifacts] |
| `mvn -B -pl it.tests -Plocal verify` | [Result not available in project artifacts] |
| `cd dispatcher && ./bin/validate.sh src` | [Result not available in project artifacts] |
| Cypress test run against live AEM | [Result not available in project artifacts] |

---

## 3. Core module — detailed test results

**Report location:** `core/target/surefire-reports/`  
**Evidence files:** `TEST-*.xml`, `*.txt`  
**Environment at execution:** Java 21.0.9, Windows 11, AEM Mock in-process (no live AEM instance)

### 3.1 Summary by test class

| Test class | Tests run | Failures | Errors | Skipped | Result |
|------------|-----------|----------|--------|---------|--------|
| `SupportTicketsApiIntegrationTest` | 11 | 0 | 0 | 0 | PASS |
| `TicketCommentIntegrationTest` | 5 | 0 | 0 | 0 | PASS |
| `TicketPersistenceIntegrationTest` | 12 | 0 | 0 | 0 | PASS |
| `TicketSearchIntegrationTest` | 13 | 0 | 0 | 0 | PASS |
| `TicketStateMachineIntegrationTest` | 17 | 0 | 0 | 0 | PASS |
| `TicketValidationIntegrationTest` | 11 | 0 | 0 | 0 | PASS |
| `TicketRepositoryTest` | 7 | 0 | 0 | 0 | PASS |
| `TicketStateMachineServiceImplTest` | 31 | 0 | 0 | 0 | PASS |
| `TicketValidatorImplTest` | 7 | 0 | 0 | 0 | PASS |
| `UserLookupServiceImplTest` | 2 | 0 | 0 | 0 | PASS |
| `ApiPathParserTest` | 9 | 0 | 0 | 0 | PASS |
| `SupportAppPageModelTest` | 1 | 0 | 0 | 0 | PASS |
| `LoggingFilterTest` | 1 | 0 | 0 | 0 | PASS |
| `HelloWorldModelTest` | 1 | 0 | 0 | 0 | PASS |
| `SimpleServletTest` | 1 | 0 | 0 | 0 | PASS |
| `SimpleResourceListenerTest` | 1 | 0 | 0 | 0 | PASS |
| `SimpleScheduledTaskTest` | 1 | 0 | 0 | 0 | PASS |
| **Total** | **131** | **0** | **0** | **0** | **PASS** |

### 3.2 Support-ticket domain tests — method-level results

All methods below **passed** (0 failures, 0 errors) per Surefire XML reports generated during `mvn -B clean install`.

#### `SupportTicketsApiIntegrationTest` (11)

| Test method | Result |
|-------------|--------|
| `ac002_postCreateReturns201` | PASS |
| `ac003_postCreateValidationErrorReturns400` | PASS |
| `ac022_getMissingTicketReturns404` | PASS |
| `ac034_putWithStatusReturns400` | PASS |
| `ac040_validTransitionReturns200` | PASS |
| `ac050_invalidTransitionReturns409` | PASS |
| `ac057_patchUnknownStatusReturns400` | PASS |
| `ac061_postCommentReturns201` | PASS |
| `ac073_getSearchNoMatchesReturnsEmptyArray` | PASS |
| `ac120_getUsersReturns200` | PASS |
| `ticketDetailSerializesForApiResponses` | PASS |

#### `TicketPersistenceIntegrationTest` (12)

| Test method | Result |
|-------------|--------|
| `ac002_createTicketPersistsOpenTicket` | PASS |
| `ac003_invalidCreateDoesNotPersist` | PASS |
| `ac006_createAlwaysForcesOpenStatus` | PASS |
| `ac011_createdTicketIsRetrievable` | PASS |
| `ac020_retrieveTicketDetail` | PASS |
| `ac022_missingTicketThrowsNotFound` | PASS |
| `ac030_updateTitle` | PASS |
| `ac031_updateDescription` | PASS |
| `ac032_updatePriority` | PASS |
| `ac033_reassignTicket` | PASS |
| `ac033_unassignTicket` | PASS |
| `updateDoesNotChangeStatus` | PASS |

#### `TicketStateMachineIntegrationTest` (17)

| Test method | Result |
|-------------|--------|
| `ac040_openToInProgress` | PASS |
| `ac041_inProgressToResolved` | PASS |
| `ac042_resolvedToClosed` | PASS |
| `ac043_openToCancelled` | PASS |
| `ac044_inProgressToCancelled` | PASS |
| `invalidTransitionsAreRejected[1]` … `[11]` (11 parameterized cases) | PASS (each) |
| `validTransitionUpdatesTimestamp` | PASS |

#### `TicketValidationIntegrationTest` (11)

| Test method | Result |
|-------------|--------|
| `ac003_rejectMissingTitle` | PASS |
| `ac004_rejectMissingPriority` | PASS |
| `ac005_rejectUnknownCreatedBy` | PASS |
| `ac005_rejectUnknownAssignedTo` | PASS |
| `ac007_rejectCreatedByOnUpdate` | PASS |
| `ac034_rejectStatusOnUpdate` | PASS |
| `ac057_rejectUnknownStatusEnum` | PASS |
| `ac057_rejectMissingStatusValue` | PASS |
| `ac057_acceptValidStatusEnum` | PASS |
| `rejectBlankTitleOnUpdate` | PASS |
| `rejectCommentWithUnknownAuthor` | PASS |

#### `TicketSearchIntegrationTest` (13)

| Test method | Result |
|-------------|--------|
| `ac010_listAllTickets` | PASS |
| `ac070_searchMatchesTitle` | PASS |
| `ac071_searchMatchesDescription` | PASS |
| `ac072_searchIsCaseInsensitive` | PASS |
| `ac073_searchNoMatchesReturnsEmptyList` | PASS |
| `ac080_filterByStatus` | PASS |
| `ac081_filterByEachStatus[1]` … `[5]` (5 statuses) | PASS (each) |
| `ac082_combinedSearchAndStatusFilter` | PASS |
| `invalidStatusFilterRejected` | PASS |

#### `TicketCommentIntegrationTest` (5)

| Test method | Result |
|-------------|--------|
| `ac061_addCommentPersistsComment` | PASS |
| `commentsAppearOnTicketDetail` | PASS |
| `ac062_commentOnMissingTicketFails` | PASS |
| `ac063_addCommentRefreshesParentUpdatedAt` | PASS |
| `blankCommentMessageRejected` | PASS |

#### `TicketRepositoryTest` (7)

| Test method | Result |
|-------------|--------|
| `createTicketPersistsOpenStatus` | PASS |
| `updateDoesNotChangeStatus` | PASS |
| `updateStatusDelegatesToStateMachine` | PASS |
| `addCommentUpdatesTicketTimestampAndSortsComments` | PASS |
| `updateStatusInvalidTransitionDoesNotModifyTicket` | PASS |
| `findByIdThrowsForMissingTicket` | PASS |
| `findByIdThrowsForInvalidId` | PASS |

#### `TicketStateMachineServiceImplTest` (31)

| Test category | Tests run | Result |
|---------------|-----------|--------|
| `applyTransition_validTransitions` (5 valid pairs) | 5 | PASS |
| `applyTransition_invalidTransitions_throwInvalidTransitionException` (exhaustive invalid matrix) | 20 | PASS |
| `getAllowedTransitions_returnsExpectedSet` (5 statuses) | 5 | PASS |
| `getAllowedTransitions_returnsUnmodifiableCopy` | 1 | PASS |

#### Other unit / component tests (20)

| Test class | Tests run | Result |
|------------|-----------|--------|
| `TicketValidatorImplTest` | 7 | PASS |
| `UserLookupServiceImplTest` | 2 | PASS |
| `ApiPathParserTest` | 9 | PASS |
| `SupportAppPageModelTest` | 1 | PASS |
| Archetype samples (`HelloWorldModelTest`, `SimpleServletTest`, `LoggingFilterTest`, `SimpleResourceListenerTest`, `SimpleScheduledTaskTest`) | 5 | PASS |

---

## 4. Acceptance-criteria execution evidence

This section records **only** criteria with automated test execution evidence from the Core module run above.  
Criteria requiring manual UI, live AEM, or unexecuted suites are marked accordingly.

| AC | Automated test evidence | Execution result |
|----|----------------------|------------------|
| AC-002 | `ac002_postCreateReturns201`, `ac002_createTicketPersistsOpenTicket` | PASS |
| AC-003 | `ac003_postCreateValidationErrorReturns400`, `ac003_rejectMissingTitle`, `ac003_invalidCreateDoesNotPersist` | PASS |
| AC-004 | `ac004_rejectMissingPriority` | PASS (missing priority only) |
| AC-005 | `ac005_rejectUnknownCreatedBy`, `ac005_rejectUnknownAssignedTo` | PASS |
| AC-006 | `ac006_createAlwaysForcesOpenStatus` | PASS |
| AC-007 | `ac007_rejectCreatedByOnUpdate` | PASS |
| AC-010 | `ac010_listAllTickets` | PASS |
| AC-011 | `ac011_createdTicketIsRetrievable` | PASS |
| AC-020 | `ac020_retrieveTicketDetail` | PASS |
| AC-021 | `commentsAppearOnTicketDetail` | PASS |
| AC-022 | `ac020_retrieveTicketDetail` (checks `allowedTransitions` at repository layer) | PASS |
| AC-023 | `ac022_getMissingTicketReturns404`, `ac022_missingTicketThrowsNotFound` | PASS |
| AC-030 | `ac030_updateTitle`, `updateDoesNotChangeStatus`, `rejectBlankTitleOnUpdate` | PASS |
| AC-031 | `ac031_updateDescription` | PASS |
| AC-032 | `ac032_updatePriority` | PASS |
| AC-033 | `ac033_reassignTicket`, `ac033_unassignTicket` | PASS |
| AC-034 | `ac034_putWithStatusReturns400`, `ac034_rejectStatusOnUpdate` | PASS |
| AC-040 – AC-044 | State machine integration + unit tests | PASS |
| AC-050 – AC-056 | Parameterized integration + unit matrix + `ac050_invalidTransitionReturns409` | PASS |
| AC-057 | `ac057_*` validation and servlet tests | PASS |
| AC-061 | `ac061_postCommentReturns201`, `ac061_addCommentPersistsComment` | PASS |
| AC-062 | `blankCommentMessageRejected` | PASS |
| AC-063 | `ac063_addCommentRefreshesParentUpdatedAt` | PASS |
| AC-070 – AC-073 | Search integration + `ac073_getSearchNoMatchesReturnsEmptyArray` | PASS |
| AC-080 – AC-082 | Search integration tests | PASS |
| AC-120 | `ac120_getUsersReturns200` | PASS (HTTP 200; mocked user list in test) |
| AC-160 | `TicketStateMachineIntegrationTest` present and passing | PASS |
| AC-161 | ac040–ac044 integration tests passing | PASS |
| AC-162 | 11 parameterized invalid transition cases + unit matrix passing | PASS |
| AC-001, AC-060, AC-090 – AC-093, AC-100 – AC-101, AC-110 – AC-111, AC-121, AC-130 – AC-136, AC-150 – AC-152 | No automated execution evidence in this session | [Result not available in project artifacts] — partial manual checks in §4.1 |

---

## 4.1 Manual Author E2E smoke (development session evidence)

> **Scope:** Informal smoke checks performed on **AEM Author** (`localhost:4502`) while debugging during implementation (Aug 30–31, 2026).  
> **Evidence source:** Cursor session transcript, [review-fixes.md](review-fixes.md) RF-001–RF-003, [debugging-notes.md](debugging-notes.md).  
> **Not equivalent to:** formal manual test script execution, full AC walkthrough, Cypress, or Publish/Dispatcher E2E.

### Environment

| Field | Value |
|-------|-------|
| **Instance** | AEM Author Quickstart |
| **Port** | `4502` (assumed per README and debug prompts) |
| **Quickstart version** | [Not recorded in project artifacts] |
| **Package deploy** | Assumed via local install during development; `mvn -PautoInstallSinglePackage` result not recorded |

### Recorded checks

| # | Check | Method | Result | Evidence |
|---|-------|--------|--------|----------|
| 1 | Nested API `GET /bin/support-tickets/users.json` | Browser or direct URL on Author | **PASS** (after ResourceProvider fix) | [debugging-notes.md](debugging-notes.md) DBG-06; transcript Aug 30 |
| 2 | `GET /libs/granite/csrf/token.json` | Browser Network tab | **PASS** — HTTP 200 | [review-fixes.md](review-fixes.md) RF-001 |
| 3 | Create ticket `POST /bin/support-tickets.json` from UI | Browser Network tab after CSRF fix | **PASS** — POST visible | RF-001; developer confirmation in session |
| 4 | `GET /bin/support-tickets/users.json` returns seeded users | API / create form dropdown | **PASS** (after UserManager fix) | RF-003; session history |
| 5 | Create ticket redirect to detail page | UI after successful create | **Fixed in source** | RF-002; end-to-end redirect not in formal log |
| 6 | List / search / filter UI | — | [Not recorded] | — |
| 7 | Status transition via UI | — | [Not recorded] | — |
| 8 | Comment via UI | — | [Not recorded] | — |
| 9 | Publish tier / Dispatcher `:80` | — | [Not executed] | CR-001 |

### Limitations of this record

1. **Session-derived**, not a repeatable test script with timestamps and screenshots.
2. **Author only** — Publish replication and Dispatcher routing not validated.
3. **Targeted at debug failures** — not systematic coverage of all 67 ACs.
4. Several UI flows (list search, status PATCH from detail, comments) have **no recorded** manual outcome.

### Relationship to automated tests

| Concern | AEM Mock (§3) | Manual Author (§4.1) |
|---------|---------------|------------------------|
| State machine / validation | Covered (131 tests) | Not re-verified manually |
| Nested `/bin` servlet routing | Not caught | **Caught** on Author |
| User listing (`UserManager`) | Mocked in integration base | **Caught** on Author |
| CSRF `fetch()` header | Not applicable | **Caught** on Author |

---

## 5. Modules built but not test-executed

### 5.1 `it.tests` (live AEM integration tests)

| Field | Value |
|-------|-------|
| **Build during `clean install`** | JAR packaged successfully |
| **Surefire tests run** | 0 (no test methods executed; sources compiled from `src/main/java`) |
| **Failsafe IT execution** | Not triggered (`local` profile not active) |
| **IT classes in repo** | `CreatePageIT.java`, `GetPageIT.java` (archetype samples; not support-ticket-specific) |
| **Execution result** | [Result not available in project artifacts] |

### 5.2 `ui.tests` (Cypress)

| Field | Value |
|-------|-------|
| **Build steps observed** | `npm ci`, `npm run lint`, Docker context assembly |
| **Cypress spec execution** | Not observed in Maven log |
| **Specs in repo** | `login.cy.js`, `basic.cy.js`, `assets.cy.js`, `console_error.cy.js` (archetype; no support-app specs) |
| **Execution result** | [Result not available in project artifacts] |

---

## 6. Build warnings (non-test)

The following warnings appeared during `mvn -B clean install` but did **not** fail the build:

| Source | Warning |
|--------|---------|
| AEM Analyser | Project configured with outdated `aemanalyser-maven-plugin` version 1.6.6 |
| `ui.frontend.react.forms.af` webpack | Module not found: `@quarry/eim-provider` (1 warning) |
| `ui.frontend.react.forms.af` webpack | Asset / entrypoint size exceeds recommended limit (performance warnings) |
| `ui.tests` npm audit | 6 vulnerabilities reported during `npm ci` |
| `ui.frontend.react.forms.af` npm audit | 39 vulnerabilities reported during `npm ci` |

---

## 7. Report artifacts and logs

| Artifact | Path |
|----------|------|
| Surefire XML reports | `core/target/surefire-reports/TEST-*.xml` |
| Surefire text summaries | `core/target/surefire-reports/*.txt` |
| Core test classes | `core/target/test-classes/` |
| Core OSGi bundle | `core/target/support-tickets.core-1.0.0-SNAPSHOT.jar` |
| Full content package | `all/target/support-tickets.all-1.0.0-SNAPSHOT.zip` |
| IT tests JAR | `it.tests/target/support-tickets.it.tests-1.0.0-SNAPSHOT-jar-with-dependencies.jar` |
| UI test Docker context | `ui.tests/target/com.adobe.cq.cloud.testing.ui.cypress.tests-0.0.1-SNAPSHOT-ui-test-docker-context.tar.gz` |
| Maven build log (this session) | Captured during agent execution on 2026-08-31 |

---

## 8. Limitations of this results record

1. **In-process only:** Core tests use AEM Mock and mocked `UserLookupService` / `QueryBuilder`; they do not prove behaviour on a live AEM Author instance.
2. **No coverage metrics:** Code coverage was not collected or reported.
3. **No formal UI/E2E test run:** Cypress and scripted manual AC walkthroughs were not executed in this session. Partial Author smoke during debugging is documented in §4.1 only.
4. **No live IT results:** `CreatePageIT` / `GetPageIT` were not run against `localhost:4502`.
5. **Many manual acceptance criteria** (AC-001, AC-060, AC-110–AC-111, AC-130–AC-136, AC-150–AC-152) remain without formal verification evidence.
6. This document reflects execution on **one machine** (Windows 11, Java 21.0.9, Maven 3.9.14) at the timestamps above; re-running tests may produce different timings but should yield the same pass/fail counts if the codebase is unchanged.

---

## 9. Verification statement

Results recorded in this document are derived from:

- Live execution of `mvn -B -pl core test` and `mvn -B clean install` on 2026-08-31
- Surefire reports under `core/target/surefire-reports/`
- Maven reactor summary in the build log

No test pass/fail counts were inferred from source code inspection. Items without execution evidence are explicitly marked `[Result not available in project artifacts]`.
