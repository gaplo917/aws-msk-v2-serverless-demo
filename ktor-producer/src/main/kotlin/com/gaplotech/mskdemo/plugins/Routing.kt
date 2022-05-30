package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.routes.createOrder
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.kafka.clients.producer.Producer


fun Application.configureRouting(
    producer: Producer<String, MSKDemo.OrderExecutionReport>
) {
    val app = this
    val config = ConfigFactory.load()
    val kafkaTopicConfig = config.getConfig("kafka.topic")
    val orderExecutionReportsTopic = kafkaTopicConfig.getString("private-v1-order-execution-report")
    // Starting point for a Ktor app:
    routing {
        post("/createOrder", app.createOrder(producer, orderExecutionReportsTopic))

        get("/ping") {
            call.respondText("OK")
        }

    }
}

