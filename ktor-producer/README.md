# AWS MSK Serverless Live Demo - Data Producer
This is a simple Ktor Kolint/JVM sample project to act a **Kafka Data Producer** in the 
[demo](https://github.com/gaplo917/aws-msk-v2-serverless-demo) 

## Highlights
- use API to publish a kafka records (easier to generate loading during demo)

## Exposed API

GET http://localhost:8080/ping

POST http://localhost:8080/createOrder

## Local Development

1. start the local kafka in docker, See 
2. Just start the arrow in `ProducerApplication.kt`
![](./intelij-ktor-play-button.png)
3. Add `-Dconfig.file=src/main/resources/application.local.conf` in the "VM options"
![](./intelij-ktor-vm-options.png)

## Local Build Docker Image
```bash
# build a image call ktor-producer:latest
./gradlew jibDockerBuild
```

## Local Deploy to AWS Elastic Container Registry

1. rename `.env.example` to `.env`
2. change the variables to your own

```bash
# export environment variable to shell
set -o allexport
source .env
set +o allexport

./gradlew jib \
-Djib.from.image="amazoncorretto:11" \
-Djib.to.image="$IMAGE_REGISTRY/$IMAGE_NAME" \
-Djib.to.credHelper="ecr-login" \
-Djib.to.tags="latest,$IMAGE_TAGS" \
-Djib.container.creationTime=USE_CURRENT_TIMESTAMP
```

# Resources for learning

See [here](https://github.com/gaplo917/aws-msk-v2-serverless-demo)
