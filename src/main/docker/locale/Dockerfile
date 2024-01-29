#FROM scolagreco/alpine-openjdk:v3.9.2
#FROM anapsix/alpine-java:jdk8
#FROM eclipse-temurin:8-jdk
#FROM openjdk:8-jdk-alpine
#FROM amazoncorretto:8-alpine-jdk
#FROM amazoncorretto:8-alpine3.18
#FROM amazoncorretto:8-al2-jdk
#FROM bellsoft/liberica-openjdk-alpine-musl:8
FROM bellsoft/liberica-runtime-container:jdk-8-cds-slim-musl


#RUN apk del openssl
VOLUME /tmp
#
#RUN apk add --no-cache tzdata
#RUN #apk add --no-cache ttf-dejavu
#
#RUN cp /usr/share/zoneinfo/Europe/Rome /etc/localtime
#RUN echo "Europe/Rome" >  /etc/timezone
#RUN export TZ='Europe/Rome'

#BUONA
#ADD ./target/app.war /opt/app.war
#locale
ADD app.war /opt/app.war
#RUN curl -L "http://maven.si.cnr.it/service/local/artifact/maven/redirect?r=releases&v=LATEST&g=it.cnr.whistleblower&a=whistleblower&e=war" --output /opt/app.war

EXPOSE 8080
#EXPOSE 8080 8899

WORKDIR /opt/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/app.war"]
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8899", "-jar","/opt/app.war"]
