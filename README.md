[![Build Status](https://travis-ci.org/mindmill/ladon-s3-server.svg?branch=master)](https://travis-ci.org/mindmill/ladon-s3-server)

# Ladon S3 Server
Build your own S3 Server and keep your data safe! 

### Installation:
The Ladon S3 Server is built using Java 8 and Maven
```bash
mvn package && java -jar ladon-s3-server-boot/target/ladon-s3-server-boot-{version}.jar
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

To see what you can build with it have a look at  [Ladon Data Center Edition](https://github.com/mindmill/ladon-data-center-edition) where you can see a full Ladon application. 
### License
Copyright (C) 2018 Mind Consulting

Free for private use, easy commercial licensing available

<a href="http://mind-consulting.de/"><img src="http://mind-consulting.de/img/logo_no_bg.png"  height="100" width="250" ></a>

