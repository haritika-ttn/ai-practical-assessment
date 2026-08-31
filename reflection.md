# Engineering Reflection — Support Ticket Management System

**Project:** AI Practical Assessment (AEMaaCS)  
**Author:** Candidate reflection  
**Date:** 2026-08-31  
**Sources:** Cursor session transcript `13262a8d-8e65-407b-ab4b-200d6bdc9f58`, [review-fixes.md](review-fixes.md), [tool-workflow.md](tool-workflow.md), [test-results.md](test-results.md)

---

## 1. What went well

The spec-driven workflow paid off. I produced and approved planning artifacts — requirements analysis, 67 acceptance criteria, API contract, data model, UI flow, and implementation plan — before writing application code. That gave me a stable scope boundary and traceability from ACs to integration tests.

The backend layering stayed clean: a thin servlet, dedicated endpoint classes, a single repository writer (`TicketRepositoryImpl`), and a state machine service enforced at the persistence boundary. I invested early in state machine tests — 31 unit tests plus 17 integration tests — which gave me confidence that invalid transitions would return HTTP 409 as specified.

By the end of the build cycle, **131 Core module tests passed** with zero failures ([test-results.md](test-results.md)). The lifecycle documentation set (README, code review, review fixes, tool workflow) is complete enough that a reviewer can understand what was built, what was tested, and what was not.

---

## 2. What was difficult

Several problems only surfaced at runtime on AEM Author, not in unit or integration tests.

**Nested `/bin` servlet routing** was the first major blocker. `GET /bin/support-tickets/users.json` returned 404 even though the list endpoint at `/bin/support-tickets.json` worked. I initially tried a REQUEST filter to forward nested paths, but Sling resolves resources before REQUEST filters run — so the filter never helped. The fix was `SupportTicketsApiResourceProvider`, which supplies synthetic resources for nested API paths.

**Empty `users.json`** was misleading. I added repoinit ACLs for the service user on `/home/users/support`, which was necessary but not sufficient. `UserLookupServiceImpl` was traversing `Resource.getChildren()`, but Oak authorizable folders do not expose users as child resources. Switching to `UserManager.findAuthorizables()` plus a JCR-SQL2 fallback finally populated the dropdown.

**Create ticket "Unable to reach server"** was confusing because the Network tab showed no POST at all. The root cause was `headers[':cq_csrf_token']` in `api.js` — a colon-prefixed header name that `fetch()` rejects synchronously. The Granite CSRF token endpoint returned 200, which made the failure harder to diagnose.

Other friction: aligning Java 21 for builds when PATH defaulted to an older JDK, and relocating the project from a nested `support-tickets/` folder to a flat repo root after I realized the assessment expected artifacts at the top level.

---

## 3. Where AI was useful

AI accelerated work I would have done manually but much more slowly:

- Decomposing the assessment brief into structured requirements and **67 acceptance criteria** with IDs traceable to tests
- Generating boilerplate Java (servlet, endpoints, DTOs, OSGi services) and matching JUnit/AEM Mock integration tests
- Writing repoinit scripts for content paths, seeded users, and service-user ACLs
- Producing lifecycle markdown (README, API contract, test strategy, code review notes)
- Tracing the nested-API 404 and CSRF failures systematically once I described symptoms

The AI also helped maintain consistency across documents — for example, keeping the state machine rules aligned between `api-contract.md`, `TicketStateMachineServiceImpl`, and integration tests.

---

## 4. Where AI needed correction

AI suggestions were not always right on the first attempt.

The **filter-based routing** approach was AI-recommended and I implemented it, but Author testing proved it ineffective. I had to abandon `SupportTicketsApiFilter` and adopt a ResourceProvider pattern instead.

For **user listing**, AI initially treated ACL grants as the likely fix. ACLs were required, but the real bug was the lookup strategy — `Resource.getChildren()` on authorizable paths.

**CSRF header naming** appeared in both generated code and documentation as `:cq_csrf_token`. That is not a valid HTTP header name for `fetch()`. The correct Granite header is `CSRF-Token`. I fixed the code (RF-001) but the planning docs still drift (CR-007).

Early on, AI may have over-relied on path servlet registration alone for all seven endpoints without accounting for Sling's nested `/bin` resolution model.

---

## 5. Debugging lessons

**When `fetch()` throws before the network layer, the Network tab shows nothing.** I spent time checking AEM logs and CSRF token availability when the problem was a synchronous `TypeError` from an invalid header name. Checking the browser console first would have saved time.

**REQUEST filters run after resource resolution in Sling.** A filter cannot fix a 404 caused by a missing resource. For custom nested paths under `/bin`, a ResourceProvider (or equivalent resolution hook) is the correct tool.

**Service-user ACLs and Oak authorizable APIs are separate concerns.** Granting `jcr:read` on `/home/users/support` does not make `Resource.getChildren()` return user nodes. I learned to verify ACLs and API choice independently.

**Iterative deploy-and-test on Author** remained essential. AEM Mock caught business logic and servlet wiring well, but not repoinit timing, Granite CSRF behavior, or authorizable folder semantics.

---

## 6. AEM-specific lessons

**Service users and repoinit** are foundational. The `support-tickets-service` user, its mapper configuration, and ACLs on `/content/support-tickets` and `/home/users` must be in place before the API can persist or list users.

**Synthetic resources via ResourceProvider** solved nested `/bin/support-tickets/{id}/status.json` routing where a single path servlet registration could not.

**UserManager vs JCR resource traversal:** seeded users live as Oak authorizables under `/home/users/support/`. Listing them requires `UserManager` or JCR-SQL2, not folder children.

