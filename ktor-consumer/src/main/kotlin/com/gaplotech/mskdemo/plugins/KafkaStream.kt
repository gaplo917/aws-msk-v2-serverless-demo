package com.gaplotech.mskdemo.plugins

import com.gaplotech.mskdemo.extensions.*
import com.gaplotech.mskdemo.kafka.KafkaProtoSerde
import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.pb.copy
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.*
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

@Suppress("RedundantSamConstructor")
fun Application.bootstrapKafkaStream() = thread {
    val logger = log
    val config = ConfigFactory.load()
    val kafkaTopicConfig = config.getConfig("kafka.topic")
    val kafkaStreamConfig = config.getConfig("kafka.stream")

    val orderExecutionReportsTopic = kafkaTopicConfig.getString("order.execution.report")
    val candleStickPerMinuteTopic = kafkaTopicConfig.getString("order.candlestick.minute")
    val slidingTwentyFourHourTopic = kafkaTopicConfig.getString("order.sliding.aggregate.twentyfourhour")

    val windowMinuteSize = TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(60))

    logger.info("bootstrap kafka stream for {}, {}, {}", orderExecutionReportsTopic, candleStickPerMinuteTopic, slidingTwentyFourHourTopic)

    val topology = StreamsBuilder().apply {
        val reports = stream(
            orderExecutionReportsTopic,
            Consumed.with(
                Serdes.String(),
                KafkaProtoSerde(MSKDemo.OrderExecutionReport::class.java)
            )
        ).peek { key, value ->
            logger.info("received {}, key:{}, bytes:{}", orderExecutionReportsTopic, key, value.serializedSize)
        }

        // Grouping Ratings
        val candleSticks = reports
            .map { _, value ->
                KeyValue("${value.instrumentId}", value)
            }
            .groupByKey(Grouped.with(Serdes.String(), KafkaProtoSerde(MSKDemo.OrderExecutionReport::class.java)))
            .windowedBy(windowMinuteSize)
            .aggregate(
                Initializer { MSKDemo.CandleStick.newBuilder().build() },
                Aggregator { _, value, aggregate -> aggregate + value },
                Materialized.with(Serdes.String(), KafkaProtoSerde(MSKDemo.CandleStick::class.java))
            )
        //.suppress(Suppressed.untilWindowCloses(unbounded()))

        val slidingTwentyFourHour = reports
            .map { _, value ->
                KeyValue("${value.instrumentId}", value)
            }
            .groupByKey(Grouped.with(Serdes.String(), KafkaProtoSerde(MSKDemo.OrderExecutionReport::class.java)))
            .aggregate(
                Initializer { MSKDemo.SlidingAggregate.newBuilder().build() },
                Aggregator { _, value, aggregate -> aggregate + value },
                Materialized.with(Serdes.String(), KafkaProtoSerde(MSKDemo.SlidingAggregate::class.java))
            )
        //.suppress(Suppressed.untilTimeLimit(Duration.ofHours(1), maxRecords(1000)))

        // persist to topic
        candleSticks
            .toStream()
            .peek { key, value ->
                logger.info("received {}, key:{}, bytes:{}", candleStickPerMinuteTopic, key, value.serializedSize)
            }
            .to(
                candleStickPerMinuteTopic,
                Produced.with(
                    WindowedSerdes.timeWindowedSerdeFrom(String::class.java, windowMinuteSize.sizeMs),
                    KafkaProtoSerde(MSKDemo.CandleStick::class.java)
                )
            )

        slidingTwentyFourHour
            .toStream()
            .peek { key, value ->
                logger.info("received {}, key:{}, bytes:{}", slidingTwentyFourHourTopic, key, value.serializedSize)
            }
            .to(
                slidingTwentyFourHourTopic,
                Produced.with(
                    Serdes.String(),
                    KafkaProtoSerde(MSKDemo.SlidingAggregate::class.java)
                )
            )

    }.build()

    val streams = KafkaStreams(topology,
        Properties().apply {
            putAll(kafkaStreamConfig.toProperties())
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String()::class.java)
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String()::class.java)
        })

    val latch = CountDownLatch(1)

    logger.info("kafka stream start")

    streams.start()
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("shutting down kafka stream")
        streams.close()
    })
    latch.await()
}



private operator fun MSKDemo.CandleStick.plus(value: MSKDemo.OrderExecutionReport): MSKDemo.CandleStick {
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

private operator fun MSKDemo.SlidingAggregate.plus(value: MSKDemo.OrderExecutionReport): MSKDemo.SlidingAggregate {
    return this.toBuilder().apply {
        val mutableTickersList = tickersList.toMutableList()
        mutableTickersList += value
        volume += value.quantity

        val latest = mutableTickersList.last()
        val windowEnd = latest.timestamp - Duration.ofHours(24).toMillis()
        val iterator = mutableTickersList.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.timestamp < windowEnd) {
                volume -= next.quantity
                iterator.remove()
            }
            // assume the kafka messages are sorted, we can break earlier
            // P.S. binary search find the index and remove the whole subArray maybe faster
            break
        }
        val earliest = mutableTickersList.first()
        priceChange = latest.price - earliest.price
        open = earliest.price.copy {}
        close = value.price.copy {}
        high = high.max(value.price)
        low = low.min(value.price)
        count = mutableTickersList.size
        startTime = earliest.timestamp
        endTime = latest.timestamp
        clearTickers()
        addAllTickers(mutableTickersList)
    }.build()
}
