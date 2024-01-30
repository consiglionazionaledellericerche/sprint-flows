#!/usr/bin/env bash

#per bloccare lo script in caso di errore
set -e

RED='\033[0;31m'
NC='\033[0m'

echo -e "\n################################## Buildo il war #################################################\n"
cd ../../../../
mvn clean install -Pprod -DskipTests   #comando eseguito da Jenckins
#mvn clean install -DskipTests

echo -e "\n################################## Copio il war nella folder di docker #################################################\n"
cd src/main/docker/locale
cp ../../../../target/app.war app.war

echo -e "\n################################## RIMUOVO sprint-flows-locale #################################################\n"
docker rmi -f sprint-flows-locale

echo -e "\n################################## BUILDING sprint-flows-locale #################################################\n"
#docker build --no-cache -t sprint-flows-locale -f Dockerfile .
docker build --no-cache -t sprint-flows-locale -f Dockerfile_amazonCorretto .


echo -e "\n################################## RUN sprint-flows-locale #################################################\n"

#ricrea i container e rebuilda le immagini
docker-compose up --force-recreate --build --remove-orphans

echo -e "\n########### IMPORTANTE PER ELIMINARE IL DB usare ${RED}docker-compose down -v${NC} ###############"
echo -e "\n########### PER VEDERE LE LOG DEI CONTAINER, USARE ${RED}docker-compose logs -f ${NC} ###############\n"


