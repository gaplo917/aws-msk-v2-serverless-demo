package com.gaplotech.mskdemo

import com.gaplotech.mskdemo.actors.websocketActor
import com.gaplotech.mskdemo.plugins.bootstrapKafkaWebsocketProxy
import com.gaplotech.mskdemo.plugins.configureCallLogging
import com.gaplotech.mskdemo.plugins.configureWebSocket
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val webSocketActor = websocketActor()
    configureCallLogging()
    configureWebSocket(webSocketActor)
    bootstrapKafkaWebsocketProxy(webSocketActor)
}
