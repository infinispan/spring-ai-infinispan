# Coding Instructions

## Tech Stack
* **Java Version:** 17
* **Build Tool:** Maven (Multi-module, aggregator POM — each module is self-contained)
* **Key Frameworks:** Spring Boot 4.1, Spring AI 2.0.1, Infinispan 16.0
* **Test Frameworks:** JUnit 5, AssertJ, Testcontainers, Awaitility

## Project Architecture

* **spring-ai-infinispan-store:** Core vector store implementation. Uses Infinispan Hot Rod client with Protobuf marshalling for storing and querying document embeddings. Implements `AbstractObservationVectorStore`.
* **spring-ai-autoconfigure-vector-store-infinispan:** Spring Boot auto-configuration for the vector store. Properties prefix: `spring.ai.vectorstore.infinispan`.
* **spring-ai-model-chat-memory-repository-infinispan:** Chat memory repository implementation. Uses Infinispan Hot Rod client for storing conversation messages. Implements `ChatMemoryRepository`.
* **spring-ai-autoconfigure-model-chat-memory-repository-infinispan:** Spring Boot auto-configuration for the chat memory repository. Properties prefix: `spring.ai.chat.memory.repository.infinispan`.

## Key Patterns

### Protobuf Marshalling
Both the vector store and chat memory use Infinispan's ProtoStream marshalling with programmatically generated schemas. Each module defines:
- A record/class for the stored data (e.g., `SpringAiInfinispanItem`, `ChatMemoryItem`)
- A `MessageMarshaller` implementation for serialization/deserialization
- Schema registration in `afterPropertiesSet()`

### Builder Pattern
All main classes use the Builder pattern following Spring AI conventions (`AbstractVectorStoreBuilder` for the vector store, custom builder for chat memory).

### Ickle Query Language
Both modules use Infinispan's Ickle query language for similarity search and data retrieval.

### Filter Expression Converter
The `InfinispanFilterExpressionConverter` translates Spring AI filter expressions into Ickle query predicates with metadata join clauses. It includes escaping for Ickle injection prevention (ported from langchain4j).

## Common Build Commands
* **Full build (skip tests):** `mvn install -DskipTests`
* **Build a single module:** `mvn install -pl spring-ai-infinispan-store -DskipTests`
* **Run tests in a module:** `mvn verify -pl spring-ai-infinispan-store`
* **Run a single test class:** `mvn verify -pl spring-ai-infinispan-store -Dtest=InfinispanFilterExpressionConverterTest`
* **Run integration tests:** `mvn verify -pl spring-ai-infinispan-store -Dit.test=InfinispanVectorStoreIT`

## Dependencies
* Spring AI dependencies come from `spring-ai-bom` — do not specify versions for Spring AI artifacts
* Spring Boot dependencies come from `spring-boot-dependencies` — do not specify versions for Spring Boot artifacts
* Infinispan dependencies come from `infinispan-bom`
* Spring milestone and snapshot repositories are configured in each module POM

## Development Standards
* **Javadocs:** Required for public API classes and methods.
* **Coding style:** Follow Spring AI conventions (formatting, import ordering).
* **Jackson:** This project uses Jackson 3 (`tools.jackson` packages), not Jackson 2 (`com.fasterxml.jackson`).
* **VectorStoreProvider:** Since `INFINISPAN` is not in Spring AI's `VectorStoreProvider` enum, use the string literal `"infinispan"` directly.

## Related Projects
* **Infinispan:** The Infinispan source code is in ../infinispan
* **Spring AI:** The Spring AI source code is in ../spring-ai
* **Azure Cosmos reference:** The Azure Cosmos DB standalone repo (reference for project structure) is in ../azure-cosmos
