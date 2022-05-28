# Create Topic (Mac Local)
```bash
curl https://archive.apache.org/dist/kafka/3.1.1/kafka_2.13-3.1.1.tgz > kafka_2.13-3.1.1.tgz
cd kafka_2.13-3.1.1/bin

BS="localhost:9092"
TOPICS=("order.execution.report" "order.candlestick.minute" "order.sliding.aggregate.twentyfourhour" "dev.com.gaplotech.mskdemo.kafka.stream.writer-store.order.candlestick.minute.aggregated-repartition" "dev.com.gaplotech.mskdemo.kafka.stream.writer-store.order.candlestick.minute.aggregated-changelog" "dev.com.gaplotech.mskdemo.kafka.stream.writer-store.order.sliding.aggregate.twentyfourhour-repartition" "dev.com.gaplotech.mskdemo.kafka.stream.writer-store.order.sliding.aggregate.twentyfourhour-changelog")

# create topics
for t in ${TOPICS[@]}; do
  ./kafka-topics.sh --bootstrap-server $BS --create --topic $t --partitions 3
done

# delete topics
for t in ${TOPICS[@]}; do
  ./kafka-topics.sh --bootstrap-server $BS --delete --topic $t
done

# List all topics with config
./kafka-configs.sh --bootstrap-server $BS --entity-type topics --describe --all

# List all topics
./kafka-topics.sh --bootstrap-server $BS --list

# consume order.execution.report
./kafka-console-consumer.sh --bootstrap-server $BS --topic order.execution.report --from-beginning

```

# Create Topic (Remote)
SSH to a remote AWS EC2 instance
```bash
sudo yum -y install java-11
wget https://archive.apache.org/dist/kafka/3.1.1/kafka_2.13-3.1.1.tgz
wget https://github.com/aws/aws-msk-iam-auth/releases/download/v1.1.1/aws-msk-iam-auth-1.1.1-all.jar
tar -xzf kafka_2.13-3.1.1.tgz

mv aws-msk-iam-auth-1.1.1-all.jar ./kafka_2.13-3.1.1/libs/
echo -e "security.protocol=SASL_SSL\nsasl.mechanism=AWS_MSK_IAM\nsasl.jaas.config=software.amazon.msk.auth.iam.IAMLoginModule required;\nsasl.client.callback.handler.class=software.amazon.msk.auth.iam.IAMClientCallbackHandler" > kafka_2.13-3.1.1/bin/client.preperties
cd kafka_2.13-3.1.1/bin

# export bootstrapserver
BS="boot-xcdtzila.c1.kafka-serverless.ap-southeast-1.amazonaws.com:9098"
TOPICS=("order.execution.report" "order.candlestick.minute" "order.sliding.aggregate.twentyfourhour" "com.gaplotech.mskdemo.kafka.stream.writer-store.order.candlestick.minute.aggregated-repartition" "com.gaplotech.mskdemo.kafka.stream.writer-store.order.candlestick.minute.aggregated-changelog" "com.gaplotech.mskdemo.kafka.stream.writer-store.order.sliding.aggregate.twentyfourhour-repartition" "com.gaplotech.mskdemo.kafka.stream.writer-store.order.sliding.aggregate.twentyfourhour-changelog")


# create topics
for t in ${TOPICS[@]}; do
  ./kafka-topics.sh --bootstrap-server $BS --command-config client.properties --create --topic $t --partitions 3
done

# delete topics
for t in ${TOPICS[@]}; do 
  ./kafka-topics.sh --bootstrap-server $BS --command-config client.properties --delete --topic $t
done;

# List all topics with config
./kafka-configs.sh --bootstrap-server $BS --command-config client.properties --entity-type topics --describe --all

# List all topics
./kafka-topics.sh --bootstrap-server $BS --command-config client.properties --list

# consume order.execution.report
./kafka-console-consumer.sh --bootstrap-server $BS --command-config client.propertie --topic order.execution.report --from-beginning

```

# Resources for learning
See [here](https://github.com/gaplo917/aws-msk-v2-serverless-demo)