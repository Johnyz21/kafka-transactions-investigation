# Kafka Transaction Investigation

The purpose of this project is to provide the developer with an interactive way to understand how Kafka transactions work
You will have the ability to run and view the output of:

* Transaction Aware Consumers
* Transaction Unaware Consumers
* Transaction Aware Producers
* Transaction Unaware Producers
* Transaction Aware Consumers within Akka Streams
* Transaction Unaware Consumers within Akka Streams

# Setup
To set up the environment the application interacts with run

    docker compose up
To start the application the following

    sbt run

From this point the application is interactive, you can selectively choose what consumer/producer you'd like to run

It's recommended to start the consumers first, then produce events.

# Example Scenario

Demo of how normal producers work

Demo of how transactions work


## Exactly Once Semantics on Kafka Streams
Kafka Streams enables exactly once semantics when `processing.guarantee` is set to `exactly_once`

## Transaction Aware Kafka Producer
Exactly once semantics is enabled for Kafka Producers when `transactional.id` is set to a value. 

## Transaction Aware Kafka Consumer
To enable consumers to read  transactional topics (i.e. duplicate events aren't read). Set the `isolation.level` flag to `read_committed`

## Useful links
Akhq - http://localhost:8093/ui/docker-kafka-server/topic
