# Ladon S3 Spring Boot Starter

This module provides the Spring Boot starter application for Ladon S3 Server.

It runs the Ladon `S3Servlet` with embedded Tomcat and exposes an S3-compatible
API.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer

## Build

Build this module together with its required upstream modules from the project
root:

```bash
mvn clean package -pl ladon-s3-server-boot -am
```

The generated application JAR can be found in:

```text
ladon-s3-server-boot/target/
```

## Run

Start the Spring Boot application:

```bash
java -jar ladon-s3-server-boot/target/ladon-s3-server-boot-2.6.0.jar
```

By default, the S3 API is exposed below:

```text
/api/s3
```

## Configuration

The Spring Boot starter uses the file-system repository by default.

Example configuration:

```properties
s3server.baseUrl=/api/s3
s3server.threadPoolSize=5
s3server.securityEnabled=true
s3server.loggingEnabled=false

s3server.fsrepo.root=${user.home}/.s3server
s3server.fsrepo.secret=change-me
```

## Repository

If no custom `S3Repository` bean is provided, the application creates a
file-system based repository.

To use a custom repository, provide your own Spring bean implementing:

```java
de.mc.ladon.s3server.repository.api.S3Repository
```

## Related Modules

| Module | Description |
|---|---|
| `ladon-s3-server` | Core S3 servlet and shared API classes |
| `ladon-s3-server-fs` | File-system based repository implementation |
| `ladon-s3-server-boot` | Spring Boot application starter |

## License

Copyright (C) 2026 Mind Consulting

Free for private use, easy commercial licensing available.

<a href="https://ladon.org/"><img src="https://ladon.org/img/logo_no_bg.png" height="100" width="250"></a>
