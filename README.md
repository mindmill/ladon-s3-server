[![CI](https://github.com/mindmill/ladon-s3-server/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/mindmill/ladon-s3-server/actions/workflows/ci.yml)

# Ladon S3 Server
Build your own S3 Server and keep your data safe!

The Ladon S3 Server is built using Java 21 and Maven

Also OSGI is supported.
### Jetty Example

If you prefer to run it with Jetty have a look at the [Jetty example](./ladon-s3-server-jetty/src/main/java/de/mc/ladon/s3server/jetty/S3JettyServer.java )

```bash
    mvn package && java -jar ladon-s3-server-jetty/target/ladon-s3-server-jetty-{version}.jar
```

### Spring Boot:

```bash
    mvn package && java -jar ladon-s3-server-boot/target/ladon-s3-server-boot-{version}.jar
```

### Example credentials

```java
  AWSCredentials credentials = new BasicAWSCredentials(
                "rHUYeAk58Ilhg6iUEFtr",
                "IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu");
```
### Customize:
The core functions of S3 are mapped to a class called S3Repository.
All you have to do is provide a bean of this type.
```java
    @Bean
    S3Repository s3Repository() {
        return new YourCustomRepository();
    }

```

The request base path for the controller is configured with a property:
```properties
s3server.baseUrl=/api/s3
```
For a quick start there is an example for a file system storage added.
```java
public class FSRepository {...}
```
If you want to use it, you have to specify the root directory where it will store the data.
Default is: {user.home}/.s3server

```properties
s3server.fsrepo.root=${user.home}/.s3server
```
For **object versioning, content deduplication and garbage collection** there is a second
repository built on a content-addressed layout — `VFSRepository` (module `ladon-s3-server-vfs`).
Register it as the (primary) `S3Repository` bean and point it at a data root:

```java
@Bean
@Primary
S3Repository s3Repository(Path dataRoot) {
    return new VFSRepository(dataRoot);
}
```

 For other configuration options have a look at the [application.properties](./ladon-s3-server-boot/src/main/resources/application.properties ) 


Supported so far is only access via REST API, no ACL, no policies, no torrent ....

## Supported Operations

| Operation | Status |
| --- | --- |
| `PUT/GET/DELETE` Object, `GET` Bucket (list) | ✅ |
| ListObjects **v1** and **v2** (`?list-type=2`, `continuation-token`, `key-count`) | ✅ |
| Multipart-style: SigV4 **chained chunk upload** (streaming payload, signature chain verified) | ✅ |
| **Bulk delete** (`POST /?delete`, per-key `Deleted`/`Error` result) | ✅ |
| `GET` object `response-*` overrides (`response-content-type/-disposition/-encoding/-language/-cache-control/-expires`) | ✅ |
| SigV4 (header **and** presigned URL) and legacy SigV2 (header **and** presigned URL) authentication | ✅ |
| OSGi (bundles, `ladon-s3-server-osgi-starter`) | ✅ |
| **Object versioning** — `?versionId`, `?versions` (list-versions), delete marker, copy, GC — via the **VFS repository** (`ladon-s3-server-vfs`) | ✅ |

## Limitations

Known, deliberate limitations (2.6.0):

- **Multipart Upload API** (`POST /?uploads`, `PUT /?partNumber=`, `POST /?uploadId=`) is
  **not** implemented — the SigV4 *streaming* (chunked) payload is, but the S3 multipart
  *object* workflow is not.
- **Browser POST-policy** uploads (`POST /bucket?policy=…`) are not implemented.
- **Object versioning** (`?versioning=`, `?versionId=`, list-versions) is **not part of the
  core `S3Repository` interface** (the file-system repository has no versioning). It **is**
  provided by the additional **VFS repository** (`ladon-s3-server-vfs`): content-addressed,
  deduplicated blobs + full AWS versioning semantics (versions, delete markers, `?versionId`,
  list-versions, garbage collection). Plug it in as the `@Primary` `S3Repository` bean.
- **Versioning-Configuration end-points** (`PUT/GET ?versioning`) are not implemented —
  versioning is always on in the VFS repository (no switch to enable/disable).
- **ACLs / bucket policies / requesters / torrent / website** sub-resources are not supported.
- Authentication is per user key, no fine-grained ACL evaluation.

## Build & Test

```bash
mvn -B clean verify        # all modules, unit + OSGi functional test (Apache Felix)
```

To see what you can build with it have a look at  [Ladon Data Center Edition](https://ladon.org)  
### License
Copyright (C) 2026 Mind Consulting

Free for private use, easy commercial licensing available [here](https://elopage.com/s/mind/ladon-s3-server/payment?locale=en)

<a href="https://ladon.org/"><img src="https://ladon.org/img/logo_no_bg.png" height="100" width="250" ></a>

