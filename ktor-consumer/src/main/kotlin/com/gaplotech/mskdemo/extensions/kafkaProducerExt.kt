package com.gaplotech.mskdemo.extensions

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <K,V> KafkaProducer<K, V>.publishToKafkaTopic(record: ProducerRecord<K, V>) = suspendCoroutine<RecordMetadata> { co ->
    this.send(record) { metadata, exception ->
        if (exception != null) {
            co.resumeWithException(exception)
        } else {
            co.resume(metadata)
        }
    }
}
