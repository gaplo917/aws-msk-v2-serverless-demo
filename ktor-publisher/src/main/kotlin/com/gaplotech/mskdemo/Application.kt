package com.gaplotech.mskdemo

import com.gaplotech.mskdemo.actors.websocketActor
import com.gaplotech.mskdemo.plugins.bootstrapKafkaStream
import com.gaplotech.mskdemo.plugins.buildKafkaProducer
import com.gaplotech.mskdemo.plugins.configureHTTP
import com.gaplotech.mskdemo.plugins.configureRouting
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.websocket.*
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val webSocketActor = websocketActor()
    install(WebSockets)
    install(CallLogging) {
        val config = ConfigFactory.load()
        level = Level.valueOf(config.getString("ktor.logging.level"))
    }
    configureHTTP()
    configureRouting(buildKafkaProducer(), webSocketActor)
    bootstrapKafkaStream(webSocketActor)
}
