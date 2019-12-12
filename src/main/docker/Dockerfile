FROM scolagreco/alpine-openjdk:v3.9.2

VOLUME /tmp

RUN apk add --update --no-cache ttf-dejavu curl

EXPOSE 8080

WORKDIR /opt/

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar","/opt/app.war", "--spring.profiles.active=native,showcase,test,swagger" ]

RUN curl -L "http://maven.si.cnr.it/service/local/artifact/maven/redirect?r=public&g=it.cnr.si.flows&a=sprint-flows&v=LATEST&e=war" --output /opt/app.war

