package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.extensions.publishToKafkaTopic
import com.gaplotech.mskdemo.extensions.toProtoDecimal
import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.pb.orderExecutionReport
import com.gaplotech.mskdemo.routes.createOrder
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.apache.kafka.clients.producer.KafkaProducer
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
fun Application.configureRouting(
    producer: KafkaProducer<String, MSKDemo.OrderExecutionReport>
) {
    val app = this
    val config = ConfigFactory.load()
    val kafkaTopicConfig = config.getConfig("kafka.topic")
    val orderExecutionReportsTopic = kafkaTopicConfig.getString("order.execution.report")
    // Starting point for a Ktor app:
    routing {
        post("/createOrder", app.createOrder(producer, orderExecutionReportsTopic))

        get("/ping") {
            call.respondText("OK")
        }

    }
}

