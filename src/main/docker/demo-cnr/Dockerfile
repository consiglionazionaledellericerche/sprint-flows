FROM scolagreco/alpine-openjdk:v3.9.2

VOLUME /tmp

ARG JAR_FILE=./app.war
ADD ${JAR_FILE} /opt/app.war
RUN apk add --update --no-cache ttf-dejavu

EXPOSE 8080

WORKDIR /opt/

# https://spring.io/guides/gs/spring-boot-docker/#_containerize_it
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar","/opt/app.war"]