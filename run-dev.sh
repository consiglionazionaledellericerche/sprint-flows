#!/usr/bin/env bash


mvn clean spring-boot:run \
	-Dserver.port=8080 \
	-Dspring.profiles.active=dev,oiv,swagger 
