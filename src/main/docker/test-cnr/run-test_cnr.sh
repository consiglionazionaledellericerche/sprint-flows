#!/usr/bin/env bash
RED='\033[0;31m'
NC='\033[0m'

echo -e "\n################################## Buildo il war #################################################\n"
cd ../../../../
mvn clean install -DskipTests
cd src/main/docker/test-cnr


echo -e "\n################################## Copio il war nella folder di docker #################################################\n"
cp ../../../../target/sprint-flows*.war app.war


echo -e "\n################################## BUILDING sprint-flows-demo #################################################\n"
docker build --no-cache -t sprint-flows-test_cnr -f Dockerfile .


echo -e "\n################################## RUN sprint-flows-test #################################################\n"
docker-compose up -d

echo -e "\n########### IMPORTANTE PER MANTENERE IL DB usare ${RED}docker-compose stop${NC} ###############"
echo -e "\n########### PER VEDERE LE LOG DEI CONTAINER, USARE ${RED}docker-compose logs -f ${NC} ###############\n"


