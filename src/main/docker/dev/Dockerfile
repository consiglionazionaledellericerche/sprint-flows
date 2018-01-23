FROM java:openjdk-8-jre-alpine

LABEL maintainer "marcinireneusz.trycz@cnr.it"

ENV JHIPSTER_SLEEP 0

# Activiti genera i diagrammi, e' importante installare i font (legato ad aopplication.yml)
RUN apk update
RUN apk add ttf-dejavu


# add directly the war
ADD app.war /app.war

RUN sh -c 'touch /app.war'
VOLUME /tmp
EXPOSE 8080 5701/udp
CMD echo "The application will start in ${JHIPSTER_SLEEP}s..." && \
    sleep ${JHIPSTER_SLEEP} && \
# tolto -Dspring.profiles.active=swagger,test perché va nello yml di docker
#    java -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=swagger,test  -jar /app.war

    java -Djava.security.egd=file:/dev/./urandom  -Dspring.profiles.active=demo,cnr -jar /app.war
    