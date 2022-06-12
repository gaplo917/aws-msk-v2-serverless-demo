package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.extensions.*
import com.gaplotech.mskdemo.kafka.KafkaProtoSerde
import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.pb.MSKDemo.CandleStick
import com.gaplotech.mskdemo.pb.copy
import com.google.protobuf.Message
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.*
import java.time.Duration
import kotlin.concurrent.thread

fun Application.bootstrapKafkaStream() = thread {
    val logger = log
    val config = ConfigFactory.load()
    val kafkaTopicConfig = config.getConfig("kafka.topic")
    val kafkaStreamConfig = config.getConfig("kafka.stream")

    val orderExecutionReportsTopic = kafkaTopicConfig.getString("private-v1-order-execution-report")
    val candleStickPerMinuteTopic = kafkaTopicConfig.getString("public-v1-order-candlestick-minute")
    val slidingTwentyFourHourTopic = kafkaTopicConfig.getString("public-v1-order-sliding-aggregate-twentyfourhour")

    val candleStickStoreTopic = kafkaTopicConfig.getString("private-v1-store-order-candlestick-minute")
    val slidingStoreTopic = kafkaTopicConfig.getString("private-v1-store-order-sliding-aggregate-twentyfourhour")

    logger.info(
        "bootstrap kafka stream for {}, {}, {}",
        orderExecutionReportsTopic,
        candleStickPerMinuteTopic,
        slidingTwentyFourHourTopic
    )

    fun logKeyAndProtoSize(topic: String): ForeachAction<Any, Message> {
        return ForeachAction { key, value ->
            logger.info("received {}, key:{}, bytes:{}", topic, key, value.serializedSize)
        }
    }

    val topology = StreamsBuilder().apply {
        val reports = buildReportStream(orderExecutionReportsTopic)
            .peek(logKeyAndProtoSize(orderExecutionReportsTopic))

        // Grouping Ratings
        val candleSticks = reports.aggregateToCandleStick(candleStickStoreTopic)

        val slidingTwentyFourHour =
            reports.aggregateSlidingAggregated(slidingStoreTopic)

        // persist to topic
        candleSticks
            .toStream()
            .peek(logKeyAndProtoSize(candleStickPerMinuteTopic))
            .to(
                candleStickPerMinuteTopic,
                Produced.with(
                    WindowedSerdes.timeWindowedSerdeFrom(String::class.java, Duration.ofMinutes(1).toMillis()),
                    KafkaProtoSerde(MSKDemo.CandleStick::class.java)
                )
            )

        slidingTwentyFourHour
            .toStream()
            .peek(logKeyAndProtoSize(slidingTwentyFourHourTopic))
            .to(
                slidingTwentyFourHourTopic,
                Produced.with(
                    Serdes.String(),
                    KafkaProtoSerde(MSKDemo.SlidingAggregate::class.java)
                )
            )
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

fun StreamsBuilder.buildReportStream(
    topic: String
): KStream<String, MSKDemo.OrderExecutionReport> {
    return this.stream(
        topic,
        Consumed.with(
            Serdes.String(),
            KafkaProtoSerde(MSKDemo.OrderExecutionReport::class.java)
        )
    )
}

fun KStream<String, MSKDemo.OrderExecutionReport>.aggregateToCandleStick(
    aggregatedTopic: String,
): KTable<Windowed<String>, MSKDemo.CandleStick> {

    return this
        .map { _, value ->
            KeyValue("${value.instrumentId}", value)
        }
        .groupByKey(Grouped.with(Serdes.String(), KafkaProtoSerde(MSKDemo.OrderExecutionReport::class.java)))
        .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(60)))
        .aggregate(
            { MSKDemo.CandleStick.newBuilder().build() },
            { _, value, aggregate -> aggregate + value },
            Materialized.`as`<String, MSKDemo.CandleStick, WindowStore<Bytes, ByteArray>>(aggregatedTopic)
                .withKeySerde(Serdes.String())
                .withValueSerde(KafkaProtoSerde(MSKDemo.CandleStick::class.java))
        )
}


fun KStream<String, MSKDemo.OrderExecutionReport>.aggregateSlidingAggregated(
    aggregatedTopic: String
): KTable<String, MSKDemo.SlidingAggregate> {
    return this
        .map { _, value ->
            KeyValue("${value.instrumentId}", value)
        }
        .groupByKey(Grouped.with(Serdes.String(), KafkaProtoSerde(MSKDemo.OrderExecutionReport::class.java)))
        .aggregate(
            { MSKDemo.SlidingAggregate.newBuilder().build() },
            { _, value, aggregate -> aggregate + value },
            Materialized.`as`<String, MSKDemo.SlidingAggregate, KeyValueStore<Bytes, ByteArray>>(aggregatedTopic)
                .withKeySerde(Serdes.String())
                .withValueSerde(KafkaProtoSerde(MSKDemo.SlidingAggregate::class.java))
        )
}


/**
 * Merge OrderExecutionReport into the running CandleStick
 */
operator fun MSKDemo.CandleStick.plus(value: MSKDemo.OrderExecutionReport): MSKDemo.CandleStick {
    return this.toBuilder().apply {
        if (instrumentId == 0) {
            instrumentId = value.instrumentId
        }
        if (open.unscaledVal == 0L) {
            open = value.price.copy {}
        }
        high = high.max(value.price)
        low = low.min(value.price)
        close = value.price.copy {}
        volume = value.quantity + volume
        count += 1
        startTime = if (startTime == 0L) value.timestamp else startTime
        endTime = value.timestamp
    }.build()
}

/**
 * Merge OrderExecutionReport into the running SlidingAggregate
 */
operator fun MSKDemo.SlidingAggregate.plus(value: MSKDemo.OrderExecutionReport): MSKDemo.SlidingAggregate {
    return this.toBuilder().apply {
        val mutableTickersList = tickersList.toMutableList()
        mutableTickersList += value
        volume += value.quantity
        count += 1

        val latest = mutableTickersList.last()
        val earliestWindow = latest.timestamp - Duration.ofHours(24).toMillis()

        val iterator = mutableTickersList.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.timestamp < earliestWindow) {
                volume -= next.quantity
                count -= 1
                iterator.remove()
            }
            // assume the kafka messages are sorted, we can break earlier
            break
        }
        val earliest = mutableTickersList.first()
        priceChange = latest.price - earliest.price
        open = earliest.price.copy {}
        close = value.price.copy {}
        high = high.max(value.price)
        low = low.min(value.price)
        startTime = earliest.timestamp
        endTime = latest.timestamp
        clearTickers()
        addAllTickers(mutableTickersList)
    }.build()
}
