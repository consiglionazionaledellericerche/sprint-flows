#Immagine che mi serve per installare i font
FROM node:8-alpine as nodejs-builder
LABEL maintainer="cironepa@gmail.com"

# Activiti genera i diagrammi, e' importante installare i font (legato ad application.yml)
RUN apk update
RUN apk add ttf-dejavu


#Immagine da cui prendo le librerie che non ci sono nella distroless
FROM openjdk:8-jdk-slim as jdk


#Jmmagine distroless java dove gira effettivamente il jar di spring-flows
FROM gcr.io/distroless/java

LABEL maintainer="cironepa@gmail.com"

# Copy over dependancies for Apache FOP, which are lacking from the JRE supplied in gcr.io/distroless/java
# Make sure java versions match both in JDK image and the distroless image
COPY --from=jdk /usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/lib/amd64/libfontmanager.so /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64/
COPY --from=jdk /usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/lib/amd64/libjavalcms.so /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64/
COPY --from=jdk /usr/lib/x86_64-linux-gnu/liblcms2.so.2.0.8 /usr/lib/x86_64-linux-gnu/liblcms2.so.2
COPY --from=jdk /usr/lib/x86_64-linux-gnu/libfreetype.so.6.12.3 /usr/lib/x86_64-linux-gnu/libfreetype.so.6
COPY --from=jdk /usr/lib/x86_64-linux-gnu/libpng16.so.16.28.0 /usr/lib/x86_64-linux-gnu/libpng16.so.16
# Copy over dependancies for Apache Batik (used by Apache FOP to handle SVG rendering)
COPY --from=jdk /usr/lib/x86_64-linux-gnu/libfontconfig.so.1.8.0 /usr/lib/x86_64-linux-gnu/libfontconfig.so.1
COPY --from=jdk /usr/share/fontconfig /usr/share/fontconfig
COPY --from=jdk /usr/share/fonts/truetype/dejavu /usr/share/fonts/truetype/dejavu
COPY --from=jdk /lib/x86_64-linux-gnu/libexpat.so.1 /lib/x86_64-linux-gnu/libexpat.so.1
#COPY --from=jdk /etc/fonts /etc/fonts

EXPOSE 8080 5701/udp

ADD app.war /app/app.war
WORKDIR /app

CMD ["-Djava.security.egd=file:/dev/./urandom", "app.war"]
