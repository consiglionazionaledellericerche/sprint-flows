FROM scolagreco/alpine-jdk-springboot
LABEL maintainer="marcinireneusz.trycz@cnr.it"

RUN apk update
RUN apk add ttf-dejavu

EXPOSE 8080 5701/udp

ADD app.war /app/app.war
WORKDIR /app

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.war"]
