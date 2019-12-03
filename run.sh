#!/usr/bin/env bash

mvn clean spring-boot:run -Pprod -DskipTests -Dspring.profiles.active=native,showcase,test,swagger

