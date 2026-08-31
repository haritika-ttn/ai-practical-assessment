# Acceptance Criteria — Support Ticket Management System

**Project:** AI Practical Assessment  
**Platform:** AEM as a Cloud Service (AEMaaCS)  
**Source document:** [requirements-analysis.md](requirements-analysis.md)  
**Document version:** 1.0  
**Status:** Approved for Core verification

---

## Purpose

This document converts the approved requirements analysis into a **precise, testable acceptance-criteria matrix**. Each criterion is independently verifiable and traceable to original assignment requirements.

### Conventions

| Field | Description |
|-------|-------------|
| **ID** | Unique acceptance criterion identifier (`AC-xxx`) |
| **Requirement** | Source requirement reference(s) |
| **Preconditions** | Environment and data state before the action |
| **Action** | Steps performed by tester or automated test |
| **Expected result** | Observable outcome that must be true |
| **HTTP/API expectation** | Applicable API contract; `N/A` when not API-tested |
| **UI expectation** | Applicable UI behaviour; `N/A` when not UI-tested |
| **Test type** | `Manual`, `Integration`, `Automated`, `Code review`, `Setup` |
| **Priority** | `P0` = Core mandatory; `P1` = Core quality gate (recommended) |

### Approved defaults (from requirements analysis)

