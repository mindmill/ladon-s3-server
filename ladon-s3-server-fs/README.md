# Ladon S3 File-System Repository

This module provides the file-system based repository implementation for Ladon S3
Server.

It stores buckets, objects and metadata on the local file system and can be used
as a simple storage backend for the S3-compatible servlet.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer

## Repository Implementation

The main repository implementation is:

```java
de.mc.ladon.s3server.repository.impl.FSRepository
```

It requires a root directory where the repository data is stored.

Default location:

```text
${user.home}/.s3server
```

## Configuration

When used through the Spring Boot starter, configure the storage root with:

```properties
s3server.fsrepo.root=${user.home}/.s3server
```

If encryption support is enabled/configured, also provide the repository secret:

```properties
s3server.fsrepo.secret=change-me
```

## Build

Build this module together with its dependencies from the project root:

```bash
mvn clean verify -pl ladon-s3-server-fs -am
```

The generated bundle/JAR can be found in:

```text
ladon-s3-server-fs/target/
```

## Usage

The repository can be created directly in Java:

```java
import de.mc.ladon.s3server.enc.AESFileEncryptor;
import de.mc.ladon.s3server.repository.api.S3Repository;
import de.mc.ladon.s3server.repository.impl.FSRepository;

import java.nio.charset.StandardCharsets;

S3Repository repository = new FSRepository(
        System.getProperty("user.home") + "/.s3server",
        new AESFileEncryptor("change-me".getBytes(StandardCharsets.UTF_8))
);
```

In the Spring Boot starter this repository is auto-configured when no custom
`S3Repository` bean is provided.

## Related Modules

| Module | Description |
|---|---|
| `ladon-s3-server` | Core S3 servlet and shared API classes |
| `ladon-s3-server-fs` | File-system based repository implementation |
| `ladon-s3-server-boot` | Spring Boot application starter |
| `ladon-s3-server-osgi-starter` | OSGi servlet activator |

## License

Copyright (C) 2026 Mind Consulting

Free for private use, easy commercial licensing available.

<a href="https://ladon.org/"><img src="https://ladon.org/img/logo_no_bg.png" height="100" width="250"></a>
