[![Build Status](https://travis-ci.org/mindmill/ladon-s3-server.svg?branch=master)](https://travis-ci.org/mindmill/ladon-s3-server)

# Ladon S3 Server
Build your own S3 Server and keep your data safe!

The Ladon S3 Server is built using Java 8-13 and Maven
### Jetty Example

If you prefer to run it with Jetty have a look at the [Jetty example](./ladon-s3-server-jetty/src/main/java/de/mc/ladon/s3server/jetty/S3JettyServer.java )

```bash
    mvn package && java -jar ladon-s3-server-jetty/target/ladon-s3-server-jetty-2.2.0.jar
```

### Spring Boot:

```bash
    mvn package && java -jar ladon-s3-server-boot/target/ladon-s3-server-boot-2.2.0.jar
```
Or run Docker:
```bash
mvn package &&  docker run -i --network=host  mindconsulting/ladon:2.2.0
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
For other configuration options have a look at the [application.properties](./ladon-s3-server-boot/src/main/resources/application.properties ) 


Supported so far is only access via REST API, no ACL, no policies, no torrent ....

To see what you can build with it have a look at  [Ladon Data Center Edition](https://ladon.org)  
### License
Copyright (C) 2021 Mind Consulting

Free for private use, easy commercial licensing available [here](https://elopage.com/s/mind/ladon-s3-server/payment?locale=en)

<a href="https://ladon.org/"><img src="https://ladon.org/img/logo_no_bg.png" height="100" width="250" ></a>

