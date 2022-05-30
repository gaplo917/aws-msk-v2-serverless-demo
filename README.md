# AWS MSK Serverless Live Demo

This project consist of four projects. Each project has its own documentation in `README.md` 

[Getting started AWS MSK Serverless](https://docs.aws.amazon.com/msk/latest/developerguide/serverless-getting-started.html)

- [Producer](ktor-producer)
- [Data Aggregator](ktor-data-aggregator)
- [WebSocket](ktor-producer)
- [CDK for deployment](cdk)
- [Create Kafka topic and useful debug command](kafka-topic-scripts)

## Getting Started and Running (Local)

```bash
CONTAINER_SERVICES=('ktor-data-aggregator' 'ktor-producer' 'ktor-websocket')
for service in ${CONTAINER_SERVICES[@]}; do
    ./$service/gradlew jibDockerBuild \
    -b ./$service/build.gradle.kts
    -Djib.from.image="amazoncorretto:11" \
    -Djib.to.image="local-$service" \
    -Djib.to.tags="latest" \
    -Djib.container.creationTime=USE_CURRENT_TIMESTAMP 
done

# init the zookeeper, kafka in background
docker-compose up -d zookeeper broker

# init the zookeeper, kafka, kafka topics
docker-compose up init-kafka-topic-client

# after all topics created,
# boot up the rest ktor-data-aggregator, ktor-producer, ktor-websocket
docker-compose up local-ktor-data-aggregator local-ktor-producer local-ktor-websocket

``` 

Test it locally
```bash
# then open the other terminal 
# install wscat (WebSocket CLI client)
npm install -g wscat

# connect to websocket
wscat --connect ws://localhost:9000/subscription

# then open the other terminal
# produce one data record (create order)
curl --location --request POST 'http://localhost:8080/createOrder' \
--header 'Content-Type: application/json' \
--data-raw '{
    "instrumentId": 0,
    "side": 0,
    "price": "5.00",
    "quantity": "10"
}'
```

### Deploy to AWS
```
# install AWS Cloud Development Kit
nvm install 16
nvm use 16
npm install -g aws-cdk

# deploy
cd cdk
cdk deploy EcrStack
cdk deploy VpcStack

# mannually create AWS MSK Serverless, because cdk don't support it yet. https://github.com/aws/aws-cdk/issues/20362

# build application docker image, follows the documentation of each repo

cdk deploy FargateStack
```

## Resources for learning

#### Always use jib to build java/kotlin image
https://cloud.google.com/java/getting-started/jib

#### Kotlin Coroutine (Actor model)
- https://kt.academy/article/cc-channel
- https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html#actors

#### Ktor websocket
- https://ktor.io/docs/websocket.html

#### Kafka Stream
- https://docs.confluent.io/platform/current/streams/developer-guide/dsl-api.html

#### AWS Glue (Kafka Schema Registry)
- https://aws.amazon.com/blogs/big-data/introducing-protocol-buffers-protobuf-schema-support-in-amazon-glue-schema-registry/
