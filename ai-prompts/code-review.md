# Code Review Prompts

**Project:** Support Ticket Management System (AEMaaCS)  
**Source:** Cursor agent transcript `13262a8d-8e65-407b-ab4b-200d6bdc9f58`  
**Phase:** Code Review  
**Prompt count:** 1 (deduplicated user messages)  
**Session span:** Wednesday, Aug 26, 2026, 4:14 PM (UTC+5:30) → Monday, Aug 31, 2026, 1:17 PM (UTC+5:30)

## Phase summary

Formal static review artifact generation (findings only, no code changes).

**Primary artifacts produced:**

- `code-review-notes.md`
- `review-fixes.md`

---

## Prompt history

Prompts below are **verbatim** user messages from the Cursor Agent chat, in chronological order within this phase. Assistant responses and tool calls are omitted. Consecutive duplicate prompts are collapsed to one entry.

### Prompt 1

**Timestamp:** Monday, Aug 31, 2026, 12:21 PM (UTC+5:30)

```text
# Generate Code Review Artifacts

The Support Ticket Management System is currently implemented and the project is building/running successfully.

I now want to perform a formal engineering code review and create the following artifacts:

- `code-review-notes.md`
- `review-fixes.md`

## Source of truth

Inspect the actual repository/project implementation and the available AI prompt history.

Use the following as sources:

- approved requirements
- `requirements-analysis.md`
- `acceptance-criteria.md`
- `implementation-plan.md`
- `design-notes.md`
- `api-contract.md`
- `data-model.md`
- `ui-flow.md`
- `test-strategy.md`
- actual source code
- actual tests
- actual Maven configuration
- actual AEM configuration
- actual build/test results
- available AI prompt history

Do NOT invent implementation details, tests, fixes, or review findings.

If something cannot be verified from the repository or available history, explicitly state:

`[Not verifiable from available project artifacts]`

## Code review scope

Review the implementation as a senior AEM engineer.

Evaluate:

### Architecture
- separation of concerns
- Sling Servlet design
- OSGi service design
- persistence layer
- Sling Models where applicable
- DTOs/mapping
- business logic placement
- state-machine implementation
- dependency management

### AEM/JCR
- JCR API usage
- ResourceResolver/session lifecycle
- repository structure
- node/property naming
- repository traversal
- query efficiency
- persistence patterns
- exception handling
- AEMaaCS compatibility

### API
- endpoint design
- HTTP methods
- status codes
- request/response handling
- validation
- error responses
- authorization/security considerations

### Business rules
- state-machine enforcement
- invalid transition handling
- separation between persistence and business logic

### Testing
- integration tests
- state-machine tests
- validation tests
- error cases
- test isolation
- meaningful coverage

### Security
- input validation
- authorization
- secrets
- user handling
- repository permissions
- unsafe input handling

### Code quality
- naming
- maintainability
- duplication
- unnecessary abstraction
- exception handling
- logging
- readability

## code-review-notes.md

Create:

`code-review-notes.md`

Structure it as:

1. Review scope
2. Files/modules reviewed
3. Architecture review
4. AEM/JCR review
5. API review
6. Business-rule review
7. Security review
8. Test review
9. Code-quality review
10. Findings

For each finding include:

- Severity: Critical / High / Medium / Low
- File/path
- Finding
- Why it matters
- Recommendation

Clearly distinguish genuine issues from optional improvements.

## review-fixes.md

Create:

`review-fixes.md`

This should document the fixes actually made as a result of the review.

For each fix include:

- Finding reference
- Original issue
- Decision: Fix / Accept / Defer
- Reason
- Files changed
- Change made
- Validation performed
- Actual result

IMPORTANT:

Do not claim that a review finding was fixed unless the change was actually implemented and validated.

Do not make code changes yet.

First create the review findings and proposed fixes.

STOP after producing the review findings and proposed-fix plan.
```
