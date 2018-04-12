#!/bin/bash

docker network create casino
docker pull cassandra:latest

CASSANDRA=$(docker run --rm --name cassandra_db -d --net casino cassandra:latest)

echo "Waiting for Cassandra is going live."
while :
do
	response=$((docker exec $CASSANDRA nodetool status | grep UN ) 2>&1)
	if [[ $response = *"UN"* ]]
	  then
	    echo "Cassandra is ready:  " $response
	    break
	  fi 
	  sleep 3
done

docker pull kroler/wallet-webservice:latest
docker run -p "8080:8080" -d --rm --name walletservice --net casino kroler/wallet-webservice

docker pull kroler/game-webservice:latest
docker run -p "8081:8081" -d -p "2551:2551" --rm --name gameservice --net casino kroler/game-webservice -Dservice.withdraw.host=walletservice:8080 -Dakka.cluster.seed-host="gameservice" -Dakka.remote.netty.tcp.hostname="gameservice"
docker run -p "8082:8081" -d -p "2552:2552" --rm --name gameservice2 --net casino kroler/game-webservice -Dservice.withdraw.host=walletservice:8080 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-host="gameservice" -Dakka.remote.netty.tcp.hostname="gameservice2" 

echo "Now all services has been started. Open Postman to test it!"

 