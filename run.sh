#!/usr/bin/env bash

npm install
bower install
mvn clean compile package -DskipTests
java -jar target/sprint-flows-0.2.1-SNAPSHOT.war --spring.profiles.active=dev,oiv,swagger

# se e' disponibile un database postgres usare invece:

#java -jar target/sprint-flows-0.2.1-SNAPSHOT.war  \
#  --spring.profiles.active=demo,oiv,swagger \
#	-Dspring.datasource.url=jdbc:postgresql://localhost:5432/flows \ # es. jdbc:postgresql://localhost:5432/flows
#	-Dspring.datasource.username=activiti \
#	-Dspring.datasource.password=activitipw \
#	-Doiv.baseurl=http://elenco-oiv-test.si.cnr.it \
#	-Doiv.psw= \
#	-Doiv.mail.mail.user= \
#	-Doiv.mail.mail.password=
