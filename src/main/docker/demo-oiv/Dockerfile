# Immagine da cui prendo le librerie che non ci sono nella distroless
FROM openjdk:8-jdk-slim as jdk

# Immagine distroless java dove gira effettivamente il jar di spring-flows
FROM gcr.io/distroless/java
LABEL maintainer="cironepa@gmail.com"

#todo: provare ad avviare i processi con un'utenza che non suia root
#USER nonroot

# Copy over dependancies for Apache FOP, which are lacking from the JRE supplied in gcr.io/distroless/java
# Make sure java versions match both in JDK image and the distroless image
COPY --from=jdk /usr/lib/x86_64-linux-gnu/libfreetype.so.6.12.3 /usr/lib/x86_64-linux-gnu/libfreetype.so.6
COPY --from=jdk /usr/lib/x86_64-linux-gnu/libpng16.so.16.28.0 /usr/lib/x86_64-linux-gnu/libpng16.so.16
# Copy over dependancies for Apache Batik (used by Apache FOP to handle SVG rendering)
COPY --from=jdk /usr/lib/x86_64-linux-gnu/libfontconfig.so.1.8.0 /usr/lib/x86_64-linux-gnu/libfontconfig.so.1
COPY --from=jdk /usr/share/fonts/truetype/dejavu /usr/share/fonts/truetype/dejavu
COPY --from=jdk /lib/x86_64-linux-gnu/libexpat.so.1 /lib/x86_64-linux-gnu/libexpat.so.1

EXPOSE 8080 5701/udp

#ADD app.war /app/app.war
WORKDIR /app

CMD ["-Djava.security.egd=file:/dev/./urandom -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5701", "app.war"]
