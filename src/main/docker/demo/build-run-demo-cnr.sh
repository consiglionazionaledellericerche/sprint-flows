#!/usr/bin/env bash
echo -e "\n################################## Buildo il war #################################################\n"
cd ../../../../
mvn clean install -DskipTests
cd src/main/docker/demo

echo -e "\n################################## Copio il war nella folder di docker #################################################\n"
cp ../../../../target/sprint-flows*.war app.war


if [[ $(docker ps -a -q -f name=sprint-flows*) != "" ]]; then
	echo -e "\n################################## RIMUOVO il container /sprint-flows #################################################\n"
    docker rm -f $(docker ps -a -q -f name=sprint-flows*)
else
	echo -e "\n################################## NON ci sono container da rimuovere #################################################\n"
fi



echo -e "\n################################## BUILDING sprint-flows-demo #################################################\n"
docker build --no-cache -t sprint-flows-demo -f Dockerfile .


#echo -e "\n################################## RUNNING sprint-flows #################################################\n"
#docker run -p 8080:8080 --name sprint-flows-dev sprint-flows-dev

#todo: customizzarsi il path di sprint-flows-demo
cd /home/cirone/Scrivania/git/docker-compose-demo/sprint-flows-demo-cnr

docker-compose up


