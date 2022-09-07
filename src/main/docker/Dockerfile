FROM scolagreco/alpine-openjdk:v3.9.2

VOLUME /tmp

RUN apk add --update --no-cache tzdata curl
RUN cp /usr/share/zoneinfo/Europe/Rome /etc/localtime
RUN echo "Europe/Rome" >  /etc/timezone
RUN export TZ='Europe/Rome'

RUN apk add --update --no-cache ttf-dejavu curl
ADD ./target/app.war /opt/app.war
#RUN curl -L "http://maven.si.cnr.it/service/local/artifact/maven/redirect?r=public&g=it.cnr.si.flows&a=sprint-flows&v=LATEST&e=war" --output /opt/app.war

EXPOSE 8080
WORKDIR /opt/

RUN apk add openssl
RUN openssl s_client -showcerts -connect missioni.cnr.it:443 </dev/null 2>/dev/null|openssl x509 -outform PEM > missioni.cnr.it.crt
RUN keytool -import -trustcacerts -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit -noprompt -alias missioni.cnr.it -file /opt/missioni.cnr.it.crt

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar","/opt/app.war" ]


