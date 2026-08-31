# Planning Prompts

**Project:** Support Ticket Management System (AEMaaCS)  
**Source:** Cursor agent transcript `13262a8d-8e65-407b-ab4b-200d6bdc9f58`  
**Phase:** Planning  
**Prompt count:** 4 (deduplicated user messages)  
**Session span:** Wednesday, Aug 26, 2026, 4:14 PM (UTC+5:30) → Monday, Aug 31, 2026, 1:17 PM (UTC+5:30)

## Phase summary

Requirements intake, adversarial review, and lifecycle spec kickoff (`requirements-analysis.md`, `acceptance-criteria.md`).

**Primary artifacts produced:**

- `requirements-analysis.md`
- `acceptance-criteria.md`

---

## Prompt history

Prompts below are **verbatim** user messages from the Cursor Agent chat, in chronological order within this phase. Assistant responses and tool calls are omitted. Consecutive duplicate prompts are collapsed to one entry.

## Cursor AskQuestion (not a chat message)

After the initial requirements prompt, the Agent invoked **AskQuestion** before drafting the requirements plan:

| Question | Developer choice |
|----------|------------------|
| UI topology | **Both Author and Publish** |
| API style | **Custom Sling Servlets** (JSON at `/bin/support-tickets`) |

---

### Prompt 1

**Timestamp:** Wednesday, Aug 26, 2026, 4:14 PM (UTC+5:30)

```text
We are building a Support Ticket Management System together. 
Below are the requirements -
Common Technical Requirements
Whichever option you choose, every submission must include:
Frontend application
Backend API
Database persistence
Database setup or migration scripts
Seed or sample data
Input validation
Error handling
One working search or filter capability (Core); more in Stretch
At least one meaningful test tier (Core); more in Stretch
README setup instructions
Full prompt history
All planning, design, testing, debugging, review, reflection, and PR artifacts in the repository structure
The full set of lifecycle artifacts is required regardless of option. Only the application surface area is small — the artifacts are the point.
Database Requirement
A database is mandatory. Acceptable options include PostgreSQL, MySQL, MongoDB, SQLite, H2, or any framework-supported local database (eg JCR in AEM). Provide your database choice, setup instructions, a schema/migration/initialization script, seed data, an environment variable example (if applicable), and steps to run locally.
Authentication
Authentication is optional — the focus is AI-assisted engineering across the lifecycle, not auth. If implemented well it counts as Stretch evidence (login/logout, JWT or session auth, role-based access, protected routes, API authorization).
Project: Support Ticket Management System
Business Context
A small application for managing support tickets. Internal users create, update, comment on, search, and progress tickets through a defined lifecycle.
Core (Mandatory)
Entities
User        (seeded only — no user-management UI required)
- id, name, email, role
 
Ticket
- id, title, description, priority, status,
  assignedTo, createdBy, createdAt, updatedAt
 
Comment
- id, ticketId, message, createdBy, createdAt

Features
Create a ticket.
List tickets.
View ticket detail.
Update ticket fields (title, description, priority, assignee).
Change ticket status through the enforced state machine.
Add comments to a ticket.
Keyword search and filter by status.
Persist all data; data survives restart.
Validate required fields; reject invalid input at the backend.
Show meaningful error states in the UI.

Implementation Assumptions/Suggestions 
AEM Development is required to solve the problem using the following topology: 1 Author + 1 Publisher + 1 Dispatcher. 
DO NOT try to solve problems like data sync across multiple publisher environments (or keep it as a stretch goal if you want to).
The solution should be built on AEM. For an AEM solution, use the latest AEMaaCS SDK.
Dev can utilise adobe official skills
Dev can utilise spec-driven workflow (WF). Ensure you read the requirement document first.
For user management, you can utilize AEM's OOTB user management. How you want to persist Ticket and Comments is completely up to you.
Status state machine (the signature judgment piece — kept in Core)
Open         -> In Progress
In Progress  -> Resolved
Resolved     -> Closed
Open         -> Cancelled
In Progress  -> Cancelled
Invalid transitions must be rejected by the backend and handled clearly in the frontend. This is deliberately the hardest part of Core because it is where engineering judgment shows.
Mandatory test tier: integration tests that prove the state-machine rules — valid transitions succeed, invalid transitions are rejected.

Stretch (Optional — evidence toward C1.1)
Third entity or richer data model
Full user CRUD and role management
Authentication, protected routes, API authorization checks
Filter by priority and assignee; sorting; pagination
Additional test tiers: unit tests and edge-case/failure tests
API documentation (Swagger / OpenAPI)
Docker setup, CI workflow
Reusable prompt templates, rules, or specs (persistent project context)

Core Acceptance Criteria
A user can create a ticket via the UI.
A user can view all tickets from the database.
A user can open a ticket detail view.
A user can update ticket fields and reassign.
A user can add comments.
Status changes only through valid transitions; invalid ones are rejected.
Keyword search and status filter work.
Data remains available after restart.
Backend validation prevents invalid records.
No secrets committed to the repo.
State-machine integration tests pass.

Required Repository Structure
Submit a Git repository following this structure as closely as possible:
ai-practical-assessment/
  README.md
  candidate-info.md
  tool-workflow.md
  requirements-analysis.md
  acceptance-criteria.md
  implementation-plan.md
  design-notes.md
  api-contract.md   data-model.md   ui-flow.md
  test-strategy.md
  src/   tests/
  ui.content
  test-results.md   debugging-notes.md
  code-review-notes.md   review-fixes.md
  pr-description.md   reflection.md   final-ai-usage-summary.md
  ai-prompts/
    planning.md  design.md  implementation.md
    testing.md  debugging.md  code-review.md  documentation.md
  tool-specific/
    kiro-specs/ | cursor-workflow/ | other-tool-workflow/

Analyze the attached Support Ticket Management System requirements.
Do not write code.
I want you to:
1. Decompose the requirements into functional and non-functional requirements.
2. Separate Core from Stretch.
3. Identify explicit technical constraints.
4. Identify ambiguities and assumptions.
5. Identify AEM-specific considerations.
6. Identify security requirements.
7. Identify testing requirements.
8. Identify documentation and lifecycle-artifact requirements.
9. Identify risks.
10. Produce a recommended implementation strategy.
The solution must use:
- AEM as a Cloud Service
- Latest AEMaaCS SDK
- 1 Author
- 1 Publisher
- 1 Dispatcher

JCR is an acceptable persistence mechanism.

Do not introduce technologies merely because they are common in modern full-stack applications. Prefer the simplest AEM-native solution that satisfies the requirements.

Most importantly, distinguish between:
- What the requirement explicitly says
- What we are assuming
- What you are recommending

At the end, provide a list of decisions that I, as the developer, should explicitly approve before implementation begins.

Do not generate application code.
```

