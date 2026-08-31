# Final AI Usage Summary — Support Ticket Management System

**Project:** AI Practical Assessment (AEMaaCS)  
**Primary AI tool:** Cursor IDE (Agent, Plan mode, Shell, AskQuestion)  
**Session evidence:** Cursor transcript `13262a8d-8e65-407b-ab4b-200d6bdc9f58` (Aug 26–31, 2026)  
**Companion docs:** [tool-workflow.md](tool-workflow.md), [reflection.md](reflection.md)

This document summarizes **how AI was used** at each lifecycle stage, what the developer reviewed or overrode, and how outcomes were validated. Claims are grounded in repository artifacts and recorded session history — not invented outcomes.

---

## Cross-cutting pattern

| Role | AI | Developer |
|------|-----|-----------|
| **Draft** | Specs, code, tests, docs | — |
| **Decide** | Propose options | Approve via AskQuestion, explicit prompts, or rejection |
| **Validate** | Run `mvn test` / `mvn install` via Shell | Runtime AEM smoke, Network tab, ACL inspection |
| **Correct** | Revise after failed validation | Report symptoms; direct structural fixes (e.g. flat repo root) |

**Spec-driven rule** (developer prompt, Aug 27): approved specs are source of truth; AI must flag conflicts before changing design.

---

## 1. Requirements analysis

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Decompose the full assignment brief into Core vs Stretch; tag [Explicit], [Assumption], [Recommendation]; produce `requirements-analysis.md`. |
| **What AI contributed** | Initial `CreatePlan` decomposition; FR-C01–C11 Core requirements; Stretch backlog; AEM/JCR/database interpretation; 11 ambiguities and 15 working assumptions. |
| **Developer review / challenge** | Before specs were written, developer triggered a **critical technical review** prompt: *"Try to find weaknesses, ambiguities, hidden requirements… Pay particular attention to AEM Author vs Publisher, JCR, CSRF, Dispatcher, state machine…"* AI produced adversarial findings (topology, caching, status-on-PUT risk). |
| **Developer decisions** | Via **AskQuestion** (Aug 26): **Author + Publish UI** (not Author-only); **Sling Servlets API** (not Sling Models-as-API). |
| **Validation** | Requirements folded into `requirements-analysis.md`; cross-referenced in later specs. No code until specs approved. |

**Example prompt:** *"We are building a Support Ticket Management System together. Below are the requirements…"* (opening session message, Aug 26).

**Changed recommendation:** AI initially offered topology/API as choices; developer selections locked dual-surface UI and servlet API for all downstream design.

---

## 2. Acceptance criteria

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Convert approved requirements into a testable matrix with ID, preconditions, action, expected result, HTTP/UI expectations, test type, priority. Include every valid/invalid state transition, search, filter, persistence, comments, errors. |
| **What AI contributed** | `acceptance-criteria.md` — **67 ACs** in 15 sections (AC-001–AC-136, AC-150–AC-162); explicit valid transitions AC-040–AC-044; invalid transitions AC-050–AC-057. |
| **Developer review** | Prompt required coverage of original assignment without adding product scope. AI read `requirements-analysis.md` before writing. |
| **Developer decisions** | Accepted matrix as "Approved for Core verification" — became test naming and traceability baseline. |
| **Validation** | AC IDs later mapped to integration test methods (e.g. `ac040_validTransitionReturns200`). Gaps documented in [test-results.md](test-results.md) §4 (manual UI, live IT, Cypress not executed). |

**Example prompt:** *"Using the approved requirements analysis, convert the requirements into a precise, testable acceptance-criteria matrix… Pay special attention to the ticket state machine."*

---

## 3. Architecture / design

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Design 17 architectural areas for AEMaaCS (modules, OSGi boundaries, servlet boundaries, search, user lookup, auth, frontend, testing, seed data). Record alternatives and trade-offs per decision. |
| **What AI contributed** | `design-notes.md` — module map (`core`, `ui.apps`, `ui.content`, `ui.config`, `all`, `dispatcher`); Author-as-write-master + replication; service user pattern; no Core auth; state machine §18; pre-implementation security review §19. |
| **Developer review** | AI read `acceptance-criteria.md` and `requirements-analysis.md` before drafting. Developer requested separate **state machine design review** and **pre-implementation security review** as additional prompts. |
| **Developer decisions** | Accepted JCR persistence, open API for Core, status-via-PATCH-only, seeded users as AEM principals (not ticket nodes). |
| **Validation** | Design docs marked "Approved for implementation planning." Later implementation checked against design; conflicts flagged (e.g. filter approach abandoned — see §9). |

**Example prompt:** *"Using the approved requirements and acceptance criteria, design the architecture… For each architectural decision: identify alternatives, explain trade-offs, recommend a choice."*

---