**Granite CSRF** expects the `CSRF-Token` request header on mutating calls from the browser. Documentation examples using `:cq_csrf_token` are misleading for modern `fetch()` usage.

**Author + Publish topology** (my choice) means Dispatcher configuration matters for Publish — the default archetype Dispatcher rules may block `/bin/support-tickets` (CR-001), which I documented but did not fix in this PR.

---

## 7. Architectural decisions I made

When the AI asked for topology and stack choices, I decided:

| Decision | Choice | Rationale |
|----------|--------|-----------|
| UI deployment | **Author + Publish** | Matches realistic dual-surface AEM deployments |
| API style | **Sling Servlets** (not Sling Models-as-API) | Clear REST boundary, familiar AEM pattern |
| Persistence | **JCR nodes** (not RDBMS or external DB) | Native AEM content repository; fits assessment |
| Status changes | **PATCH-only** via `/status.json` | Separates field updates from workflow transitions |
| API security | **Open endpoints** for Core scope | Accepted assessment risk; documented as CR-002 |
| Repository layout | **Flat repo root** | Corrected nested folder structure per assessment expectations |
| Build JDK | **Java 21** | Matches `.cloudmanager/java-version` |

These choices shaped the implementation plan and what "done" meant for Core versus Stretch.

---

## 8. How I validated AI work

I did not trust generated code without verification:

- **`mvn -B -pl core test`** after significant backend changes — final result: 131 tests, 0 failures
- **`mvn -B clean install`** for full reactor validation — BUILD SUCCESS on 2026-08-31T12:14:30+05:30
- **Manual smoke on AEM Author `:4502`** for servlet routing, users.json, CSRF, and create flow
- **Developer approval gates** — when AI proposed CSRF and redirect fixes, I explicitly approved ("implement 1 and 2") before changes landed
- **Formal code review** — produced [code-review-notes.md](code-review-notes.md) with 14 findings to catch gaps AI and I missed

What I did **not** validate in recorded artifacts: live `it.tests`, Cypress UI tests, Dispatcher `validate.sh`, or a formal manual UI test log ([test-results.md](test-results.md) §4).

---

## 9. Mistakes / inefficiencies

**Filter-first for nested API paths** cost a deploy-debug cycle. ResourceProvider should have been the first approach for nested `/bin` URLs.

**Initial user folder traversal** assumed JCR resources mirror a simple folder listing. That wasted time on ACL tuning when the lookup algorithm was wrong.

**Documentation lagged behind the CSRF fix.** `api-contract.md` and related docs still reference `:cq_csrf_token` while `api.js` correctly sends `CSRF-Token` — a review finding I have not yet fixed.

**Integration tests mock `UserLookupService`** in the shared base class, so the real `UserLookupServiceImpl` path is only covered by two unit tests, not full API integration tests (CR-008).

**Archetype boilerplate** (HelloWorld, sample scheduled tasks) remains in the bundle — noise for reviewers but low priority during the assessment timeline.

---

## 10. What I'd do differently

1. **Start with ResourceProvider** for any nested `/bin/...` API paths instead of experimenting with filters.
2. **Use real `UserLookupService` in integration tests** (or a dedicated IT) rather than mocking it in the base class.
3. **Add Dispatcher allow rules during implementation**, not as a post-review finding — especially with Author+Publish UI topology.
4. **Update `api-contract.md` in the same commit** as the CSRF header fix to prevent doc drift.
5. **Record manual UI test results** in `test-results.md` so CSRF and redirect fixes have artifact-level evidence, not just session history.

---

## 11. Spec-driven workflow effect

Writing specs first slowed the start but reduced scope creep. When AI suggested Stretch features (auth, pagination, OpenAPI), I could point to [requirements-analysis.md](requirements-analysis.md) §2.2 and defer them.

Acceptance criteria IDs (e.g. AC-040, AC-050) mapped directly to integration test method names like `ac040_validTransitionReturns200` and `ac050_invalidTransitionReturns409`, which made gaps visible during test planning.

The trade-off: specs can become stale (CSRF header documentation) if not updated when implementation diverges. Treating docs as part of the deliverable, not a one-time planning step, would help.

---

## 12. How AI changed my workflow

AI compressed spec writing and boilerplate implementation from days to hours. Plan mode and structured questions (Author vs Publish, servlet vs models) forced explicit decisions early instead of implicit defaults.

Debugging was faster when I described symptoms precisely — empty Network tab, 404 on nested path, empty JSON array despite repoinit users. AI could propose hypotheses in sequence (filter → ResourceProvider; ACL → UserManager).

AI did **not** replace AEM runtime. Repoinit, CSRF filters, authorizable semantics, and Dispatcher rules required a running Quickstart. The workflow became: AI generates → I deploy → I observe → I correct → AI helps with the next iteration.

---

## 13. Human judgment still required

Several decisions could not be delegated:

- **Topology and stack** — Author+Publish, Sling Servlets, JCR persistence, open API for Core
- **Rejecting the filter approach** after Author proved it failed, despite it being already implemented
- **Interpreting the Network tab** — no POST means client-side failure, not server unreachable
- **Accepting CR-002** (no API auth) as an assessment-scope risk with documentation, not a last-minute security retrofit
- **Correcting project root layout** when the nested folder did not match submission expectations
- **Approving debug fixes** explicitly before AI edited `api.js` and `create.js`
- **Deciding what "done" means** — 131 Core tests pass, lifecycle docs complete, but live IT/Cypress explicitly out of scope for evidenced validation

AI accelerated execution; I remained responsible for architecture, platform-specific correctness, and honest reporting of what was and was not tested.
