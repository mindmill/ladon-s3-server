FROM openjdk:8-jre
MAINTAINER Ralf Ulrich <Ralf.Ulrich@Mind-Consulting.de>

ENTRYPOINT ["java", "-jar", "/usr/share/ladon/ladon.jar"]

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/ladon/ladon.jar
EXPOSE 8080/tcp
