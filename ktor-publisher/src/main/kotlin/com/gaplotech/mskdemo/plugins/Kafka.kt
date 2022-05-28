package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.actors.ProtobufMessageWSAEnvelop
import com.gaplotech.mskdemo.actors.WebSocketActorEnvelop
import com.gaplotech.mskdemo.extensions.toProperties
import com.gaplotech.mskdemo.extensions.toWebSocketResponse
import com.gaplotech.mskdemo.kafka.KafkaProtoSerde
import com.gaplotech.mskdemo.kafka.KafkaProtoSerializer
import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.pb.MSKDemo.SlidingAggregate
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.WindowedSerdes
import java.time.Duration
import kotlin.concurrent.thread

fun Application.buildKafkaProducer(): KafkaProducer<String, MSKDemo.OrderExecutionReport> {
    val config = ConfigFactory.load()

    return KafkaProducer(
        config.getConfig("kafka.producer").toProperties(),
        StringSerializer(),
        KafkaProtoSerializer(MSKDemo.OrderExecutionReport::class.java)
    )
}

fun Application.bootstrapKafkaStream(
    webSocketActor: SendChannel<WebSocketActorEnvelop<*>>
) = thread {
    val logger = log
    val config = ConfigFactory.load()
    val kafkaTopicConfig = config.getConfig("kafka.topic")
    val kafkaStreamConfig = config.getConfig("kafka.stream")

    val candleStickPerMinuteTopic = kafkaTopicConfig.getString("order.candlestick.minute")
    val slidingTwentyFourHourTopic = kafkaTopicConfig.getString("order.sliding.aggregate.twentyfourhour")

    val topology = StreamsBuilder().apply {
        // ready only stream
        stream(
            candleStickPerMinuteTopic,
            Consumed.with(
                WindowedSerdes.timeWindowedSerdeFrom(String::class.java, Duration.ofSeconds(5).toMillis()),
                KafkaProtoSerde(MSKDemo.CandleStick::class.java)
            )
        ).foreach { key, value ->
            logger.debug("receive CandleStick msg: {}", key)
            runBlocking {
                webSocketActor.send(ProtobufMessageWSAEnvelop(value.toWebSocketResponse()))
            }
        }

        stream(
            slidingTwentyFourHourTopic,
            Consumed.with(
                Serdes.String(),
                KafkaProtoSerde(SlidingAggregate::class.java)
            )
        ).foreach { key, value ->
            logger.debug("receive SlidingAggregate msg: {}", key)
            runBlocking {
                webSocketActor.send(ProtobufMessageWSAEnvelop(value.toWebSocketResponse()))
            }
        }
    }.build()

    KafkaStreams(topology, kafkaStreamConfig.toProperties()).apply {
        logger.info("kafka stream start")
        // always restart the thread in case of error
        setUncaughtExceptionHandler { e ->
            logger.error("kafka stream has been crashed", e)
            StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
        }
        start()
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("shutting down kafka stream")
            close()
        })
    }

}