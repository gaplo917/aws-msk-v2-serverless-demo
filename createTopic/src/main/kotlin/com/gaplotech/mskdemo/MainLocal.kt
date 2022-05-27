import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.slf4j.LoggerFactory

/**
 * reset all kafka topic in local
 */
fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("main")
    val client = KafkaAdminClient.create(
        mapOf(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092")
    )

    val topics = listOf("order.execution.report", "order.candlestick.minute", "order.sliding.aggregate.twentyfourhour")

    try {

        // clear all topics for demo purpose
        client.deleteTopics(client.listTopics().names().get()).all().get()

        Thread.sleep(2000)

        client.createTopics(
            topics.map { NewTopic(it, 3, 1) }
        ).all().get()

    } catch (e: Throwable) {
        logger.info("unexpected error when creating topic")
        e.printStackTrace()
    }

    Thread.sleep(2000)

    logger.info("topics: {}", client.listTopics().names().get())
}