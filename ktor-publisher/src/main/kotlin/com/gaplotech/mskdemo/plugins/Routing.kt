package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.actors.AddSessionWSAEnvelop
import com.gaplotech.mskdemo.actors.RemoveSessionWSAEnvelop
import com.gaplotech.mskdemo.actors.WebSocketActorEnvelop
import com.gaplotech.mskdemo.extensions.publishToKafkaTopic
import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.pb.decimal
import com.gaplotech.mskdemo.pb.orderExecutionReport
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.SendChannel
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong


fun Application.configureRouting(
    producer: KafkaProducer<String, MSKDemo.OrderExecutionReport>?,
    webSocketActor: SendChannel<WebSocketActorEnvelop<*>>
) {
    val logger = log
    val config = ConfigFactory.load()
    val kafkaTopicConfig = config.getConfig("kafka.topic")
    val orderExecutionReportsTopic = kafkaTopicConfig.getString("order.execution.report")
    // Starting point for a Ktor app:
    routing {
        get("/emitOrderExecutionReport/{instrumentId}") {
            logger.info("accepting publish kafka topic")
            val orderTime = System.currentTimeMillis()
            val instrumentId = call.parameters["instrumentId"]?.toIntOrNull() ?: 0
            // 3 partition, only
            if(instrumentId < 0 || instrumentId > 3) {
                call.respond(HttpStatusCode.BadRequest, "instrumentId not found")
                return@get
            }
            val recordKey = "1"
            val metadata = producer?.publishToKafkaTopic(
                ProducerRecord(
                    orderExecutionReportsTopic,
                    instrumentId,
                    orderTime,
                    recordKey,
                    orderExecutionReport {
                        this.instrumentId = instrumentId
                        side = MSKDemo.Side.BUY // dummy for the demo
                        orderId = 0 // dummy for the demo
                        price = decimal { scale = 2; unscaledVal = nextLong(100,200L) }
                        quantity = decimal { scale = 0; unscaledVal = nextLong(1,10) * 10 }
                        timestamp = orderTime
                    }
                )
            )

            call.respondText("published to topic=${metadata?.topic()},partition=${instrumentId},offset=${metadata?.offset()}")
        }

        get("/ping") {
            logger.info("accepting health check")
            call.respondText("OK")
        }

        webSocket("/subscription") {
            // Handle a WebSocket session
            webSocketActor.send(AddSessionWSAEnvelop(this))
            for (frame in incoming) {
                // do nothing
            }
            webSocketActor.send(RemoveSessionWSAEnvelop(this))
        }
    }
}

