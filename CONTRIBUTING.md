# Contributing to Spring AI Infinispan

Contributions from the community are essential in keeping this project strong and successful.

## Legal

All original contributions to this project are licensed under the ASL - Apache License, version 2.0 or later.

All contributions are subject to the Developer Certificate of Origin (DCO).

## Getting Started

- Make sure you have a GitHub account
- Fork the repository
- Clone your fork
- Set up your development environment (Java 17+, Maven, Docker for tests)

## Create a Topic Branch

Create a branch from `main`:

```bash
git checkout -b my-feature origin/main
```

## Building

```bash
mvn install -DskipTests
```

## Testing

Integration tests require Docker (for Testcontainers):

```bash
mvn verify
```

## Commit

- Make commits of logical units
- Write clear, descriptive commit messages
- Add necessary tests
- Run all tests before submitting

## Use of Generative AI

AI tools may be used provided you fully understand every change you submit. You must be able to explain and defend any code you contribute.

## Submit

- Push to a topic branch in your fork
- Initiate a pull request
- Describe what the change does and why

## Contact

- Questions: Open a GitHub Discussion
- Bugs: Open a GitHub Issue
- Security: security@infinispan.org
