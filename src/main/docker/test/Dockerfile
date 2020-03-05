FROM scolagreco/alpine-openjdk:v3.9.2

VOLUME /tmp

RUN apk add --update --no-cache ttf-dejavu curl
ARG JAR_FILE=./target/app.war
ADD ${JAR_FILE} /opt/app.war

EXPOSE 8080
EXPOSE 8899

WORKDIR /opt/

# https://spring.io/guides/gs/spring-boot-docker/#_containerize_it
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8899", "-jar","/opt/app.war"]
