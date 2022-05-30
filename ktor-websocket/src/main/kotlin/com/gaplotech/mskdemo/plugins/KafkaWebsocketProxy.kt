package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.actors.BroadcastProtobufMessageEnvelop
import com.gaplotech.mskdemo.actors.WebSocketActorEnvelop
import com.gaplotech.mskdemo.extensions.toProperties
import com.gaplotech.mskdemo.extensions.toWebSocketResponse
import com.gaplotech.mskdemo.kafka.KafkaProtoSerde
import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.pb.MSKDemo.SlidingAggregate
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.WindowedSerdes
import java.time.Duration
import kotlin.concurrent.thread

fun Application.bootstrapKafkaWebsocketProxy(
    webSocketActor: SendChannel<WebSocketActorEnvelop<*>>
) = thread {
    val logger = log
    val config = ConfigFactory.load()
    val kafkaTopicConfig = config.getConfig("kafka.topic")
    val kafkaStreamConfig = config.getConfig("kafka.stream")

    val candleStickPerMinuteTopic = kafkaTopicConfig.getString("public-v1-order-candlestick-minute")
    val slidingTwentyFourHourTopic = kafkaTopicConfig.getString("public-v1-order-sliding-aggregate-twentyfourhour")

    val topology = StreamsBuilder().apply {

        // ready only stream
        stream(
            candleStickPerMinuteTopic,
            Consumed.with(
                WindowedSerdes.timeWindowedSerdeFrom(String::class.java, Duration.ofMinutes(1).toMillis()),
                KafkaProtoSerde(MSKDemo.CandleStick::class.java)
            )
        ).foreach { key, value ->
            logger.info("received {}, key:{}, bytes:{}", candleStickPerMinuteTopic, key, value.serializedSize)
            runBlocking {
                webSocketActor.send(BroadcastProtobufMessageEnvelop(value.toWebSocketResponse()))
            }
        }

        stream(
            slidingTwentyFourHourTopic,
            Consumed.with(
                Serdes.String(),
                KafkaProtoSerde(SlidingAggregate::class.java)
            )
        ).foreach { key, value ->
            logger.info("received {}, key:{}, bytes:{}", slidingTwentyFourHourTopic, key, value.serializedSize)
            runBlocking {
                webSocketActor.send(BroadcastProtobufMessageEnvelop(value.toWebSocketResponse()))
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