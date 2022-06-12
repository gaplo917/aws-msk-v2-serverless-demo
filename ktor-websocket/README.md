# AWS MSK Serverless Live Demo - Websocket
This is a simple Ktor Kolint/JVM sample project to act a **Websocket to stream kafka data** in the 
[demo](https://github.com/gaplo917/aws-msk-v2-serverless-demo) 

## Highlights
- use websocket to push kafka stream aggregated data to client (real-time data)
- use Kotlin coroutine feature to handle shared mutable state without context switch in thread, 
See [the article](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html#actors).


## Exposed API

Websocket ws://localhost:9000/subscription
dd
## Local Development

1. start the local kafka in docker, See 
2. Just start the arrow in `WebSocketApplication.kt`
![](./intelij-ktor-play-button.png)
3. Add `-Dconfig.file=src/main/resources/application.local.conf` in the "VM options"
![](./intelij-ktor-vm-options.png)

## Local Build Docker Image
```bash
# build a image call ktor-websocket:latest
./gradlew jibDockerBuild 
```

## Build Docker Image to AWS Elastic Container Registry

1. rename `.env.example` to `.env`
2. change the variables to your own

```bash
# export environment variable to shell
set -o allexport
source .env
set +o allexportc

./gradlew jib \
-Djib.from.image="amazoncorretto:11" \
-Djib.to.image="$IMAGE_REGISTRY/$IMAGE_NAME" \
-Djib.to.credHelper="ecr-login" \
-Djib.to.tags="latest,$IMAGE_TAGS" \
-Djib.container.creationTime=USE_CURRENT_TIMESTAMP
```

# Resources for learning

See [here](https://github.com/gaplo917/aws-msk-v2-serverless-demo)
