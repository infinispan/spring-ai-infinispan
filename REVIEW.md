# Pull Request Reviewing Instructions

## Verify the Author Checklist

- Test coverage for new functionality
- Integration tests pass with Testcontainers
- Commit message is clear and descriptive
- No breaking changes to public API without discussion

## What Important Means Here

Reserve Important for findings that would break behavior, cause data loss, or introduce security vulnerabilities. Style, naming, and refactoring suggestions are Nit at most.

## Project-Specific Concerns

- **Protobuf schema compatibility:** changes to the Protobuf schema must be backward compatible for existing caches
- **Ickle query correctness:** filter expression conversion must produce valid Ickle and properly escape user input
- **Marshalling:** any new or modified marshaller must correctly serialize/deserialize all field types
- **Spring AI API compatibility:** changes must compile against the targeted Spring AI release (currently 2.0.0)
- **Jackson 3:** this project uses `tools.jackson` packages, not `com.fasterxml.jackson`

## Cap the Nits

At most five Nits per review. "No blocking issues." if everything is a Nit.

## Do Not Report

- Anything CI already enforces
- Test-only code that intentionally violates production rules
