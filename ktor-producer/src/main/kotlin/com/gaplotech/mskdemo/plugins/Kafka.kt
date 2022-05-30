package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.extensions.toProperties
import com.gaplotech.mskdemo.kafka.KafkaProtoSerializer
import com.gaplotech.mskdemo.pb.MSKDemo
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringSerializer

fun Application.buildKafkaProducer(): Producer<String, MSKDemo.OrderExecutionReport> {
    val config = ConfigFactory.load()

    return KafkaProducer(
        config.getConfig("kafka.producer").toProperties(),
        StringSerializer(),
        KafkaProtoSerializer(MSKDemo.OrderExecutionReport::class.java)
    )
}