## 4. Data model

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Define JCR hierarchy, node types, properties, validation constraints, search fields, seed-user representation. |
| **What AI contributed** | `data-model.md` — tickets at `/content/support-tickets/tickets/{uuid}`; comments as child nodes; users under `/home/users/support/`; single-writer principle (`TicketRepository` only). |
| **Developer review** | AI cross-checked `design-notes.md` §11 and acceptance criteria before writing. |
| **Developer decisions** | Accepted flat ticket folder (no date/category sub-trees); UUID node names. |
| **Validation** | `TicketRepositoryImpl` and repoinit scripts implemented to match paths; persistence integration tests (AC-090–AC-093) pass in AEM Mock. |

**Later correction:** Initial runtime user lookup used `Resource.getChildren()` on user paths — **contradicted** Oak authorizable semantics. Fixed in debugging (§9) with `UserManager`, not by changing the data model doc.

---

## 5. API design

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Define minimum Core REST surface: 7 endpoints, error format, status separation, examples. Do not implement yet. |
| **What AI contributed** | `api-contract.md` — `PUT` rejects `status`/`createdBy`; `PATCH .../status.json` only for transitions; `ApiErrorResponse` envelope; CSRF notes for Publish. |
| **Developer review** | Prompt explicitly required separate status-transition API unless strong reason not to — AI aligned with PATCH-only design. |
| **Developer decisions** | Accepted 7-endpoint contract as implementation boundary. |
| **Validation** | `SupportTicketsApiIntegrationTest` (11 tests) validates status codes and rejection rules. **Doc drift:** contract still references `:cq_csrf_token`; code uses `CSRF-Token` (CR-007) — docs not updated when RF-001 fixed frontend. |

**Example prompt:** *"Create the REST API contract… The status-transition API must be separate from general ticket updates unless there is a strong architectural reason not to."*

---

## 6. Implementation planning

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Produce phased task plan mapping to ACs; state machine early; buildable increments; TDD checkpoints. |
| **What AI contributed** | `implementation-plan.md` — Phases 0–11; task IDs T-010+; explicit "state machine before servlets" ordering; `ui-flow.md` for 3-screen UI. |
| **Developer review** | Plan reviewed before implementation start. |
| **Developer decisions** | Approved spec-driven implementation workflow (Aug 27 prompt): use approved specs as source of truth; identify ambiguities before coding. |
| **Validation** | Phase checkpoints referenced `mvn clean install` and `mvn test`; phase order followed in implementation session. |

**Example prompt:** *"You are now moving from the approved specification/design phase into implementation… Use the previously approved… as the source of truth… Where an implementation decision is ambiguous: identify, refer to spec, recommend, explain before implementing."*

---

## 7. AEM project foundation

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Scaffold AEMaaCS Maven project (archetype v57), configure Java 21 + latest SDK, minimal package structure — no business logic yet. |
| **What AI contributed** | `mvn archetype:generate`; `CreatePlan` for foundation; POM fixes for Java 21 and SDK API version; repoinit/service-user scaffolding in `ui.config`. |
| **Developer review / challenge** | Archetype initially generated under nested `support-tickets/` subfolder; developer directed **move to flat repo root** to match assessment layout. |
| **Developer decisions** | **Java 21** for builds (matches `.cloudmanager/java-version`) over default PATH Java 11; flat repository root. |
| **Validation** | `mvn clean install` — BUILD SUCCESS after relocation and Java 21 configuration ([test-results.md](test-results.md)). WebSearch used for archetype/SDK version lookup. |

