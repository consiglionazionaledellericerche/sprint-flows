FROM scolagreco/alpine-openjdk:v3.9.2

VOLUME /tmp

RUN apk add --update --no-cache ttf-dejavu

EXPOSE 8080
WORKDIR /opt/

# https://spring.io/guides/gs/spring-boot-docker/#_containerize_it
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/opt/app.war"]

RUN curl -L "http://maven.si.cnr.it/service/local/artifact/maven/redirect?r=public&g=it.cnr.si.flows&a=sprint-flows&v=LATEST&e=war" --output /opt/app.war
