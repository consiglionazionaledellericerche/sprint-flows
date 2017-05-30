FROM java:openjdk-8-jre-alpine

ENV JHIPSTER_SLEEP 0

RUN apk update
RUN apk add fontconfig
RUN apk add ttf-dejavu


# add directly the war
ADD *.war /app.war

RUN sh -c 'touch /app.war'
VOLUME /tmp
EXPOSE 8080 5701/udp
CMD echo "The application will start in ${JHIPSTER_SLEEP}s..." && \
    sleep ${JHIPSTER_SLEEP} && \
    java -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=cnr,swagger,test  -jar /app.war
