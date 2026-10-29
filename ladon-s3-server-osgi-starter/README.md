# Ladon S3 Server OSGi Servlet Activator

This module provides the OSGi starter bundle for Ladon S3 Server.

It registers the Ladon `S3Servlet` in an OSGi HTTP Whiteboard compatible
container and exposes the S3-compatible API below:

```text
/api/s3/*
```

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- An OSGi runtime with Servlet Whiteboard support

## Build

Build this module together with its required upstream modules from the project
root:

```bash
mvn clean verify -pl ladon-s3-server-osgi-starter -am
```

The generated bundle can be found in:

```text
ladon-s3-server-osgi-starter/target/
```

## OSGi Functional Test

This module contains an OSGi functional test that starts an embedded Apache Felix
framework, installs the Ladon bundles and verifies bundle resolution, service
wiring and the S3 repository integration.

Run it from the project root:

```bash
mvn clean verify -pl ladon-s3-server-osgi-starter -am
```

## Runtime Behavior

The starter bundle looks up an available `S3Repository` OSGi service and wires it
into the `S3Servlet`.

The servlet is registered with the HTTP Whiteboard pattern:

```text
/api/s3/*
```

## Related Modules

| Module | Description |
|---|---|
| `ladon-s3-server` | Core S3 servlet and shared API classes |
| `ladon-s3-server-fs` | File-system based repository implementation |
| `ladon-s3-server-osgi-starter` | OSGi servlet activator |

## License

Copyright (C) 2026 Mind Consulting

Free for private use, easy commercial licensing available.

<a href="https://ladon.org/"><img src="https://ladon.org/img/logo_no_bg.png" height="100" width="250"></a>