**Changed recommendation:** AI scaffolded nested folder structure; developer overrode with explicit relocation to `d:\ai-practical-assessment\`.

**Build influenced next step:** Failed/partial archetype (dispatcher symlinks) led to foundation plan and manual completion before Core implementation phases.

---

## 8. Implementation

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Implement Core per approved specs: state machine, repository, validator, search, servlet/endpoints, HTL, clientlibs, repoinit. |
| **What AI contributed** | Full `core` bundle (servlet, endpoints, `TicketRepositoryImpl`, `TicketStateMachineServiceImpl`, `UserLookupServiceImpl`, `SupportTicketsApiResourceProvider`); `ui.apps`/`ui.content`/`ui.config`; integration + unit tests alongside features. |
| **Developer review** | Spec-driven gate: AI instructed not to silently change design. Ambiguities flagged before coding. |
| **Developer decisions** | Approved implementation start; accepted open API, no pagination, no Stretch features. |
| **Validation** | Repeated `mvn -pl core test` after backend changes; deploy to Author `:4502` for servlet/UI smoke. Final: **131/131 Core tests PASS**. |

**Phased approach (AI + plan):** State machine + tests (Phase 3) → services (Phase 4) → API (Phase 5) → UI (Phases 6–9). Thin servlet + endpoint delegation matched `design-notes.md`.

---

## 9. Debugging

Three major iterative cycles. In each, **AI diagnosis required developer runtime verification** before the fix was trusted.

### 9a. Nested API 404 (`/bin/support-tickets/users.json`)

| Cycle | AI | Result |
|-------|-----|--------|
| 1 | Path servlet should cover all endpoints | **Fail** — nested URLs 404 on Author |
| 2 | `SupportTicketsApiFilter` (REQUEST forward) | **Implemented → Fail** — filter runs after resource resolution |
| 3 | `SupportTicketsApiResourceProvider` + resource-type servlet binding; remove filter | **Pass** — endpoints reachable |

**Developer input:** Reported `Resource at '/bin/support-tickets/users.json' not found`.  
**Changed recommendation:** Filter approach **abandoned** after Author test proved ineffective.

### 9b. Empty `users.json` (`[]`)

| Cycle | AI | Result |
|-------|-----|--------|
| 1 | Add repoinit ACLs for service user on `/home/users/support` | **Implemented → Fail** — still `[]` |
| 2 | `Resource.getChildren()` doesn't list Oak authorizables; use `UserManager.findAuthorizables()` + JCR-SQL2 | **Pass** — dropdown populated; `UserLookupServiceImplTest` added |

**Developer input:** Confirmed ACLs present but array still empty — challenged ACL-only diagnosis.  
**Changed recommendation:** ACL fix necessary but **insufficient**; lookup algorithm rewritten (RF-003–RF-006).

### 9c. Create ticket — "Unable to reach server"

| Cycle | AI | Result |
|-------|-----|--------|
| 1 | `headers[':cq_csrf_token']` invalid → `fetch()` throws before network | **Developer approved fix** → **Pass** |

**Developer input:** No POST in Network tab; CSRF token GET returns 200.  
**Developer decision:** *"implement 1 and 2"* — CSRF header fix (RF-001) + create redirect (RF-002) after AI asked before implementing.  
**Diagnosis verification:** Empty Network tab confirmed synchronous client-side failure, not server unreachable.

---

## 10. Testing

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Define test strategy; write JUnit 5 + AEM Mock tests; map to ACs; record execution results honestly. |
| **What AI contributed** | `test-strategy.md`; integration tests (`SupportTicketsApiIntegrationTest`, `TicketStateMachineIntegrationTest`, etc.); `@DisplayName("AC-xxx …")` traceability; `test-results.md` from Surefire output. |
| **Developer review** | Instructed not to claim passed tests without evidence; distinguish Mock vs live AEM vs Cypress. |
| **Developer decisions** | Core tier = AEM Mock integration tests in `core` module; defer live `it.tests` and Cypress. |
| **Validation** | `mvn -B -pl core test` — 131 tests, 0 failures (`2026-08-31T12:08:46+05:30`); `mvn -B clean install` — BUILD SUCCESS (`2026-08-31T12:14:30+05:30`). |

**Build/test influenced next prompt:** Test failures during RF-003 (Mockito stubbing, JCR import errors) triggered fix prompts; green suite unlocked test-results documentation.

**Limitations documented:** `UserLookupService` and `QueryBuilder` mocked in integration base (CR-008); no Cypress or live IT execution recorded.

---

## 11. Code review

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Generate formal code review artifacts from implemented code — findings only, no code changes. |
| **What AI contributed** | `code-review-notes.md` (14 findings CR-001–CR-014, 7 positives); `review-fixes.md` (RF-001–RF-006 implemented fixes + Part B proposed fixes). Task explore subagents scanned core, config, clientlibs in parallel. |
| **Developer review** | Explicit instruction: review-only, stop after artifacts. |
| **Developer decisions** | **Document only** for formal review findings — no code changes during review pass. Accepted CR-002 (no API auth) as Core scope risk. |
| **Validation** | Findings cross-checked against specs, [test-results.md](test-results.md), and session debug history. Severity-rated (0 Critical, 3 High, 6 Medium, 5 Low). |

**AI flagged its own prior mistakes:** CSRF doc drift (CR-007), Dispatcher `/bin/*` gap (CR-001), mocked UserLookup in tests (CR-008).

---

## 12. Documentation

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Produce lifecycle artifacts in dependency order; README; tool workflow; candidate info; PR description; reflection. |
| **What AI contributed** | Full spec suite (Aug 26–27); `README.md`, `tool-workflow.md`, `candidate-info.md`, `pr-description.md`, `reflection.md`, this summary (Aug 31). |
| **Developer review** | Prompts required factual grounding — no invented test results, commands, or features. |
| **Developer decisions** | Approved spec content; requested honest gaps (e.g. manual UI not in test-results). |
| **Validation** | Docs cite Surefire reports, review-fixes, transcript. Unrecorded items marked explicitly. |

**Artifact order:** requirements → ACs → design → data model → API → UI flow → implementation plan → *(code)* → test strategy/results → review → README/workflow → PR/reflection/AI summary.

---

## 13. Final validation

| Aspect | Detail |
|--------|--------|
| **What AI was asked** | Run full reactor build; document exact results; produce submission-ready artifacts. |
| **What AI contributed** | `mvn -B clean install` execution; `test-results.md` with timestamps, module table, per-class results; consistency pass across PR/reflection docs. |
| **Developer review** | Validation rules: do not claim Cypress, live IT, Dispatcher validate, or formal manual UI without evidence. |
| **Developer decisions** | Accept Core complete with documented gaps; Stretch deferred. |
| **Validation** | **Recorded:** Java 21.0.9, Maven 3.9.14, 131/131 Core tests PASS, full reactor BUILD SUCCESS. **Not recorded:** `autoInstallSinglePackage`, Cypress, live `it.tests`, Dispatcher `validate.sh`, Cloud Manager deploy. |

---

## Explicit cases: AI overridden, verified, or iteratively corrected

### Initial AI recommendation changed

| Topic | AI first | Final |
|-------|----------|-------|
| Nested `/bin` routing | Path servlet / REQUEST filter | `SupportTicketsApiResourceProvider` |
| User listing | Repoinit ACLs + `Resource.getChildren()` | ACLs + `UserManager` + JCR-SQL2 |
| CSRF header | `:cq_csrf_token` in `api.js` and docs | `CSRF-Token` only in code |
| Project layout | Nested `support-tickets/` folder | Flat repo root (developer-directed) |

### AI diagnosis required verification

| Symptom | AI hypothesis | Verification |
|---------|---------------|--------------|
| No POST in Network tab | Invalid header name breaks `fetch()` sync | Developer confirmed empty Network + console TypeError |
| `users.json` → `[]` | Missing ACLs | Developer confirmed ACLs present → hypothesis revised |
| Nested API 404 | Need request forwarding | Author retest → 404 before filter runs |

### Incremental debugging used

- **API routing:** 3 cycles (servlet → filter → ResourceProvider)
- **User visibility:** 2 cycles (ACL → UserManager)
- **Create ticket:** 1 cycle (CSRF + redirect after developer approval)

### Developer chose between alternatives

| Decision point | Choice | Mechanism |
|----------------|--------|-----------|
| UI topology | Author + Publish | AskQuestion |
| API style | Sling Servlets | AskQuestion |
| Java version | Java 21 | Developer + `.cloudmanager/java-version` |
| Debug fixes | Implement CSRF + redirect | Explicit *"implement 1 and 2"* |
| Code review | Document only, no fixes | Developer instruction |
| API auth | Accept open API for Core | Spec + CR-002 acceptance |

### Build/test results influenced next prompt

| Result | Next action |
|--------|-------------|
| Archetype partial failure | Foundation plan + manual scaffold completion |
| `mvn -pl core test` failures (RF-005, RF-006) | Fix imports and Mockito stubbing |
| Green Core tests after RF-003 | Proceed to test-results documentation |
| Author 404 after filter deploy | New prompt cycle → ResourceProvider |
| `users.json` still `[]` after ACL deploy | Deeper UserManager rewrite |
| `mvn -B clean install` SUCCESS | Formal test-results + review artifacts |

---

## Human judgment that AI could not replace

- Topology and stack choices (Author+Publish, servlets, JCR, open API)
- Rejecting implemented filter after runtime proof of failure
- Interpreting Network tab (client vs server failure)
- Directing flat repo root correction
- Approving debug fixes before code edit
- Accepting documented test gaps vs claiming unrun suites passed
- Deciding formal review = document only, not fix-in-review

---

## Summary metrics

| Metric | Value |
|--------|-------|
| Session span | Aug 26–31, 2026 (~435 transcript messages) |
| Lifecycle specs produced | 7 pre-implementation + 8+ post-implementation artifacts |
| Acceptance criteria | 67 (documented) |
| Core automated tests | 131 PASS (AEM Mock) |
| AI suggestions superseded | ≥3 major (filter, ACL-only, CSRF header) |
| Formal review findings | 14 (0 Critical) |
| Implemented debug fixes (pre-review) | RF-001–RF-006 |

**Prompt history:** Cursor transcript `13262a8d-8e65-407b-ab4b-200d6bdc9f58` — not committed as a repository file; export per assessment instructions if required.

---

## Related documents

| Document | Role |
|----------|------|
| [tool-workflow.md](tool-workflow.md) | Detailed workflow by tool and phase |
| [reflection.md](reflection.md) | First-person engineering reflection |
| [review-fixes.md](review-fixes.md) | Debug fix log (RF-001–RF-006) |
| [test-results.md](test-results.md) | Objective test execution evidence |
| [code-review-notes.md](code-review-notes.md) | Post-implementation static review |
