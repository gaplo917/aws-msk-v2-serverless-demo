# AWS MSK Serverless Live Demo

This project consist of four projects. Each project has its own documentation in `README.md` 

[Getting started AWS MSK Serverless](https://docs.aws.amazon.com/msk/latest/developerguide/serverless-getting-started.html)

- [Kafka Publisher and WebSocket](ktor-publisher)
- [Kafka Steam Aggregator](ktor-data-aggregator)
- [CDK for deployment](cdk)
- [Create Kafka topic (demo purpose)](kafka-topic-scripts)

## Getting Started (Local Development)

```bash
# init local kafka
docker-compose up -d
```
Then start the application in the following sequence:
- [createTopic](kafka-topic-scripts)
- [ktor-consumer](ktor-data-aggregator)
- [ktor-publisher](ktor-publisher)


### Deploy
```
# install AWS Cloud Development Kit
nvm install 16
nvm use 16
npm install -g aws-cdk

# deploy
cd cdk
cdk deploy EcrStack
cdk deploy VpcStack
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
