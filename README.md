## !this project needs more testing and is not production ready yet
# Java S3 Server API
( with Spring Boot )

### Installation:
The Java S3 Server is built using Java 8 and Maven
```bash
mvn package && java -jar target/s3-server-{version}.jar
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
s3server.api.base.url=/api/s3
```
For a quick start there is an example for a file system storage added.
```java
public class FSRepository {...}
```
If you want to use it, you have to specify the root directory where it will store the data.
Default is: {user.home}/.s3server

```properties
s3server.fsrepo.baseurl=${user.home}/.s3server
```

Supported so far are only basic S3 operations, no authentication, no ACL, no policies, no torrent ....

# License
Copyright (C) 2016 Mind Consulting

Free for private use, easy commercial licensing available

<a href="http://mind-consulting.de/"><img src="http://mind-consulting.de/img/logo_no_bg.png"  height="100" width="250" ></a>

