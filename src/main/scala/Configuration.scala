import akka.actor.ActorSystem
import akka.kafka.ConsumerSettings
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import pureconfig._
import pureconfig.generic.auto._

import java.util.Properties
import java.util.UUID.randomUUID

object Configuration {
  val conf: Conf = ConfigSource.default.loadOrThrow[Conf]
  val nonTransactionProducerProperties: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", Configuration.conf.kafka.bootstrapServers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props
  }
  val transactionProducerProperties: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", Configuration.conf.kafka.bootstrapServers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("transactional.id", "transaction-demo")
    props.put("enable.idempotence", "true")
    props
  }
  val nonTransactionalConsumerProperties: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", Configuration.conf.kafka.bootstrapServers)
    props.put("group.id", s"normal-consumer-${randomUUID.toString}")
    props.put("enable.auto.commit", "true")
    props.put("isolation.level", "read_uncommitted") // Default setting - consume both committed and uncommitted messages in offset ordering.
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props
  }
  val transactionConsumerProperties: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", Configuration.conf.kafka.bootstrapServers)
    props.put("group.id", s"transactional-consumer-${randomUUID.toString}")
    props.put("enable.auto.commit", "true")
    props.put("isolation.level", "read_committed") //only consume non-transactional messages or committed transactional messages in offset order
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    props
  }
  val transactionalStateConsumerProperties: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", Configuration.conf.kafka.bootstrapServers)
    props.put("group.id", s"transactional-state-consumer-${randomUUID.toString}")
    props.put("enable.auto.commit", "true")
    props.put("isolation.level", "read_committed") //only consume non-transactional messages or committed transactional messages in offset order
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    props
  }
  def transactionUnawareConsumerStreamingConfig(implicit system: ActorSystem): ConsumerSettings[String, String] = {
    val config = system.settings.config.getConfig("transaction-unaware-consumer")
    ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
      .withGroupId(s"transaction-unaware-stream-${randomUUID.toString}")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  }
  def transactionAwareConsumerStreamingConfig(implicit system: ActorSystem): ConsumerSettings[String, String] = {
    val config = system.settings.config.getConfig("transaction-aware-consumer")
    ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
      .withGroupId(s"transaction-aware-stream-${randomUUID.toString}")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  }
  case class Conf(kafka: KafkaConf)
  case class KafkaConf(bootstrapServers: String, consumerTopics: List[String], producerTopics: List[String])
}
