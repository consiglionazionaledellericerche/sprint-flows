# Immagine da cui prendo le librerie che non ci sono nella distroless
FROM openjdk:8-jdk-alpine
LABEL maintainer="cironepa@gmail.com"

RUN apk update
RUN apk add ttf-dejavu

EXPOSE 8080 5701/udp

ADD app.war /app/app.war
WORKDIR /app

CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.war"]
#CMD ["-Djava.security.egd=file:/dev/./urandom -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5701", "app.war"]
#CMD ["app.war"]
