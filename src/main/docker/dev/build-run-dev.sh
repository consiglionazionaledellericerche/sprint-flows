#!/usr/bin/env bash
echo "################################## Buildo il war #################################################!"
cd ../../../../
mvn clean install -Pdev -DskipTests
cd src/main/docker/demo


echo "################################## Copio il war nella folder di docker #################################################!"
cp target/sprint-flows*.war src/main/docker/dev/app.war



if [ $(docker ps -a -q -f name=sprint-flows*) ]; then
	echo "################################## RIMUOVO il container /sprint-flows #################################################!"
    docker rm -f $(docker ps -a -q -f name=sprint-flows*)
else
	echo "################################## NON ci sono container da rimuovere #################################################!"
fi


echo "################################## BUILDING sprint-flows #################################################!"
docker build --no-cache -t sprint-flows-dev -f Dockerfile .


#echo "################################## RUNNING sprint-flows #################################################!"
#docker run -p 8080:8080 --name sprint-flows-dev sprint-flows-dev

#todo: customizzarsi il path di sprint-flows-demo
cd /home/cirone/Scrivania/git/docker-compose-demo/sprint-flows-dev
docker-compose up


