import Configuration.{nonTransactionalConsumerProperties, transactionConsumerProperties, transactionalStateConsumerProperties}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import kafka.coordinator.transaction.TransactionLog.{readTxnRecordKey, readTxnRecordValue}

import java.nio.ByteBuffer
import scala.jdk.CollectionConverters._

object Consumer extends App {

  val logger = Logger("Consumer")
  if(!isCustomTopics){
    readTransaction
  } else {
    if(isTransactional){
      Consumer.readCommitted(Configuration.conf.kafka.consumerTopics)
    } else {
      Consumer.readUncommitted(Configuration.conf.kafka.consumerTopics)
    }
  }

  def readCommitted(topics: List[String]) = {
    val consumer = new KafkaConsumer[String, String](transactionConsumerProperties)
    consumer.subscribe(topics.asJava)
    continuouslyPollConsumer(consumer, "TRANSACTIONAL CONSUMER")
  }
  def readUncommitted(topics: List[String]) = {
    val consumer = new KafkaConsumer[String, String](nonTransactionalConsumerProperties)
    consumer.subscribe(topics.asJava)
    continuouslyPollConsumer(consumer, "NON-TRANSACTIONAL CONSUMER")
  }
  /**
   * Similar to package kafka.coordinator.transaction.TransactionLog.TransactionLogMessageFormatter.writeTo
   */
  def readTransaction = {
    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](transactionalStateConsumerProperties)
    consumer.subscribe(List("__transaction_state").asJava)

    while(true){
      val records : ConsumerRecords[Array[Byte], Array[Byte]] = consumer.poll(100)
      records.forEach(record => {
        Option(record.key).map(key => readTxnRecordKey(ByteBuffer.wrap(key))).foreach { txnKey =>
          val transactionalId = txnKey.transactionalId
          val value = record.value
          val producerIdMetadata = if (value == null)
            None
          else
            readTxnRecordValue(transactionalId, ByteBuffer.wrap(value))
          producerIdMetadata match {
            case Some(tx) => {
              logger.info(
                s"""
                  |=====================
                  |TRANSACTION-STATE-MESSAGE
                  |=====================
                  |Transactional ID: ${tx.transactionalId}
                  |Producer ID: ${tx.producerId}
                  |ProducerEpoch: ${tx.producerEpoch}
                  |Txn Timeout Ms: ${tx.txnTimeoutMs}
                  |State: ${tx.state}
                  |Pending State: ${tx.pendingState}
                  |Topic Partitions: ${tx.topicPartitions}
                  |Txn Start Timestamp: ${tx.txnStartTimestamp}
                  |Txn Last Update Timestamp: ${tx.txnLastUpdateTimestamp}
                  |Complete Message: ${tx}
                  |=====================
                  |""".stripMargin)
            }
            case None =>
          }
        }
      })
    }
  }
  private def continuouslyPollConsumer (consumer: KafkaConsumer[String, String], logHeader: String)= {

    while(true){
      val records = consumer.poll(100)
      records.forEach(record => {
        logger.info(
          s"""
             |=====================
             | ${logHeader}
             | =====================
             |Reading from topic ${record.topic()}
             |Reading record with key: ${record.key()}
             |Reading record with value: ${record.value()}
             |Complete record: ${record}
             |=====================
             |""".stripMargin)
      })
    }
  }


  def isCustomTopics: Boolean = {
    def customTopics()  =  {
      println(
        """
          |What topics would you like to consume?
          |Enter number associated with the type:
          |0. Custom Topics
          |1. Transactional State
          |""".stripMargin)
      if(scala.io.StdIn.readInt() == 0) true else false
    }
    UserInputHandler.getUserInput(customTopics)
  }
  def isTransactional: (Boolean) = {
    def transactional() = {
      println(
        """
          |What type of messages would you like to Consume?
          |Enter number associated with the type:
          |0. Transactional
          |1. Non-Transactional
          |""".stripMargin)
      if(scala.io.StdIn.readInt() == 0) true else false
    }
    UserInputHandler.getUserInput(transactional)
  }

}
