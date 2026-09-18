# Testing Instructions

## Test Frameworks

| Module Type | Framework | Test Naming |
|-------------|-----------|-------------|
| Core modules (store, chat memory) | JUnit 5 | Unit tests: `*Test`, Integration tests: `*IT` |
| Autoconfigure modules | JUnit 5 | Integration tests: `*IT` |

## Test Infrastructure

### Testcontainers
All integration tests use Testcontainers with the Infinispan container image. The container version matches the Infinispan client version:

```java
@Container
static InfinispanContainer infinispanContainer = new InfinispanContainer(
    InfinispanContainer.IMAGE_BASENAME + ":" + Version.getVersion());
```

### ApplicationContextRunner
Autoconfigure integration tests use Spring Boot's `ApplicationContextRunner` to test auto-configuration:

```java
new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(InfinispanRemoteAutoConfiguration.class, ...))
    .withUserConfiguration(Config.class)
    .withPropertyValues(...)
    .run(context -> { ... });
```

### Awaitility
Since Infinispan indexing is eventually consistent, use Awaitility to wait for index updates:

```java
Awaitility.await()
    .until(() -> vectorStore.similaritySearch(
        SearchRequest.builder().query("...").topK(1).build()), hasSize(1));
```

## Running Tests

* **Unit tests only:** `mvn test -pl spring-ai-infinispan-store`
* **Integration tests:** `mvn verify -pl spring-ai-infinispan-store` (requires Docker)
* **All tests:** `mvn verify` (requires Docker)
* **Single test class:** `mvn verify -pl spring-ai-infinispan-store -Dtest=InfinispanFilterExpressionConverterTest`
* **Single IT class:** `mvn verify -pl spring-ai-infinispan-store -Dit.test=InfinispanVectorStoreIT`

## Test Configuration

Surefire is configured to exclude `*IT.java` files (integration tests). Failsafe runs integration tests during the `verify` phase.

## Test Data

Vector store integration tests use test data files from the `spring-ai-test` module:
- `classpath:/test/data/spring.ai.txt`
- `classpath:/test/data/time.shelter.txt`
- `classpath:/test/data/great.depression.txt`

The `TransformersEmbeddingModel` is used as the embedding model in tests (no external API calls needed).
