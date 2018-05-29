FROM java:openjdk-8-jre-alpine

LABEL maintainer="cironepa@gmail.com"

ENV JHIPSTER_SLEEP 0

# Activiti genera i diagrammi, e' importante installare i font (legato ad application.yml)
RUN apk update
RUN apk add ttf-dejavu


# add directly the war
ADD app.war /app.war

RUN sh -c 'touch /app.war'
VOLUME /tmp
EXPOSE 8080 5701/udp
CMD echo "The application will start in ${JHIPSTER_SLEEP}s..." && \
    sleep ${JHIPSTER_SLEEP} && \
    java -Djava.security.egd=file:/dev/./urandom -jar /app.war
