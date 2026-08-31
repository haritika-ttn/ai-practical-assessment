# Candidate Information — Support Ticket Management System

**Assessment:** AI Practical Assessment  
**Document type:** Candidate / submission details  
**Document version:** 1.0  
**Last updated:** 2026-08-31

---

## Purpose

This file identifies the candidate and summarizes the submission for reviewers. It is a required lifecycle artifact per [requirements-analysis.md](requirements-analysis.md) repository structure.

Personal identification fields must be completed by the candidate before final submission. Project and technical details below are drawn from the repository and recorded build/test evidence.

---

## 1. Candidate identification

| Field | Value |
|-------|-------|
| **Full name** | [To be completed by candidate] |
| **Email** | [To be completed by candidate] |
| **Employee / candidate ID** | [To be completed by candidate] |
| **Organization** | [To be completed by candidate] |
| **Role / title** | [To be completed by candidate] |
| **Submission date** | [To be completed by candidate] |
| **Assessment cohort / batch** | [To be completed by candidate] |

> **Note:** No candidate name, email, or ID was found in repository files or git history at the time this document was generated. [Git repository: not initialized in project workspace]

---

## 2. Submission summary

| Field | Value |
|-------|-------|
| **Project title** | Support Ticket Management System |
| **Assessment option** | AEM as a Cloud Service (JCR persistence) |
| **Platform** | Adobe Experience Manager as a Cloud Service (AEMaaCS) |
| **Maven coordinates** | `com.supporttickets:support-tickets:1.0.0-SNAPSHOT` |
| **Repository path (local)** | `d:\ai-practical-assessment` |
| **Repository URL (remote)** | [To be completed by candidate] |
| **Scope delivered** | **Core** mandatory requirements; **Stretch** deferred |
| **Primary AI tool** | Cursor IDE (Agent mode) |
| **AI-assisted workflow** | Spec-driven; documented in [tool-workflow.md](tool-workflow.md) |

### Assignment option — database

| Requirement | How addressed |
|-------------|---------------|
| Database persistence | **JCR (Oak)** — tickets and comments under `/content/support-tickets/tickets` |
| Setup / migration scripts | **Repoinit** in `ui.config/.../RepositoryInitializer~supporttickets.cfg.json` |
| Seed / sample data | Repoinit seeded users (`agent1`, `agent2`, `supervisor1`); optional content in `ui.content` |

---

## 3. Key implementation decisions (developer-approved)

These choices were made during planning and recorded in lifecycle specs:

| Decision | Choice | Source |
|----------|--------|--------|
| UI topology | Author **and** Publish (+ Dispatcher) | Developer selection via planning Q&A (Aug 26, 2026) |
| API style | Sling Servlets JSON API at `/bin/support-tickets` | Developer selection via planning Q&A |
| Persistence | JCR nodes (not external RDBMS) | [requirements-analysis.md](requirements-analysis.md), [data-model.md](data-model.md) |
| Authentication | None in Core (open API by design) | [api-contract.md](api-contract.md) |
| Status changes | `PATCH .../status.json` only; state machine in backend | [design-notes.md](design-notes.md) §18 |
| Test tier (Core) | JUnit 5 + AEM Mock integration tests (`core` module) | [test-strategy.md](test-strategy.md) |

---

## 4. Development timeline

Dates derived from Cursor agent session transcript and [test-results.md](test-results.md):

| Phase | Approximate dates | Milestone |
|-------|-------------------|-----------|
| Requirements & planning | 2026-08-26 | `requirements-analysis.md`, `acceptance-criteria.md`, design specs |
| AEM scaffold & build setup | 2026-08-27 | Archetype v57, project root relocation, Java 21 validation |
| Core implementation | 2026-08-27 – 2026-08-30 | Backend, UI, repoinit, live AEM debugging |
| Test documentation | 2026-08-31 | `test-strategy.md`, `test-results.md` |
| Code review | 2026-08-31 | `code-review-notes.md`, `review-fixes.md` |
| README & workflow docs | 2026-08-31 | `README.md`, `tool-workflow.md`, this file |

**Prompt history:** Cursor agent transcript `13262a8d-8e65-407b-ab4b-200d6bdc9f58` (Aug 26–31, 2026). [Not committed as a file in this repository — export per assessment instructions if required]

---

## 5. Environment used for validation

From [test-results.md](test-results.md) (recorded execution on 2026-08-31):

| Component | Version / detail |
|-----------|------------------|
| **OS** | Windows 11 (`os.arch=amd64`) |
| **Java** | 21.0.9 (Oracle JDK) |
| **Maven** | 3.9.14 |
| **AEM SDK API (dependency)** | `2026.8.27673.20260811T193135Z-260700` |
| **Cloud Manager Java** | 21 (`.cloudmanager/java-version`) |
| **JUnit** | 5.8.2 |
| **AEM Mock** | io.wcm.testing.aem-mock 5.5.4 |

