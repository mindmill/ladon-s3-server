# Ladon S3 Server Core

This module contains the core library of Ladon S3 Server.

It provides the servlet-based S3-compatible API implementation, shared request
and response model classes, authentication handling and the repository API used
by storage backends.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- Jakarta Servlet API
- Jakarta XML Bind API

## Main Components

| Component | Description |
|---|---|
| `S3Servlet` | Servlet entry point for S3-compatible HTTP requests |
| `S3Repository` | Repository interface implemented by storage backends |
| Authentication | AWS Signature V2/V4 request validation |
| JAXB entities | XML request/response mappings for S3-compatible responses |
| Request/response models | Shared API and implementation classes used across modules |

## Build

Build this module from the project root:

```bash
mvn clean verify -pl ladon-s3-server -am
```

The generated JAR/bundle can be found in:

```text
ladon-s3-server/target/
```

## Usage

The core module is usually used together with a repository implementation such as
`ladon-s3-server-fs` or `ladon-s3-server-vfs`.

A runtime integration creates an `S3Servlet`, injects an implementation of
`S3Repository` and exposes the servlet through Spring Boot, Jetty or an OSGi
container.

## Repository API

Storage backends implement:

```java
de.mc.ladon.s3server.repository.api.S3Repository
```

Available repository modules include:

| Module | Description |
|---|---|
| `ladon-s3-server-fs` | File-system based repository implementation |
| `ladon-s3-server-vfs` | Virtual file-system repository with versioning and deduplication |

## Related Modules

| Module | Description |
|---|---|
| `ladon-s3-server` | Core servlet and shared API classes |
| `ladon-s3-server-fs` | File-system based repository implementation |
| `ladon-s3-server-vfs` | Virtual file-system repository implementation |
| `ladon-s3-server-boot` | Spring Boot application starter |
| `ladon-s3-server-jetty` | Embedded Jetty example/server |
| `ladon-s3-server-osgi-starter` | OSGi servlet activator |

## License

Copyright (C) 2026 Mind Consulting

Free for private use, easy commercial licensing available.

<a href="https://ladon.org/"><img src="https://ladon.org/img/logo_no_bg.png" height="100" width="250"></a>
