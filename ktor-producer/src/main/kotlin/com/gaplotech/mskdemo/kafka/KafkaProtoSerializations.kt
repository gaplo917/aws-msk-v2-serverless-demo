package com.gaplotech.mskdemo.kafka

import com.google.protobuf.Message
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

// Kafka originally support protobuf serialization, but it has to be used with schema registry.
// For keep this demo simple, just cut the schema registry part and use a custom serialization.

/**
 * Bytes to protobuf java class.
 */
class KafkaProtoDeserializer<T : Message>(private val clazz: Class<T>) : Deserializer<T> {
    override fun deserialize(topic: String, data: ByteArray): T {
        @Suppress("unchecked_cast")
        return clazz.getMethod("parseFrom", ByteArray::class.java).invoke(null, data) as T
    }
}

/**
 * Protobuf java class to bytes
 */
class KafkaProtoSerializer<T : Message>(private val clazz: Class<T>) : Serializer<T> {
    override fun serialize(topic: String, data: T): ByteArray {
        return data.toByteArray()
    }
}

/**
 * Kafka Serde for protobuf
 */
class KafkaProtoSerde<T : Message>(private val clazz: Class<T>) : Serde<T> {
    override fun deserializer(): Deserializer<T> = KafkaProtoDeserializer(clazz)
    override fun serializer(): Serializer<T> = KafkaProtoSerializer(clazz)
}