---

### Prompt 2

**Timestamp:** Wednesday, Aug 26, 2026, 4:22 PM (UTC+5:30)

```text
Review the Support Ticket Management System requirements as a critical technical reviewer.

Try to find weaknesses, ambiguities, hidden requirements, edge cases, and areas where an implementation could technically satisfy the acceptance criteria but still be considered poor engineering.

Pay particular attention to:

- AEM Author vs Publisher responsibilities
- JCR persistence
- ticket ownership
- user identity
- authorization
- status transitions
- concurrent updates
- validation
- search
- API error handling
- Dispatcher
- caching
- CSRF
- XSS
- seed data
- persistence after restart
- testability

For each issue:
1. Explain the concern.
2. Explain whether it is Core or Stretch.
3. Recommend a practical solution.
4. Explain whether that solution should be documented as an explicit architectural decision.

Do not write code.
```

---

### Prompt 3

**Timestamp:** Wednesday, Aug 26, 2026, 4:25 PM (UTC+5:30)

```text
generate requirements-analysis.md
```

---

### Prompt 4

**Timestamp:** Wednesday, Aug 26, 2026, 4:31 PM (UTC+5:30)

```text
Using the approved requirements analysis, convert the requirements into a precise, testable acceptance-criteria matrix.

For every acceptance criterion provide:

- ID
- Requirement
- Preconditions
- Action
- Expected result
- HTTP/API expectation where applicable
- UI expectation where applicable
- Test type
- Priority

Make sure every requirement from the original assignment is represented.

Pay special attention to the ticket state machine.

Include explicit acceptance criteria for:
- every valid transition
- representative invalid transitions
- backend validation
- search
- status filtering
- persistence
- comments
- error handling

Do not add unnecessary product functionality.
generate acceptance-criteria.md
```
