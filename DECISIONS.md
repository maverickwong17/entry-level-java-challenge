# Decisions

Fill this in as part of your submission. Brief and specific beats long and
vague — a few sentences per question is plenty. Replace the prompts with your
answers.

## 1. Which endpoints did you add, and why does this system need them?

`PATCH /api/v1/employee/{uuid}` for partial updates.

The API could create and read employees but never change one.   
That matters here because this system exists to sync with Employees-R-US during a migration: 
records will be corrected, and the fields can change over an employee's life.   
Without an update path, a consumer would have to recreate the employee and get a new UUID.  
There was a consideration to search via email, but there may be duplicate entries but UUID is unique.

## 2. Did you add any dependency? What does it buy, and what does it cost?

No. `build.gradle` is unchanged.

Validated the body manually in the service instead of adding `spring-boot-starter-validation`.
Annotations don't fit PATCH well: update merges the partial body into the stored employee and validates the result, so one rule set covers create and update. 
The termination-before-hire rule also compares two fields and would need a custom constraint, so the dependency would still have left me writing part of the validation by hand. It would also be one more dependency to track and upgrade, for little gain.

The cost is that the rules are not declared on the model, and the validation code is longer than annotations would be. 

## 3. How did you handle errors and logging, and why those mechanisms?

I chose a central handler over try/catch in controllers and `ResponseStatusException` (one message at a time, mapping scattered).

**Errors.** One `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`, returning `ProblemDetail` (RFC 7807).
- Unknown employee: 404.
- Malformed UUID, unreadable JSON, unparseable date, failed validation: 400.
  - Validation failures list every problem in an `errors` array so a client
  fixes them in one pass.
- Anything unexpected: 500 with a generic message. The stack trace goes to
  the log, never to the client.

**Logging.** I asked what I would want at 3 a.m.
- Creates and updates: info, with the UUID only.
- Rejected requests: warn, with the field messages but never the values.
- Unknown employee: warn.
- Unexpected exceptions: error, with the stack trace.
- Reads: debug, so routine traffic does not bury the lines above.

No salary, email, or name is logged, and I never log a whole employee object.
The UUID is the correlation key between a client report and a log line.

## 4. Where the brief left a decision to you, what did you decide?

- **Request vs. model.** The POST body is its own class   (`EmployeeRequestBody`), not `Employee`, so clients cannot set `uuid` or `fullName`.
The server generates the UUID and derives `fullName` from the trimmed first and last name.
- **Required fields.** `firstName`, `lastName`, `jobTitle`, `email`, `salary`, `age`, `contractHireDate`. `contractTerminationDate` is optional. 
`salary` and `age` are `Integer` so a missing value is null and can be rejected, where an `int` would silently become 0.
- **Rules:** 
    - Names and job title non-blank; 
    - email must match a basic address pattern; 
    - salary not negative; 
    - age 16 to 100 (my picks); 
    - termination date not before hire date. 
    - Dates are ISO-8601 `Instant`s.
- **Status codes.** 201 with a `Location` header on create, 404 for an unknown UUID, 400 for a malformed one.
- **PATCH.** A null or omitted field means "unchanged". 
The patch is validated as the merged result, so the date rule works when only one date is sent, and an invalid patch leaves the employee untouched. 
An empty patch is a 400. Create and patch share one request class because validation lives in the service; I would split them if it moved to annotations.
## 5. What did you deliberately choose *not* to do?

- **Pagination.** The list is unbounded. Fine for mock data but I would add it once the list grows past a few hundred records.
- **Search by email.** I considered it but chose update as the bigger gap. I would revisit it when a consumer needs lookup without downloading everyone to find their specific uuid.
- **Duplicate-email check.** Not enforced. I would add it, returning 409, if email becomes the lookup key.
- **Authentication.** Out of scope per the brief. The API is open, and I would not expose it beyond a trusted network. I would revisit it before any real deployment, starting with a shared secret or signature check for
  the webhooks.