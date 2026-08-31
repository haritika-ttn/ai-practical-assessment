# Design Prompts

**Project:** Support Ticket Management System (AEMaaCS)  
**Source:** Cursor agent transcript `13262a8d-8e65-407b-ab4b-200d6bdc9f58`  
**Phase:** Design  
**Prompt count:** 7 (deduplicated user messages)  
**Session span:** Wednesday, Aug 26, 2026, 4:14 PM (UTC+5:30) → Monday, Aug 31, 2026, 1:17 PM (UTC+5:30)

## Phase summary

Architecture, JCR data model, API contract, state machine, UI flow, security review, and phased implementation plan.

**Primary artifacts produced:**

- `design-notes.md`
- `data-model.md`
- `api-contract.md`
- `ui-flow.md`
- `implementation-plan.md`

---

## Prompt history

Prompts below are **verbatim** user messages from the Cursor Agent chat, in chronological order within this phase. Assistant responses and tool calls are omitted. Consecutive duplicate prompts are collapsed to one entry.

### Prompt 1

**Timestamp:** Wednesday, Aug 26, 2026, 4:37 PM (UTC+5:30)

```text
Using the approved requirements and acceptance criteria, design the architecture for the Support Ticket Management System.

Constraints:

- AEM as a Cloud Service
- Latest AEMaaCS SDK
- 1 Author
- 1 Publisher
- 1 Dispatcher
- JCR is acceptable for persistence
- No unnecessary external services

Design:

1. AEM Maven module structure
2. core module responsibilities
3. ui.apps responsibilities
4. ui.content responsibilities
5. Dispatcher responsibilities
6. Author responsibilities
7. Publisher responsibilities
8. OSGi service boundaries
9. Servlet boundaries
10. Sling Model usage
11. JCR persistence approach
12. search strategy
13. user lookup strategy
14. authorization approach
15. frontend architecture
16. testing architecture
17. seed-data strategy

For each architectural decision:
- identify alternatives
- explain trade-offs
- recommend one
- explain why

Do not generate implementation code yet.

Produce an architecture diagram using Mermaid where useful.
```

---

### Prompt 2

**Timestamp:** Thursday, Aug 27, 2026, 10:25 AM (UTC+5:30)

```text
Design the JCR data model for the Support Ticket Management System.

Requirements:

User:
- id
- name
- email
- role

Ticket:
- id
- title
- description
- priority
- status
- assignedTo
- createdBy
- createdAt
- updatedAt

Comment:
- id
- ticketId
- message
- createdBy
- createdAt

Determine:

1. Recommended JCR hierarchy.
2. Node types.
3. Properties.
4. Child-node relationships.
5. Naming conventions.
6. Required vs optional properties.
7. Validation constraints.
8. How users should be referenced.
9. How comments should be persisted.
10. How search should work.
11. How seed data should be represented.
12. How the design remains maintainable as ticket volume grows.

Consider whether QueryBuilder is appropriate and explain the trade-offs.

Do not generate Java code.
```

---

### Prompt 3

**Timestamp:** Thursday, Aug 27, 2026, 11:10 AM (UTC+5:30)

```text
Create the REST API contract for the Support Ticket Management System based on the approved requirements.

Define the minimum Core API surface.

For every endpoint specify:

- HTTP method
- URL
- authentication expectation
- authorization expectation
- request headers
- request body
- query parameters
- response body
- HTTP status codes
- validation errors
- business errors
- not-found behavior
- example success response
- example error response

The status-transition API must be separate from general ticket updates unless there is a strong architectural reason not to.

Define a consistent error response format.

Do not implement the APIs yet.

The output should be suitable for `api-contract.md`.
```

---

### Prompt 4

**Timestamp:** Thursday, Aug 27, 2026, 11:14 AM (UTC+5:30)

```text
review the ticket lifecycle.

The allowed state transitions are:

OPEN -> IN_PROGRESS
IN_PROGRESS -> RESOLVED
RESOLVED -> CLOSED
OPEN -> CANCELLED
IN_PROGRESS -> CANCELLED

Design a robust state-machine implementation for an AEM/OSGi application.

Requirements:

1. The backend must be the authoritative enforcement point.
2. The frontend must never be trusted to enforce the rules.
3. Invalid transitions must return a meaningful business error.
4. The state-machine logic must be independently testable.
5. The design should make adding a future status or transition straightforward.
6. The service should not become tightly coupled to the HTTP servlet.

Compare at least two implementation approaches.

Recommend one.

Then define:
- domain behavior
- service boundary
- exception/error strategy
- HTTP mapping
- unit-test cases
- integration-test cases
- edge cases

Do not generate implementation code yet.
```

---

### Prompt 5

**Timestamp:** Thursday, Aug 27, 2026, 11:19 AM (UTC+5:30)

```text
Design the minimum practical UI for the Support Ticket Management System.

Core features only.

Required user flows:

1. List tickets
2. Search tickets
3. Filter by status
4. Create ticket
5. Open ticket detail
6. Edit ticket
7. Reassign ticket
8. Change status
9. Add comment
10. Handle backend errors

For each screen define:

- purpose
- fields
- actions
- validation
- API interaction
- loading state
- empty state
- error state
- success feedback

The UI should be simple and professional rather than visually complex.

Do not introduce unnecessary UI functionality.
Do not write the code implementation yet.

Produce a Mermaid user-flow diagram where useful.
```

---

### Prompt 6

**Timestamp:** Thursday, Aug 27, 2026, 11:23 AM (UTC+5:30)

```text
Perform a security review of the proposed AEM Support Ticket Management System before implementation.

Review:

- AEM authentication
- AEM users/groups
- authorization
- resource permissions
- servlet exposure
- CSRF
- XSS
- request validation
- output encoding
- JCR access
- query injection
- Dispatcher rules
- API caching
- sensitive data
- secrets
- error messages
- logging

For every concern:
- identify the risk
- explain the attack/impact
- recommend a practical mitigation
- identify whether it belongs in Core or Stretch

Do not over-engineer the solution.
Do not write the code implementation yet.
Produce recommendations that are appropriate for an AEMaaCS implementation.
```

---

### Prompt 7

**Timestamp:** Thursday, Aug 27, 2026, 11:33 AM (UTC+5:30)

```text
Using the approved requirements, architecture, data model, API contract, UI flow, security analysis, and acceptance criteria, create the implementation plan.

Break implementation into small, independently verifiable tasks.

For each task provide:

- Task ID
- Description
- Files/modules affected
- Dependencies
- Expected outcome
- Acceptance criteria
- Test requirement
- AI assistance opportunity
- Developer validation required

Order the tasks so that each stage leaves the project buildable.

Do not implement anything yet.

Prioritize Core over Stretch.
```