- API base path: `/bin/support-tickets`
- Status values: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`
- Priority values: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- Keyword search scope: **title and description** (case-insensitive)
- New tickets always created with status `OPEN`
- `status` changed only via `PATCH /bin/support-tickets/{id}/status.json`
- Invalid transitions return **409 Conflict**
- Seeded users referenced by AEM user path (e.g. `/home/users/support/agent1`)

---

## Traceability Index

| Assignment area | Acceptance criteria IDs |
|-----------------|-------------------------|
| Create ticket | AC-001 – AC-007 |
| List tickets | AC-010 – AC-011 |
| View ticket detail | AC-020 – AC-022 |
| Update ticket fields | AC-030 – AC-034 |
| State machine — valid transitions | AC-040 – AC-044 |
| State machine — invalid transitions | AC-050 – AC-057 |
| Comments | AC-060 – AC-063 |
| Keyword search | AC-070 – AC-073 |
| Status filter | AC-080 – AC-082 |
| Persistence | AC-090 – AC-092 |
| Backend validation | AC-100 – AC-104 |
| Error handling (UI) | AC-110 – AC-111 |
| User entity (seeded) | AC-120 – AC-121 |
| Common technical requirements | AC-130 – AC-140 |
| AEM topology | AC-150 – AC-152 |
| Mandatory test tier | AC-160 – AC-162 |

---

## 1. Ticket Creation

### AC-001 — Create ticket via UI

| Field | Value |
|-------|-------|
| **Requirement** | FR-C01, NFR-C01; Core acceptance: "A user can create a ticket via the UI" |
| **Preconditions** | AEM Author or Publish UI is running; at least one seeded user exists; ticket list page is accessible |
| **Action** | Open create-ticket form; enter title, description, priority; select `createdBy` (acting-as user); optionally select assignee; submit |
| **Expected result** | Ticket is created with status `OPEN`; user is redirected to detail or list showing the new ticket |
| **HTTP/API expectation** | `POST /bin/support-tickets.json` returns `201 Created` with body containing `id`, `title`, `status: "OPEN"`, `createdBy`, `createdAt` |
| **UI expectation** | Success feedback shown; new ticket visible in list without page hard-refresh tricks (normal navigation or list refresh) |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-002 — Create ticket via API with valid payload

| Field | Value |
|-------|-------|
| **Requirement** | FR-C01, NFR-C02 |
| **Preconditions** | Backend API deployed; seeded user path known |
| **Action** | `POST /bin/support-tickets.json` with `{ "title": "Login issue", "description": "Cannot reset password", "priority": "HIGH", "createdBy": "/home/users/support/agent1" }` |
| **Expected result** | Ticket persisted in JCR with generated UUID `id`, status `OPEN`, timestamps set |
| **HTTP/API expectation** | `201 Created`; `Content-Type: application/json`; response includes all submitted fields plus `id`, `status`, `createdAt`, `updatedAt` |
| **UI expectation** | N/A |
| **Test type** | Manual or Integration |
| **Priority** | P0 |

### AC-003 — Reject create when required field `title` is missing

| Field | Value |
|-------|-------|
| **Requirement** | FR-C10, NFR-C04; SEC-C02 |
| **Preconditions** | Backend API deployed |
| **Action** | `POST /bin/support-tickets.json` with `{ "description": "No title", "priority": "LOW", "createdBy": "/home/users/support/agent1" }` |
| **Expected result** | No ticket node created in JCR |
| **HTTP/API expectation** | `400 Bad Request`; body `{ "code": "VALIDATION_ERROR", "message": "...", "fields": { "title": "..." } }` (or equivalent structured error) |
| **UI expectation** | Form shows inline/ banner error identifying missing title; ticket not created |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-004 — Reject create with invalid `priority` enum

| Field | Value |
|-------|-------|
| **Requirement** | FR-C10, NFR-C04 |
| **Preconditions** | Backend API deployed |
| **Action** | `POST /bin/support-tickets.json` with `"priority": "URGENT"` (not in allowed enum) |
| **Expected result** | No ticket created |
| **HTTP/API expectation** | `400 Bad Request`; field error on `priority` |
| **UI expectation** | Error shown; submission blocked or rejected with visible message |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-005 — Reject create with unknown `createdBy` user reference

| Field | Value |
|-------|-------|
| **Requirement** | FR-C10, FR-C09, NFR-C04 |
| **Preconditions** | Backend API deployed; seeded users installed |
| **Action** | `POST /bin/support-tickets.json` with `"createdBy": "/home/users/support/nonexistent"` |
| **Expected result** | No ticket created |
| **HTTP/API expectation** | `400 Bad Request`; field error on `createdBy` |
| **UI expectation** | Error shown if UI allows free-text user entry; otherwise only seeded users selectable |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-006 — Force initial status to OPEN on create

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, FR-C01 |
| **Preconditions** | Backend API deployed |
| **Action** | `POST /bin/support-tickets.json` including `"status": "CLOSED"` in request body |
| **Expected result** | Ticket created with `status: "OPEN"` regardless of submitted status |
| **HTTP/API expectation** | `201 Created`; response `status` is `"OPEN"` |
| **UI expectation** | New ticket detail/list shows status Open |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-007 — `createdBy` is immutable after create

| Field | Value |
|-------|-------|
| **Requirement** | FR-C04, FR-C10 (audit integrity) |
| **Preconditions** | Ticket exists with known `createdBy` |
| **Action** | `PUT /bin/support-tickets/{id}.json` with `{ "createdBy": "/home/users/support/other-user", "title": "Updated" }` |
| **Expected result** | `createdBy` unchanged; title updated if otherwise valid |
| **HTTP/API expectation** | `200 OK`; response `createdBy` equals original value; or `400` if `createdBy` in body is explicitly rejected |
| **UI expectation** | Creator field read-only or absent from edit form |
| **Test type** | Manual |
| **Priority** | P1 |

---

## 2. List Tickets

### AC-010 — List all tickets from persistent storage

| Field | Value |
|-------|-------|
| **Requirement** | FR-C02, FR-C11; Core acceptance: "A user can view all tickets from the database" |
| **Preconditions** | Seed data and/or manually created tickets exist in JCR; UI list page accessible |
| **Action** | Open ticket list page with no search or filter applied |
| **Expected result** | All persisted tickets displayed; count matches JCR/API |
| **HTTP/API expectation** | `GET /bin/support-tickets.json` returns `200 OK` with JSON array containing all tickets |
| **UI expectation** | List renders title, status, priority, assignee (if set), and key metadata for each ticket |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-011 — Newly created ticket appears in list

| Field | Value |
|-------|-------|
| **Requirement** | FR-C01, FR-C02 |
| **Preconditions** | Known ticket count before create |
| **Action** | Create ticket (AC-001 or AC-002); return to list |
| **Expected result** | List count increases by one; new ticket row present |
| **HTTP/API expectation** | `GET /bin/support-tickets.json` includes new ticket `id` |
| **UI expectation** | New ticket visible in list |
| **Test type** | Manual |
| **Priority** | P0 |

---

## 3. View Ticket Detail

### AC-020 — View ticket detail

| Field | Value |
|-------|-------|
| **Requirement** | FR-C03; Core acceptance: "A user can open a ticket detail view" |
| **Preconditions** | Ticket with known `id` exists |
| **Action** | Navigate to ticket detail from list or direct URL |
| **Expected result** | Detail shows `id`, `title`, `description`, `priority`, `status`, `assignedTo`, `createdBy`, `createdAt`, `updatedAt` |
| **HTTP/API expectation** | `GET /bin/support-tickets/{id}.json` returns `200 OK` with full ticket object |
| **UI expectation** | All ticket fields rendered on detail page |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-021 — Detail includes comments

| Field | Value |
|-------|-------|
| **Requirement** | FR-C03, FR-C06 |
| **Preconditions** | Ticket exists with at least one comment |
| **Action** | Open ticket detail |
| **Expected result** | Comments listed with `message`, `createdBy`, `createdAt` in chronological order |
| **HTTP/API expectation** | `GET /bin/support-tickets/{id}.json` includes `comments` array |
| **UI expectation** | Comment thread visible on detail page |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-022 — Detail exposes allowed status transitions

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05 (UX support for state machine) |
| **Preconditions** | Ticket in status `OPEN` |
| **Action** | `GET /bin/support-tickets/{id}.json` |
| **Expected result** | Response includes `allowedTransitions` containing only valid next states |
| **HTTP/API expectation** | For `OPEN` ticket: `allowedTransitions: ["IN_PROGRESS", "CANCELLED"]` |
| **UI expectation** | Status change control offers only `allowedTransitions` options |
| **Test type** | Manual |
| **Priority** | P1 |

### AC-023 — Return 404 for unknown ticket ID

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C05 (error handling) |
| **Preconditions** | ID does not exist in JCR |
| **Action** | `GET /bin/support-tickets/00000000-0000-0000-0000-000000000000.json` |
| **Expected result** | Not found error returned |
| **HTTP/API expectation** | `404 Not Found`; `{ "code": "NOT_FOUND", "message": "..." }` |
| **UI expectation** | User-friendly "ticket not found" message; no unhandled exception page |
| **Test type** | Manual |
| **Priority** | P0 |

---

## 4. Update Ticket Fields

### AC-030 — Update title via UI and API

| Field | Value |
|-------|-------|
| **Requirement** | FR-C04; Core acceptance: "A user can update ticket fields" |
| **Preconditions** | Ticket exists and is not in a read-only terminal state for field edits (Core: all statuses editable except none mandated terminal-lock) |
| **Action** | Edit title on detail page and save; or `PUT /bin/support-tickets/{id}.json` with `{ "title": "New title" }` |
| **Expected result** | Title updated in JCR; `updatedAt` refreshed |
| **HTTP/API expectation** | `200 OK`; response reflects new title |
| **UI expectation** | Updated title shown on detail and list after save |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-031 — Update description

| Field | Value |
|-------|-------|
| **Requirement** | FR-C04 |
| **Preconditions** | Ticket exists |
| **Action** | `PUT /bin/support-tickets/{id}.json` with `{ "description": "Updated description" }` |
| **Expected result** | Description persisted |
| **HTTP/API expectation** | `200 OK` |
| **UI expectation** | Updated description on detail page |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-032 — Update priority

| Field | Value |
|-------|-------|
| **Requirement** | FR-C04 |
| **Preconditions** | Ticket exists |
| **Action** | `PUT /bin/support-tickets/{id}.json` with `{ "priority": "CRITICAL" }` |
| **Expected result** | Priority updated |
| **HTTP/API expectation** | `200 OK`; `priority: "CRITICAL"` |
| **UI expectation** | Priority displayed as Critical on detail/list |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-033 — Reassign ticket (update assignee)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C04; Core acceptance: "reassign" |
| **Preconditions** | Ticket exists; second seeded user exists |
| **Action** | `PUT /bin/support-tickets/{id}.json` with `{ "assignedTo": "/home/users/support/agent2" }` |
| **Expected result** | Assignee updated in JCR |
| **HTTP/API expectation** | `200 OK`; `assignedTo` reflects new user path |
| **UI expectation** | Assignee shown on detail and list |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-034 — Reject `status` change via PUT (state machine isolation)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, SEC-C03 |
| **Preconditions** | Ticket in status `OPEN` |
| **Action** | `PUT /bin/support-tickets/{id}.json` with `{ "status": "IN_PROGRESS" }` |
| **Expected result** | Status remains `OPEN`; no bypass of state machine |
| **HTTP/API expectation** | `400 Bad Request` (field rejected) or `200 OK` with `status` unchanged and field ignored — **must not** transition status via PUT |
| **UI expectation** | Status edit not available on general edit form; only dedicated status control |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

---

## 5. State Machine — Valid Transitions

> Each valid transition must succeed via `PATCH /bin/support-tickets/{id}/status.json` with body `{ "status": "<TARGET>" }` and be covered by integration tests.

### AC-040 — OPEN → IN_PROGRESS

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C02 |
| **Preconditions** | Ticket exists with `status: "OPEN"` |
| **Action** | `PATCH /bin/support-tickets/{id}/status.json` `{ "status": "IN_PROGRESS" }` |
| **Expected result** | Status becomes `IN_PROGRESS`; `updatedAt` changed |
| **HTTP/API expectation** | `200 OK`; `{ "status": "IN_PROGRESS", "allowedTransitions": ["RESOLVED", "CANCELLED"] }` |
| **UI expectation** | Detail and list show In Progress |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

### AC-041 — IN_PROGRESS → RESOLVED

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C02 |
| **Preconditions** | Ticket in `IN_PROGRESS` |
| **Action** | `PATCH .../status.json` `{ "status": "RESOLVED" }` |
| **Expected result** | Status becomes `RESOLVED` |
| **HTTP/API expectation** | `200 OK`; `allowedTransitions: ["CLOSED"]` |
| **UI expectation** | Status shows Resolved |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

### AC-042 — RESOLVED → CLOSED

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C02 |
| **Preconditions** | Ticket in `RESOLVED` |
| **Action** | `PATCH .../status.json` `{ "status": "CLOSED" }` |
| **Expected result** | Status becomes `CLOSED` (terminal) |
| **HTTP/API expectation** | `200 OK`; `allowedTransitions: []` |
| **UI expectation** | Status shows Closed; no further transitions offered |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

### AC-043 — OPEN → CANCELLED

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C02 |
| **Preconditions** | Ticket in `OPEN` |
| **Action** | `PATCH .../status.json` `{ "status": "CANCELLED" }` |
| **Expected result** | Status becomes `CANCELLED` (terminal) |
| **HTTP/API expectation** | `200 OK`; `allowedTransitions: []` |
| **UI expectation** | Status shows Cancelled |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

### AC-044 — IN_PROGRESS → CANCELLED

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C02 |
| **Preconditions** | Ticket in `IN_PROGRESS` |
| **Action** | `PATCH .../status.json` `{ "status": "CANCELLED" }` |
| **Expected result** | Status becomes `CANCELLED` |
| **HTTP/API expectation** | `200 OK`; `allowedTransitions: []` |
| **UI expectation** | Status shows Cancelled |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

---

## 6. State Machine — Invalid Transitions (Representative Set)

> Each invalid transition must be **rejected by the backend**; status unchanged in JCR. Core acceptance: "invalid ones are rejected."

### AC-050 — Reject OPEN → RESOLVED (skip-level)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C03, SEC-C03 |
| **Preconditions** | Ticket in `OPEN` |
| **Action** | `PATCH .../status.json` `{ "status": "RESOLVED" }` |
| **Expected result** | Status remains `OPEN` |
| **HTTP/API expectation** | `409 Conflict`; `{ "code": "INVALID_TRANSITION", "currentStatus": "OPEN", "requestedStatus": "RESOLVED" }` |
| **UI expectation** | Error message shown; status unchanged on screen |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

### AC-051 — Reject OPEN → CLOSED (skip-level)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C03 |
| **Preconditions** | Ticket in `OPEN` |
| **Action** | `PATCH .../status.json` `{ "status": "CLOSED" }` |
| **Expected result** | Status remains `OPEN` |
| **HTTP/API expectation** | `409 Conflict` |
| **UI expectation** | Error shown; status unchanged |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

### AC-052 — Reject IN_PROGRESS → OPEN (backward)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C03 |
| **Preconditions** | Ticket in `IN_PROGRESS` |
| **Action** | `PATCH .../status.json` `{ "status": "OPEN" }` |
| **Expected result** | Status remains `IN_PROGRESS` |
| **HTTP/API expectation** | `409 Conflict` |
| **UI expectation** | OPEN not offered; if forced via API, UI reflects unchanged status after refresh |
| **Test type** | Integration (Automated) |
| **Priority** | P0 |

### AC-053 — Reject RESOLVED → IN_PROGRESS (backward)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C03 |
| **Preconditions** | Ticket in `RESOLVED` |
| **Action** | `PATCH .../status.json` `{ "status": "IN_PROGRESS" }` |
| **Expected result** | Status remains `RESOLVED` |
| **HTTP/API expectation** | `409 Conflict` |
| **UI expectation** | N/A |
| **Test type** | Integration (Automated) |
| **Priority** | P0 |

### AC-054 — Reject RESOLVED → CANCELLED

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C03 |
| **Preconditions** | Ticket in `RESOLVED` |
| **Action** | `PATCH .../status.json` `{ "status": "CANCELLED" }` |
| **Expected result** | Status remains `RESOLVED` |
| **HTTP/API expectation** | `409 Conflict` |
| **UI expectation** | N/A |
| **Test type** | Integration (Automated) |
| **Priority** | P0 |

### AC-055 — Reject any transition from CLOSED (terminal)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C03 |
| **Preconditions** | Ticket in `CLOSED` |
| **Action** | `PATCH .../status.json` `{ "status": "OPEN" }` |
| **Expected result** | Status remains `CLOSED` |
| **HTTP/API expectation** | `409 Conflict`; `allowedTransitions: []` on prior GET |
| **UI expectation** | No status change controls available |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

### AC-056 — Reject any transition from CANCELLED (terminal)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, TEST-C03 |
| **Preconditions** | Ticket in `CANCELLED` |
| **Action** | `PATCH .../status.json` `{ "status": "IN_PROGRESS" }` |
| **Expected result** | Status remains `CANCELLED` |
| **HTTP/API expectation** | `409 Conflict` |
| **UI expectation** | No status change controls available |
| **Test type** | Integration (Automated) |
| **Priority** | P0 |

### AC-057 — Reject invalid target status value

| Field | Value |
|-------|-------|
| **Requirement** | FR-C10, NFR-C04 |
| **Preconditions** | Ticket in `OPEN` |
| **Action** | `PATCH .../status.json` `{ "status": "INVALID_STATUS" }` |
| **Expected result** | Status remains `OPEN` |
| **HTTP/API expectation** | `400 Bad Request` (unknown enum) |
| **UI expectation** | N/A |
| **Test type** | Integration (Automated) |
| **Priority** | P0 |

---

## 7. Comments

### AC-060 — Add comment via UI

| Field | Value |
|-------|-------|
| **Requirement** | FR-C06; Core acceptance: "A user can add comments" |
| **Preconditions** | Ticket detail open; seeded user selected as comment author |
| **Action** | Enter comment message; submit |
| **Expected result** | Comment persisted and visible in thread |
| **HTTP/API expectation** | `POST /bin/support-tickets/{id}/comments.json` returns `201 Created` with comment `id`, `message`, `createdBy`, `createdAt` |
| **UI expectation** | New comment appears in thread without full page reload (or after expected refresh) |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-061 — Add comment via API

| Field | Value |
|-------|-------|
| **Requirement** | FR-C06, NFR-C02 |
| **Preconditions** | Ticket exists |
| **Action** | `POST /bin/support-tickets/{id}/comments.json` `{ "message": "Customer called back", "createdBy": "/home/users/support/agent1" }` |
| **Expected result** | Comment child node created under ticket in JCR |
| **HTTP/API expectation** | `201 Created` |
| **UI expectation** | N/A |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-062 — Reject empty comment message

| Field | Value |
|-------|-------|
| **Requirement** | FR-C10, NFR-C04 |
| **Preconditions** | Ticket exists |
| **Action** | `POST .../comments.json` `{ "message": "", "createdBy": "/home/users/support/agent1" }` |
| **Expected result** | No comment created |
| **HTTP/API expectation** | `400 Bad Request`; field error on `message` |
| **UI expectation** | Validation error shown; comment not added |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-063 — Comment updates ticket `updatedAt`

| Field | Value |
|-------|-------|
| **Requirement** | FR-C06, data model consistency |
| **Preconditions** | Ticket with known `updatedAt` |
| **Action** | Add comment; re-fetch ticket |
| **Expected result** | Ticket `updatedAt` is greater than or equal to comment `createdAt` |
| **HTTP/API expectation** | `GET /bin/support-tickets/{id}.json` shows refreshed `updatedAt` |
| **UI expectation** | N/A |
| **Test type** | Manual |
| **Priority** | P1 |

---

## 8. Keyword Search

### AC-070 — Search matches ticket title

| Field | Value |
|-------|-------|
| **Requirement** | FR-C07; Core acceptance: "Keyword search ... work" |
| **Preconditions** | Ticket exists with unique keyword in title (e.g. "ZEBRA-UNIQUE-123") |
| **Action** | `GET /bin/support-tickets.json?q=ZEBRA-UNIQUE` |
| **Expected result** | Matching ticket returned; non-matching tickets excluded |
| **HTTP/API expectation** | `200 OK`; array contains only matching ticket(s) |
| **UI expectation** | Search box with same term filters list to matching ticket(s) |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-071 — Search matches ticket description

| Field | Value |
|-------|-------|
| **Requirement** | FR-C07 |
| **Preconditions** | Ticket exists with unique keyword only in description |
| **Action** | `GET /bin/support-tickets.json?q=<unique-description-term>` |
| **Expected result** | Ticket returned |
| **HTTP/API expectation** | `200 OK`; matching ticket in array |
| **UI expectation** | UI search returns same ticket |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-072 — Search is case-insensitive

| Field | Value |
|-------|-------|
| **Requirement** | FR-C07 |
| **Preconditions** | Ticket title contains "Password Reset" |
| **Action** | `GET /bin/support-tickets.json?q=password` |
| **Expected result** | Ticket returned |
| **HTTP/API expectation** | `200 OK`; match found regardless of case |
| **UI expectation** | Lowercase search term finds ticket |
| **Test type** | Manual |
| **Priority** | P1 |

### AC-073 — Search with no matches returns empty list

| Field | Value |
|-------|-------|
| **Requirement** | FR-C07, NFR-C05 |
| **Preconditions** | No ticket contains term `XYZNOMATCH999` |
| **Action** | `GET /bin/support-tickets.json?q=XYZNOMATCH999` |
| **Expected result** | Empty array; not an error |
| **HTTP/API expectation** | `200 OK`; `[]` |
| **UI expectation** | Empty state message (e.g. "No tickets found") |
| **Test type** | Manual |
| **Priority** | P0 |

---

## 9. Status Filter

### AC-080 — Filter tickets by status

| Field | Value |
|-------|-------|
| **Requirement** | FR-C08; Core acceptance: "status filter work" |
| **Preconditions** | Tickets exist in multiple statuses (seed data) |
| **Action** | `GET /bin/support-tickets.json?status=OPEN` |
| **Expected result** | Only tickets with `status: "OPEN"` returned |
| **HTTP/API expectation** | `200 OK`; every item has `"status": "OPEN"` |
| **UI expectation** | Status filter dropdown limits list to selected status |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-081 — Filter by each valid status value

| Field | Value |
|-------|-------|
| **Requirement** | FR-C08 |
| **Preconditions** | At least one ticket per status in seed data (or create as needed) |
| **Action** | For each status in `[OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED]`, call `GET /bin/support-tickets.json?status=<STATUS>` |
| **Expected result** | Results contain only tickets of that status |
| **HTTP/API expectation** | `200 OK` per request; all items match filter |
| **UI expectation** | UI filter produces same subset as API |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-082 — Combined keyword search AND status filter

| Field | Value |
|-------|-------|
| **Requirement** | FR-C07, FR-C08 |
| **Preconditions** | Ticket A: `OPEN`, title contains "billing"; Ticket B: `CLOSED`, title contains "billing" |
| **Action** | `GET /bin/support-tickets.json?q=billing&status=OPEN` |
| **Expected result** | Only Ticket A returned |
| **HTTP/API expectation** | `200 OK`; single matching ticket |
| **UI expectation** | Combined search + filter in UI matches API result |
| **Test type** | Manual |
| **Priority** | P0 |

---

## 10. Persistence

### AC-090 — Data survives AEM restart

| Field | Value |
|-------|-------|
| **Requirement** | FR-C11, NFR-C03; Core acceptance: "Data remains available after restart" |
| **Preconditions** | Known tickets exist; record ticket `id` and field values |
| **Action** | Stop AEM Author (and Publish if tested); restart; `GET /bin/support-tickets/{id}.json` |
| **Expected result** | Ticket data identical to pre-restart values |
| **HTTP/API expectation** | `200 OK`; same `id`, `title`, `status`, comments |
| **UI expectation** | List and detail show same data after restart |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-091 — Seed data available after setup

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C03; common technical: "Seed or sample data" |
| **Preconditions** | Fresh setup per README (packages installed, repoinit applied) |
| **Action** | `GET /bin/support-tickets.json`; verify seeded users via API or AEM user admin |
| **Expected result** | Sample tickets and seeded users present without manual Groovy scripts |
| **HTTP/API expectation** | `200 OK`; non-empty ticket list from seed package |
| **UI expectation** | List shows seed tickets on first launch |
| **Test type** | Setup |
| **Priority** | P0 |

### AC-092 — Tickets stored in JCR (not in-memory)

| Field | Value |
|-------|-------|
| **Requirement** | FR-C11, NFR-C03; database persistence mandatory |
| **Preconditions** | Ticket created via API |
| **Action** | Verify node exists under `/content/support-tickets/` (CRXDE, ResourceResolver, or repository inspection) |
| **Expected result** | Ticket node with properties in JCR |
| **HTTP/API expectation** | N/A |
| **UI expectation** | N/A |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-093 — Database setup or migration scripts provided

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C03; common technical: "Database setup or migration scripts" |
| **Preconditions** | Repository cloned; README available |
| **Action** | Follow README setup; confirm `repoinit` and/or content package installs ticket path structure |
| **Expected result** | `/content/support-tickets` path and ACLs exist without manual CRXDE creation |
| **HTTP/API expectation** | N/A |
| **UI expectation** | N/A |
| **Test type** | Setup |
| **Priority** | P0 |

---

## 11. Backend Validation and Error Handling

### AC-100 — Structured validation error response

| Field | Value |
|-------|-------|
| **Requirement** | FR-C10, NFR-C04, NFR-C05 |
| **Preconditions** | Backend API deployed |
| **Action** | Submit any invalid payload (AC-003, AC-004, AC-005, AC-062) |
| **Expected result** | Consistent JSON error shape across endpoints |
| **HTTP/API expectation** | `400 Bad Request`; body includes `code` and `message`; optional `fields` map |
| **UI expectation** | Human-readable message derived from error response |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-101 — API errors do not expose stack traces or JCR paths

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C05, NFR-C09, SEC-R05 |
| **Preconditions** | Backend API deployed |
| **Action** | Trigger `404` and `409` errors; inspect response bodies |
| **Expected result** | No Java stack traces, internal paths, or sensitive details in JSON |
| **HTTP/API expectation** | Error body contains only safe `code` and `message` |
| **UI expectation** | Generic user-facing message; no raw exception text |
| **Test type** | Manual |
| **Priority** | P1 |

### AC-102 — Invalid transition returns 409 with context

| Field | Value |
|-------|-------|
| **Requirement** | FR-C05, NFR-C05; Core acceptance: "invalid ones are rejected" |
| **Preconditions** | Ticket in `OPEN` |
| **Action** | `PATCH .../status.json` `{ "status": "CLOSED" }` |
| **Expected result** | Clear conflict error with current and requested status |
| **HTTP/API expectation** | `409 Conflict`; includes `currentStatus` and `requestedStatus` |
| **UI expectation** | Message explains transition is not allowed |
| **Test type** | Integration (Automated) + Manual |
| **Priority** | P0 |

### AC-110 — UI displays meaningful error on API failure

| Field | Value |
|-------|-------|
| **Requirement** | FR-C10; Core acceptance: "Show meaningful error states in the UI" |
| **Preconditions** | UI connected to backend |
| **Action** | Trigger validation error (empty title on create) and invalid transition in UI |
| **Expected result** | User sees specific, actionable error — not blank screen or silent failure |
| **HTTP/API expectation** | Underlying `400` or `409` as per scenario |
| **UI expectation** | Visible error banner or inline message; form state preserved where appropriate |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-111 — UI does not rely solely on client-side validation

| Field | Value |
|-------|-------|
| **Requirement** | FR-C10, SEC-C03 |
| **Preconditions** | UI and API running |
| **Action** | Bypass UI validation (e.g. API client) with invalid data; also disable JS if feasible |
| **Expected result** | Backend still rejects invalid input |
| **HTTP/API expectation** | `4xx` responses for invalid input |
| **UI expectation** | N/A for API-only path |
| **Test type** | Manual |
| **Priority** | P0 |

---

## 12. User Entity (Seeded Only)

### AC-120 — Seeded users exist with required fields

| Field | Value |
|-------|-------|
| **Requirement** | FR-C09; entity spec: User (id, name, email, role) |
| **Preconditions** | Setup complete per README |
| **Action** | Inspect seed data via API user list endpoint or AEM Users console |
| **Expected result** | At least two seeded users with `id`, `name`, `email`, `role` |
| **HTTP/API expectation** | User references resolvable in create/comment payloads |
| **UI expectation** | `createdBy` / assignee selectors populated from seeded users only |
| **Test type** | Setup |
| **Priority** | P0 |

### AC-121 — No user-management UI in Core

| Field | Value |
|-------|-------|
| **Requirement** | FR-C09 |
| **Preconditions** | Application UI deployed |
| **Action** | Navigate entire application |
| **Expected result** | No screens to create, edit, or delete users |
| **HTTP/API expectation** | No user CRUD endpoints exposed |
| **UI expectation** | No user admin UI within support app |
| **Test type** | Manual |
| **Priority** | P0 |

---

## 13. Common Technical Requirements

### AC-130 — Frontend application present

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C01 |
| **Preconditions** | Application deployed |
| **Action** | Open support app URL on Author and Publish |
| **Expected result** | Functional UI for list, detail, create, edit, comment, search, filter |
| **HTTP/API expectation** | N/A |
| **UI expectation** | HTL page + Clientlibs renders without console errors blocking core flows |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-131 — Backend API present

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C02 |
| **Preconditions** | AEM running |
| **Action** | Call each documented servlet endpoint |
| **Expected result** | JSON REST API responds for list, create, detail, update, status, comment |
| **HTTP/API expectation** | All Core endpoints return expected status codes for valid/invalid input |
| **UI expectation** | N/A |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-132 — Input validation at backend

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C04; Core acceptance: "Backend validation prevents invalid records" |
| **Preconditions** | API deployed |
| **Action** | Execute AC-003, AC-004, AC-005, AC-057, AC-062 |
| **Expected result** | All invalid records rejected; none persisted |
| **HTTP/API expectation** | `400` or `409` as appropriate; no `500` for validation failures |
| **UI expectation** | Corresponding UI errors for each scenario |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-133 — README setup instructions complete

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C07 |
| **Preconditions** | Clean environment; README only |
| **Action** | Follow README to install SDK, packages, seed data; run app |
| **Expected result** | Reviewer can reach UI and API without undocumented steps |
| **HTTP/API expectation** | N/A |
| **UI expectation** | App accessible at documented URLs |
| **Test type** | Setup |
| **Priority** | P0 |

### AC-134 — No secrets committed to repository

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C09; Core acceptance: "No secrets committed to the repo" |
| **Preconditions** | Repository checkout |
| **Action** | Search codebase for passwords, API keys, private keys, `.env` with real credentials |
| **Expected result** | No secrets in tracked files; `.env.example` only if needed |
| **HTTP/API expectation** | N/A |
| **UI expectation** | N/A |
| **Test type** | Code review |
| **Priority** | P0 |

### AC-135 — Lifecycle artifacts present in repository

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C08 |
| **Preconditions** | Repository checkout |
| **Action** | Verify required artifact paths from assignment structure exist |
| **Expected result** | `requirements-analysis.md`, `acceptance-criteria.md`, `ai-prompts/`, etc. present |
| **HTTP/API expectation** | N/A |
| **UI expectation** | N/A |
| **Test type** | Code review |
| **Priority** | P0 |

### AC-136 — Full prompt history captured

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C08; common technical: "Full prompt history" |
| **Preconditions** | Repository checkout |
| **Action** | Inspect `ai-prompts/` directory |
| **Expected result** | Planning, design, implementation, testing, debugging, review, documentation prompt logs exist |
| **HTTP/API expectation** | N/A |
| **UI expectation** | N/A |
| **Test type** | Code review |
| **Priority** | P0 |

---

## 14. AEM Topology

### AC-150 — Application runs on AEMaaCS SDK (latest)

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C10 |
| **Preconditions** | Local SDK installed per README |
| **Action** | Verify SDK version documented; app deploys to Author |
| **Expected result** | Multi-module AEM project builds and installs |
| **HTTP/API expectation** | Servlets respond on Author |
| **UI expectation** | UI accessible on Author |
| **Test type** | Setup |
| **Priority** | P0 |

### AC-151 — Publish tier serves application

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C11; approved UI on Publish |
| **Preconditions** | Content and code replicated/installed on Publish |
| **Action** | Open support app on Publish (port 4503) |
| **Expected result** | List and detail functional with replicated data |
| **HTTP/API expectation** | `GET /bin/support-tickets.json` on Publish returns tickets |
| **UI expectation** | Same core flows as Author |
| **Test type** | Manual |
| **Priority** | P0 |

### AC-152 — Dispatcher serves Publish application

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C11; topology: 1 Dispatcher |
| **Preconditions** | Dispatcher configured per README; Publish running |
| **Action** | Open support app via Dispatcher URL (port 80) |
| **Expected result** | UI loads; list reflects current data; mutations work with CSRF |
| **HTTP/API expectation** | `/bin/support-tickets` accessible through Dispatcher; not blocked by default filters |
| **UI expectation** | Create/update/comment succeed through Dispatcher |
| **Test type** | Manual |
| **Priority** | P0 |

---

## 15. Mandatory Test Tier — State Machine Integration Tests

### AC-160 — Integration tests exist for state machine

| Field | Value |
|-------|-------|
| **Requirement** | NFR-C06, TEST-C01; common technical: "At least one meaningful test tier" |
| **Preconditions** | Source code checkout |
| **Action** | Run `mvn test` (or documented test command) |
| **Expected result** | `TicketStateMachineServiceTest` (or equivalent) runs without running AEM |
| **HTTP/API expectation** | N/A |
| **UI expectation** | N/A |
| **Test type** | Automated |
| **Priority** | P0 |

### AC-161 — All five valid transitions pass in automated tests

| Field | Value |
|-------|-------|
| **Requirement** | TEST-C02 |
| **Preconditions** | Test suite compiles |
| **Action** | `mvn test` |
| **Expected result** | AC-040 through AC-044 covered by green tests |
| **HTTP/API expectation** | N/A |
| **UI expectation** | N/A |
| **Test type** | Automated |
| **Priority** | P0 |

### AC-162 — Representative invalid transitions fail in automated tests

| Field | Value |
|-------|-------|
| **Requirement** | TEST-C03; Core acceptance: "State-machine integration tests pass" |
| **Preconditions** | Test suite compiles |
| **Action** | `mvn test` |
| **Expected result** | At minimum AC-050, AC-051, AC-052, AC-053, AC-054, AC-055, AC-056, and AC-034 (status-in-PUT) covered by green tests; total ≥ 13 state-machine cases |
| **HTTP/API expectation** | N/A |
| **UI expectation** | N/A |
| **Test type** | Automated |
| **Priority** | P0 |

---

## 16. Core Acceptance Checklist (Assignment Wording)

Quick reference mapping to original Core Acceptance Criteria:

| # | Assignment criterion | Pass condition (acceptance IDs) |
|---|---------------------|--------------------------------|
| 1 | Create ticket via UI | AC-001 |
| 2 | View all tickets from database | AC-010, AC-011 |
| 3 | Open ticket detail view | AC-020, AC-021 |
| 4 | Update fields and reassign | AC-030 – AC-033 |
| 5 | Add comments | AC-060, AC-061 |
| 6 | Valid transitions only; invalid rejected | AC-040 – AC-044, AC-050 – AC-057, AC-102, AC-161, AC-162 |
| 7 | Keyword search and status filter work | AC-070 – AC-073, AC-080 – AC-082 |
| 8 | Data remains after restart | AC-090 |
| 9 | Backend validation prevents invalid records | AC-003 – AC-005, AC-057, AC-062, AC-100, AC-132 |
| 10 | No secrets in repo | AC-134 |
| 11 | State-machine integration tests pass | AC-160 – AC-162 |

---

## 17. Out of Scope (Core)

The following are explicitly **not** Core acceptance criteria:

- User CRUD UI (FR-S02)
- Authentication / RBAC (FR-S03)
- Filter by priority or assignee, sorting, pagination (FR-S04)
- Ticket or comment delete
- Comment edit
- Keyword search in comments
- OpenAPI / Swagger (FR-S05)
- Docker / CI (FR-S06)
- Optimistic locking / concurrent update handling

---

## 18. Verification Summary

| Priority | Count | Description |
|----------|-------|-------------|
| P0 | 62 | Must pass for Core completion |
| P1 | 5 | Recommended quality gates (AC-007, AC-022, AC-063, AC-072, AC-101) |

| Test type | Count (approx.) |
|-----------|-----------------|
| Manual | 45 |
| Integration (Automated) | 14 |
| Automated (`mvn test`) | 3 |
| Setup | 5 |
| Code review | 3 |

---

## 19. Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-26 | AI-assisted | Initial acceptance criteria matrix from approved requirements analysis |
