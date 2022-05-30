package com.gaplotech.mskdemo.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <K,V> Producer<K, V>.publishToKafkaTopic(record: ProducerRecord<K, V>): RecordMetadata {
    val self = this

    return withContext(Dispatchers.IO) {
        suspendCoroutine { co ->
            self.send(record) { metadata, exception ->
                if (exception != null) {
                    co.resumeWithException(exception)
                } else {
                    co.resume(metadata)
                }
            }

        }
    }
}