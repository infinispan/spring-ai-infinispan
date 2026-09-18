# Spring AI Infinispan

Spring AI integration for [Infinispan](https://infinispan.org/), providing vector store and chat memory repository implementations.

Infinispan is an open-source in-memory distributed database that offers flexible deployment options and robust capabilities for storing, managing, and processing data.

## Modules

| Module | Description |
|--------|-------------|
| `spring-ai-infinispan-store` | Vector store implementation using Infinispan |
| `spring-ai-autoconfigure-vector-store-infinispan` | Spring Boot auto-configuration for the vector store |
| `spring-ai-model-chat-memory-repository-infinispan` | Chat memory repository implementation using Infinispan |
| `spring-ai-autoconfigure-model-chat-memory-repository-infinispan` | Spring Boot auto-configuration for the chat memory repository |

## Getting Started

### Vector Store

Add the auto-configuration dependency to your Spring Boot project:

```xml
<dependency>
    <groupId>org.infinispan</groupId>
    <artifactId>spring-ai-autoconfigure-vector-store-infinispan</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Configure Infinispan connection in `application.properties`:

```properties
infinispan.remote.server-list=localhost:11222
infinispan.remote.auth-username=admin
infinispan.remote.auth-password=password
```

### Chat Memory Repository

```xml
<dependency>
    <groupId>org.infinispan</groupId>
    <artifactId>spring-ai-autoconfigure-model-chat-memory-repository-infinispan</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Building

```bash
mvn install -DskipTests
```

## Running Tests

Integration tests require Docker (Testcontainers):

```bash
mvn verify
```

## Documentation

- [Infinispan Documentation](https://infinispan.org/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Infinispan Simple Tutorials](https://github.com/infinispan/infinispan-simple-tutorials)

## Contributing

For contributing guidelines please refer to [CONTRIBUTING.md](CONTRIBUTING.md). All contributions are subject to the Developer Certificate of Origin (DCO).
