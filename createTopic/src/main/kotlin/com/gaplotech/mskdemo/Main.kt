import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.SaslConfigs
import org.slf4j.LoggerFactory

/**
 * reset all kafka topic on AWS
 *
 * Connect to an ssh tunnel and execute remotely by using intellij Ultimate
 */
fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("main")
    val client = KafkaAdminClient.create(mapOf(
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to "boot-xcdtzila.c1.kafka-serverless.ap-southeast-1.amazonaws.com:9098",
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
        SaslConfigs.SASL_MECHANISM to "AWS_MSK_IAM",
        SaslConfigs.SASL_JAAS_CONFIG to "software.amazon.msk.auth.iam.IAMLoginModule required;",
        SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS to "software.amazon.msk.auth.iam.IAMClientCallbackHandler",
    ))
    val topics = listOf(
        "order.execution.report",
        "order.candlestick.minute",
        "order.sliding.aggregate.twentyfourhour",
        "com.gaplotech.mskdemo.kafka.stream.writer-KSTREAM-AGGREGATE-STATE-STORE-0000000003-repartition",
        "com.gaplotech.mskdemo.kafka.stream.writer-KSTREAM-AGGREGATE-STATE-STORE-0000000009-repartition",
        "com.gaplotech.mskdemo.kafka.stream.writer-KSTREAM-AGGREGATE-STATE-STORE-0000000009-changelog",
        "com.gaplotech.mskdemo.kafka.stream.writer-KSTREAM-AGGREGATE-STATE-STORE-0000000003-changelog",
    )

    try {
        // clear all topics
        client.deleteTopics(client.listTopics().names().get()).all().get()

        Thread.sleep(2000)

        client.createTopics(
            topics.map { NewTopic(it, 3, 3) }
        ).all().get()

    } catch (e: Throwable) {
        logger.info("unexpected error when creating topic")
        e.printStackTrace()
    }

    Thread.sleep(2000)

    logger.info("topics: {}", client.listTopics().names().get())
}


