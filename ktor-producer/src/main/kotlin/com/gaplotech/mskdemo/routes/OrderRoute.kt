package com.gaplotech.mskdemo.routes

import com.gaplotech.mskdemo.extensions.publishToKafkaTopic
import com.gaplotech.mskdemo.extensions.toProtoDecimal
import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.pb.orderExecutionReport
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Serializable
data class OrderRequest(
    val instrumentId: Int,
    val side: Int,
    val price: String,
    val quantity: String
) {
    val decimalPrice: MSKDemo.Decimal by lazy {
        BigDecimal(price, MathContext.DECIMAL64)
            .setScale(2, RoundingMode.UNNECESSARY)
            .toProtoDecimal()
    }
    val decimalQuantity: MSKDemo.Decimal by lazy {
        BigDecimal(quantity, MathContext.DECIMAL64)
            .setScale(0, RoundingMode.UNNECESSARY)
            .toProtoDecimal()
    }
}

fun Application.createOrder(
    producer: Producer<String, MSKDemo.OrderExecutionReport>,
    topic: String
): suspend PipelineContext<Unit, ApplicationCall>.(OrderRequest) -> Unit {
    val logger = log
    return { orderRequest ->
        logger.info("received order request")
        val orderTime = System.currentTimeMillis()
        val instrumentId = orderRequest.instrumentId

        // 3 partition, only
        if (instrumentId < 0 || instrumentId > 2) {
            call.respond(HttpStatusCode.BadRequest, "instrumentId not found")
        }
        val recordKey = "$instrumentId"
        val metadata = producer.publishToKafkaTopic(
            ProducerRecord(
                topic,
                instrumentId % 3,
                orderTime,
                recordKey,
                orderExecutionReport {
                    this.instrumentId = instrumentId
                    side = MSKDemo.Side.forNumber(orderRequest.side)
                    orderId = 0 // dummy for the demo
                    price = orderRequest.decimalPrice
                    quantity = orderRequest.decimalQuantity
                    timestamp = orderTime
                }
            )
        )

        call.respondText("published to topic=${metadata.topic()},partition=${instrumentId},offset=${metadata.offset()}")
    }
}