FROM scolagreco/alpine-openjdk:v3.9.2

VOLUME /tmp

RUN apk add --update --no-cache tzdata curl
RUN cp /usr/share/zoneinfo/Europe/Rome /etc/localtime
RUN echo "Europe/Rome" >  /etc/timezone

RUN apk add --update --no-cache ttf-dejavu curl
ARG JAR_FILE=./target/app.war
ADD ${JAR_FILE} /opt/app.war

EXPOSE 8080
EXPOSE 8899

WORKDIR /opt/

# https://spring.io/guides/gs/spring-boot-docker/#_containerize_it
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8899", "-Dmaven.wagon.http.ssl.insecure=true", "-Dmaven.wagon.http.ssl.allowall=true"-jar","/opt/app.war"]
