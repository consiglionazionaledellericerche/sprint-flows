# Immagine da cui prendo le librerie che non ci sono nella distroless
FROM openjdk:8-jdk-slim

# Immagine distroless java dove gira effettivamente il jar di spring-flows
FROM gcr.io/distroless/java:latest
LABEL maintainer="cironepa@gmail.com"

#todo: provare ad avviare i processi con un'utenza che non suia root
#USER nonroot


# Copy over dependancies for Apache Batik (used by Apache FOP to handle SVG rendering)

#COPY usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf /usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf
#COPY usr/share/fonts/truetype/dejavu/DejaVuSans.ttf /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf
#COPY usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf
#COPY usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf /usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf
#COPY usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf /usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf
#COPY usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf /usr/share/fonts/truetype/dejavu/dejavu/DejaVuSerif.ttf
## Copy over dependancies for Apache FOP, which are lacking from the JRE supplied in gcr.io/distroless/java
## Make sure java versions match both in JDK image and the distroless image
COPY usr/lib/x86_64-linux-gnu/libfontconfig.so.1.8.0 /usr/lib/x86_64-linux-gnu/libfontconfig.so.1
#COPY usr/lib/x86_64-linux-gnu/libfreetype.so.6.12.3 /usr/lib/x86_64-linux-gnu/libfreetype.so.6
#COPY usr/lib/x86_64-linux-gnu/libpng16.so.16.28.0 /usr/lib/x86_64-linux-gnu/libpng16.so.16
#COPY lib/x86_64-linux-gnu/libexpat.so.1 /lib/x86_64-linux-gnu/libexpat.so.1


EXPOSE 8080 5701/udp

ADD app.war /app/app.war
WORKDIR /app

CMD ["-Djava.security.egd=file:/dev/./urandom -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5701", "app.war"]
#CMD ["app.war"]
