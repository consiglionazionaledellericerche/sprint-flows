#!/usr/bin/env bash


mvn clean spring-boot:run \
	-Dserver.port=8080 \
	-Dspring.profiles.active=demo,oiv,swagger \
	-Dspring.datasource.url= \ # es. jdbc:postgresql://localhost:5432/flows
	-Dspring.datasource.username=activiti \
	-Dspring.datasource.password=activitipw \
	-Doiv.baseurl=http://elenco-oiv-test.si.cnr.it \
	-Doiv.psw= \
	-Doiv.mail.mail.user= \
	-Doiv.mail.mail.password=
