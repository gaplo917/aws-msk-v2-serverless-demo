package com.gaplotech.mskdemo

import com.gaplotech.mskdemo.actors.websocketActor
import com.gaplotech.mskdemo.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val webSocketActor = websocketActor()
    configureWebSocket(webSocketActor)
    configureContentNegotiation()
    configureHTTP()
    configureRouting(buildKafkaProducer())
    bootstrapKafkaStream(webSocketActor)
}
