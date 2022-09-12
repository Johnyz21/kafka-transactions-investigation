import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.UUID.randomUUID
import scala.util.{Failure, Success, Try}

object Producer extends App {

  val logger: Logger = Logger("Producer")
  val events = getEvents

  getUserInput match {
    case (true, errorAfterIndex) => Producer.writeWithTransactions(events, errorAfterIndex)
    case (false, errorAfterIndex) => Producer.writeWithoutTransactions(events, errorAfterIndex)
  }
  def writeWithoutTransactions(records: List[ProducerRecord[String, String]], errorOnIndex: Int = -1): Unit = {
    val producer = new KafkaProducer[String, String](Configuration.nonTransactionProducerProperties)
    sendRecords(producer, records, errorOnIndex)
    producer.close()
  }
  def writeWithTransactions(records: List[ProducerRecord[String, String]], errorOnIndex: Int = -1): Unit = {
    val producer = new KafkaProducer[String, String](Configuration.transactionProducerProperties)
    logger.info(
      """
        |Initialising transaction...""".stripMargin)
    producer.initTransactions()
    logger.info(
      """
        |Beginning transaction...""".stripMargin)
    producer.beginTransaction()

    sendRecords(producer, records, errorOnIndex) match {
      case Success(_) => {
        logger.info(
        """
            |Committing transaction...""".stripMargin)
        producer.commitTransaction()
        logger.info(
          """
            |Closing Producer...""")
        producer.close()
      }
      case Failure(e) => {
        producer.abortTransaction()
        logger.error(
          """
            |Error occurred producing record""".stripMargin)
        logger.error(s"""
                 |${e.toString}""")
        logger.info(
          """
            |Closing Producer...""".stripMargin)
        producer.close()
      }
    }
  }
  private def sendRecords(producer: KafkaProducer[String, String], records: List[ProducerRecord[String, String]], errorOnIndex: Int ) = {
    logger.info(s"Sending Records")
    Try (
      records.zipWithIndex.foreach {
        case (record: ProducerRecord[String,String], key: Int) =>
          if(key == errorOnIndex){
            throw new Exception(s"Throwing error on index ${key}")
          } else {
            producer.send(record).get()
            logger.info(s"Sent ${record}")
          }
      }
    )
  }
  def getUserInput: (Boolean, Int) = {
    def isTransactional() = {
      println(
        s"""
          |What type of message would you like to Producer?
          |Enter number associated with the type:
          |0. Transactional
          |1. Non-Transactional
          |""".stripMargin)

      if (scala.io.StdIn.readInt() == 0) true else false
    }
    def getIndexToErrorOn() = {
      println(
        """
          |How many records would you like the producer to send before it errors out?
          |Enter number associated with your request:
          |-1. None
          |0. 0
          |1. 1
          |2. 2
          |""".stripMargin
      )
      scala.io.StdIn.readInt()
    }
    val transactional = UserInputHandler.getUserInput(isTransactional)
    val errorAfterIndex = UserInputHandler.getUserInput(getIndexToErrorOn)

    (transactional, errorAfterIndex)
  }
  def getEvents: List[ProducerRecord[String,String]] = {
    val events : List[ProducerRecord[String,String]] = for {
      topic <- Configuration.conf.kafka.producerTopics
    } yield {
      new ProducerRecord[String,String](topic, randomUUID().toString, getTimeAndDate)
    }
    events
  }
  private def getTimeAndDate: String = {
    val format = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy")
    format.format(Calendar.getInstance().getTime)
  }
}
