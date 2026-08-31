# Testing Prompts

**Project:** Support Ticket Management System (AEMaaCS)  
**Source:** Cursor agent transcript `13262a8d-8e65-407b-ab4b-200d6bdc9f58`  
**Phase:** Testing  
**Prompt count:** 3 (deduplicated user messages)  
**Session span:** Wednesday, Aug 26, 2026, 4:14 PM (UTC+5:30) → Monday, Aug 31, 2026, 1:17 PM (UTC+5:30)

## Phase summary

Core automated test suite, test strategy, and recorded test execution results.

**Primary artifacts produced:**

- `test-strategy.md`
- `test-results.md`
- `core/src/test/`

---

## Prompt history

Prompts below are **verbatim** user messages from the Cursor Agent chat, in chronological order within this phase. Assistant responses and tool calls are omitted. Consecutive duplicate prompts are collapsed to one entry.

### Prompt 1

**Timestamp:** Saturday, Aug 29, 2026, 9:12 PM (UTC+5:30)

```text
Using the acceptance criteria and implementation, create the Core automated test suite.

Prioritize:

1. State-machine integration tests.
2. Backend validation.
3. Ticket creation.
4. Ticket retrieval.
5. Ticket update.
6. Comments.
7. Search.
8. Status filtering.
9. Error scenarios.

For the state machine, explicitly test every valid transition and every meaningful invalid transition.

Tests must verify behavior, not implementation details.

For every test explain:
- requirement being verified
- setup
- action
- expected result

Do not weaken tests to make existing implementation pass.

If implementation behavior conflicts with the requirements, identify it instead.
```

---

### Prompt 2

**Timestamp:** Monday, Aug 31, 2026, 12:02 PM (UTC+5:30)

```text
# Generate Test Strategy

Create or update:

`test-strategy.md`

The Support Ticket Management System implementation is now complete.

Use the following as the source of truth:

- requirements-analysis.md
- acceptance-criteria.md
- design-notes.md
- api-contract.md
- data-model.md
- ui-flow.md
- implementation-plan.md
- actual source code
- actual test code
- actual AEM project structure
- available debugging/prompt history

Do not invent tests or capabilities that are not present in the project.

## Objective

Create a realistic test strategy for the Support Ticket Management System that explains:

- What is being tested
- Why it is being tested
- At which test tier it is tested
- Important positive and negative scenarios
- How the AEM/JCR persistence layer is tested
- How backend validation is tested
- How the ticket state machine is tested
- How search/filter functionality is tested
- How comments are tested
- How error handling is tested
- What is intentionally not covered

## Required sections

### 1. Test objectives

Explain the quality goals for the application.

### 2. Test scope

Cover the implemented Core requirements:

- Ticket creation
- Ticket listing
- Ticket detail
- Ticket updates
- Ticket reassignment
- Comment creation
- Search
- Status filtering
- Status transitions
- Backend validation
- Error handling
- Persistence

Only include functionality that actually exists.

### 3. Test levels / tiers

Identify the actual test tiers present in the project, for example:

- Unit tests
- Integration tests
- AEM context tests
- API-level tests
- Frontend tests
- End-to-end tests

Do not claim a test tier exists unless it is actually implemented.

### 4. State-machine testing

This is a mandatory assessment area.

Document the expected valid transitions:

Open → In Progress
In Progress → Resolved
Resolved → Closed
Open → Cancelled
In Progress → Cancelled

Also document invalid transitions that must be rejected.

Explain why state-machine integration testing is important.

### 5. Validation testing

Document required-field and invalid-input scenarios actually supported by the implementation.

### 6. Persistence testing

Explain how persistence is verified, including relevant JCR behavior.

### 7. Search/filter testing

Document keyword search and status-filter scenarios.

### 8. Error-handling testing

Document expected behavior for:

- invalid input
- missing ticket
- invalid status transition
- missing/invalid user where applicable
- persistence failures where testable

### 9. Test data

Document test/sample data actually used.

### 10. Test environment

Document the actual AEM SDK, Java version, Maven setup, and other relevant environment details.

### 11. Acceptance-criteria coverage

Create a table mapping each acceptance criterion to:

- Test scenario
- Test level
- Automated/manual
- Status

Do not mark anything as passed unless supported by actual test evidence.

### 12. Known test limitations

Document genuine gaps or areas not covered.

### 13. Recommended future tests

Separate future/stretch tests from the tests actually implemented.

## Important

This is a TEST STRATEGY document, not a test-results report.

Do not fabricate test execution results.

Do not modify source code or test code.

After creating the file, verify that every stated test capability is supported by the actual repository.
```

---

### Prompt 3

**Timestamp:** Monday, Aug 31, 2026, 12:07 PM (UTC+5:30)

```text
# Generate Test Results

Create or update:

`test-results.md`

The Support Ticket Management System implementation and test execution are complete.

Use the actual repository, test source code, Maven output, test reports, build output, and available project history as the source of truth.

## Critical requirement

Record ONLY actual test execution results.

Do NOT:

- invent test results
- infer that a test passed because the code looks correct
- mark tests as passed without execution evidence
- confuse expected results with actual results
- claim coverage percentages unless actually measured
- claim frontend/E2E tests if they were not executed
- claim integration tests if they were not actually executed

If a result cannot be verified, write:

`[Result not available in project artifacts]`

## Required sections

### 1. Test execution summary

Include:

- Date/time if available
- Java version
- Maven version
- AEM SDK/version
- Test framework(s)
- Test command(s) executed
- Overall result

### 2. Build validation

Record the actual Maven commands executed, for example:

```text
mvn -B clean install
```
