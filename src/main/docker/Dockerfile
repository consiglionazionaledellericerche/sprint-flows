FROM scolagreco/alpine-openjdk:v3.9.2

VOLUME /tmp

RUN cp /usr/share/zoneinfo/Europe/Rome /etc/localtime
RUN echo "Europe/Rome" >  /etc/timezone

RUN apk add --update --no-cache ttf-dejavu curl
ADD ./target/app.war /opt/app.war
#RUN curl -L "http://maven.si.cnr.it/service/local/artifact/maven/redirect?r=public&g=it.cnr.si.flows&a=sprint-flows&v=LATEST&e=war" --output /opt/app.war

EXPOSE 8080
WORKDIR /opt/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar","/opt/app.war" ]


