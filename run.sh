#!/usr/bin/env bash

mvn clean compile package spring-boot:run -Pprod -DskipTests -Dspring.profiles.active=native,showcase,test,swagger

cp ./target/*.war ./target/app.war

docker build --file src/main/docker/test/Dockerfile -t docker.si.cnr.it/sprint-flows-showcase .

docker run docker.si.cnr.it/sprint-flows-showcase