### Build / test results (recorded)

| Command | Result |
|---------|--------|
| `mvn -B clean install` | **BUILD SUCCESS** (2026-08-31T12:14:30+05:30) |
| `mvn -B -pl core test` | **131 tests, 0 failures** (2026-08-31T12:08:46+05:30) |

**Local AEM Quickstart version:** [Not recorded in project artifacts]  
**Manual UI testing on Author `:4502`:** Performed during development (see [review-fixes.md](review-fixes.md) RF-001–RF-003); formal results not in `test-results.md`

---

## 6. Deliverables checklist

### Application

| Deliverable | Status | Location |
|-------------|--------|----------|
| Frontend application | Implemented | `ui.apps`, `ui.content` — `/content/support-app` |
| Backend API (7 endpoints) | Implemented | `core` — `/bin/support-tickets` |
| JCR persistence | Implemented | `/content/support-tickets/tickets` |
| Repoinit / seed data | Implemented | `ui.config` repoinit |
| Input validation | Implemented | `TicketValidatorImpl` |
| Error handling (API + UI) | Implemented | `ApiErrorMapper`, `utils.js` |
| Search + status filter | Implemented | `TicketSearchServiceImpl` |
| Automated tests (Core tier) | **131 tests PASS** | `core/src/test` |

### Lifecycle artifacts

| Artifact | Status |
|----------|--------|
| [README.md](README.md) | Complete |
| [requirements-analysis.md](requirements-analysis.md) | Complete |
| [acceptance-criteria.md](acceptance-criteria.md) | Complete (67 ACs) |
| [design-notes.md](design-notes.md) | Complete |
| [data-model.md](data-model.md) | Complete |
| [api-contract.md](api-contract.md) | Complete |
| [ui-flow.md](ui-flow.md) | Complete |
| [implementation-plan.md](implementation-plan.md) | Complete |
| [test-strategy.md](test-strategy.md) | Complete |
| [test-results.md](test-results.md) | Complete |
| [code-review-notes.md](code-review-notes.md) | Complete |
| [review-fixes.md](review-fixes.md) | Complete |
| [tool-workflow.md](tool-workflow.md) | Complete |
| **candidate-info.md** | This document |
| [reflection.md](reflection.md) | [Not present in repository] |
| [final-ai-usage-summary.md](final-ai-usage-summary.md) | [Not present in repository] |
| [debugging-notes.md](debugging-notes.md) | [Not present in repository — see review-fixes.md Part A] |
| [pr-description.md](pr-description.md) | [Not present in repository] |
| `ai-prompts/*.md` | [Not present in repository] |

---

## 7. AI usage declaration

| Field | Detail |
|-------|--------|
| **AI tool(s) used** | Cursor IDE (Agent, Plan mode, Shell, code exploration) |
| **How AI was used** | Requirements decomposition, spec authoring, Java/JS/config implementation, debugging assistance, test and review documentation |
| **Human oversight** | Developer approved specs, directed project structure fixes, approved debug fixes, validated on local AEM Author |
| **Workflow documentation** | [tool-workflow.md](tool-workflow.md) |
| **Review of AI output** | Formal static review in [code-review-notes.md](code-review-notes.md) |

The candidate confirms that AI-generated content was reviewed and validated before inclusion in this submission. [Candidate signature / confirmation: to be completed by candidate]

---

## 8. Known submission gaps (honest disclosure)

Items identified in project artifacts that are incomplete or not executed:

| Item | Status |
|------|--------|
| Stretch features (auth, user CRUD, pagination, etc.) | Not implemented |
| Live AEM `it.tests` execution | Not recorded |
| Cypress UI test execution | Not recorded |
| Dispatcher `validate.sh` | Not recorded |
| Code coverage metrics | Not measured |
| `reflection.md`, `ai-prompts/`, `pr-description.md` | Not in repository |
| Dispatcher `/bin/support-tickets` allow filter | Not implemented ([CR-001](code-review-notes.md)) |

---

## 9. Reviewer quick reference

| Resource | Path |
|----------|------|
| Setup & run | [README.md](README.md) |
| API contract | [api-contract.md](api-contract.md) |
| Test evidence | [test-results.md](test-results.md) |
| Code review | [code-review-notes.md](code-review-notes.md) |
| Fixes applied | [review-fixes.md](review-fixes.md) |
| AI workflow | [tool-workflow.md](tool-workflow.md) |

**Entry points after deploy to Author:**

- UI list: `http://localhost:4502/content/support-app.html`
- API: `http://localhost:4502/bin/support-tickets.json`

---

## 10. Document history

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-08-31 | Initial population from repository artifacts and session transcript |
