# Simple webservice for emulating casino's bet placing process

# Operations
[![Run in Postman](https://run.pstmn.io/button.svg)](https://www.getpostman.com/collections/816d9da9b94d63db1221)

## Main features
* Place Bet
* Show Bet's list

## Languages and technologies 
* Scala
* Akka Actors, Akka HTTP, Akka Persistence, Akka Cluster
* Cassandra
* Docker
    
## Usage
The best way to run this webservice is to build and run docker image.

### Installation
* Validate docker installation or install it ([Download here](https://www.docker.com/community-edition))
* Create docker network: `docker network create casino`
* Download Cassandra image using docker: `docker pull cassandra`   
* Run cassandra container with alias name _cassandra_db_: `docker run --name cassandra_db -d --net casino cassandra:latest`
* Clone the repository for Wallet service: `git clone https://github.com/KRoLer/Casino-Wallet-Service.git`
* Navigate to the root folder `cd Casino-Wallet-Service` and run: `sbt docker:publishLocal`
* Run newly created image: `docker run -p "8080:8080" -d --rm --name walletservice --net casino wallet-webservice:0.1`
* Clone this repository: `git clone https://github.com/KRoLer/Casino-Game-Service.git`
* Navigate to the root folder `cd Casino-Game-Service` and run: `sbt docker:publishLocal`
* Run cluster with two instance:
    - `docker run -p "8081:8081" -d -p "2551:2551" --rm --name gameservice --net casino game-webservice:0.1 -Dservice.withdraw.host=walletservice:8080 -Dakka.cluster.seed-host="gameservice" -Dakka.remote.netty.tcp.hostname="gameservice"`
    - `docker run -p "8082:8081" -d -p "2552:2552" --rm --name gameservice2 --net casino game-webservice:0.1 -Dservice.withdraw.host=walletservice:8080 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.seed-host="gameservice" -Dakka.remote.netty.tcp.hostname="gameservice2"`

* To stop all containers use: `docker stop gameservice gameservice2 walletservice cassandra_db`

### Validation
To validate this service locally we recommend to use [Postman](https://www.getpostman.com/apps).
After installation open the [collection link](https://www.getpostman.com/collections/816d9da9b94d63db1221) to import predefined basic calls.

**cURL basic queries**
* Place Bet Node 1 (Port: 8081)
```bash
curl --request POST \
  --url http://localhost:8081/api/v1/bet \
  --header 'Content-Type: application/json' \
  --data '{
	"playerId": 1,
	"gameId": 1,
	"amount": 10
}'
```
* Place Bet Node 2 (Port: 8082)
```bash
curl --request POST \
  --url http://localhost:8082/api/v1/bet \
  --header 'Content-Type: application/json' \
  --data '{
	"playerId": 1,
	"gameId": 2,
	"amount": 100
}'
```
* Get bets Node 1 (Port: 8081)
```bash
curl --request GET \
  --url http://localhost:8081/api/v1/bets/1
```
* Get bets Node 2 (Port: 8082)
```bash
curl --request GET \
  --url http://localhost:8082/api/v1/bets/1
```



