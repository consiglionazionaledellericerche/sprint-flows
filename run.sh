#!/usr/bin/env bash

mvn clean compile package -Pprod -DskipTests -Dspring.profiles.active=native,showcase,test,swagger -U && \
docker build --file src/main/docker/showcase/Dockerfile -t docker.si.cnr.it/sprint-flows-showcase . && \
docker run -p 8080:8080 docker.si.cnr.it/sprint-flows-showcase


