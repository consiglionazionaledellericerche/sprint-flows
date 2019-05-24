#!/usr/bin/env bash



docker logs $(docker ps -q -f 'name=democnr_sprint-flows-demo_cnr_1') >& ../../../../logs-spring.log
docker logs $(docker ps -q -f 'name=democnr_sprint-flows-demo_cnr-pg_1') >& ../../../../logs-DB.log
docker-compose logs >& ../../../../logs-ALL.log


