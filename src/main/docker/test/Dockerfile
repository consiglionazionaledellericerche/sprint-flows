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

RUN apk add openssl
RUN openssl s_client -showcerts -connect missioni.cnr.it:443 </dev/null 2>/dev/null|openssl x509 -outform PEM > missioni.cnr.it.crt
RUN keytool -import -trustcacerts -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit -noprompt -alias missioni.cnr.it -file /opt/missioni.cnr.it.crt

# https://spring.io/guides/gs/spring-boot-docker/#_containerize_it
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8899", "-jar","/opt/app.war"]
