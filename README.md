# Ladon S3 Server

Ladon S3 Server is a Java/Jakarta EE based S3-compatible server implementation.
It provides a servlet-based S3 API core, repository implementations and starter
modules for different runtime environments.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer

## Modules

| Module | Description |
|---|---|
| `ladon-s3-server` | Core S3 servlet, request handling, authentication and shared API classes |
| `ladon-s3-server-fs` | File-system based repository implementation |
| `ladon-s3-server-vfs` | Virtual file-system repository with versioning, deduplication and garbage collection |
| `ladon-s3-server-boot` | Spring Boot application starter |
| `ladon-s3-server-jetty` | Embedded Jetty example/server |
| `ladon-s3-server-osgi-starter` | OSGi servlet activator/starter |

## Build

Build the complete Maven reactor from the repository root:

```bash
mvn clean verify
```

To build only the OSGi starter and all required upstream modules:

```bash
mvn clean verify -pl ladon-s3-server-osgi-starter -am
```

## Run with Spring Boot

Build the project and run the Spring Boot starter:

```bash
mvn clean package
java -jar ladon-s3-server-boot/target/ladon-s3-server-boot-2.6.0.jar
```

By default, the S3 API is exposed below:

```text
/api/s3
```

## Run with Jetty

Build the Jetty module:

```bash
mvn clean package -pl ladon-s3-server-jetty -am
```

Then run the generated jar:

```bash
java -jar ladon-s3-server-jetty/target/ladon-s3-server-jetty-2.6.0.jar
```

## OSGi Tests

The OSGi functional test starts an embedded Apache Felix framework and installs
the Ladon bundles from the sibling module `target/` folders.

Run it from the root reactor so the required bundles are built first:

```bash
mvn clean verify -pl ladon-s3-server-osgi-starter -am
```

## License

Copyright (C) 2026 Mind Consulting

Free for private use, easy commercial licensing available.

<a href="https://ladon.org/"><img src="https://ladon.org/img/logo_no_bg.png" height="100" width="250"></a>
