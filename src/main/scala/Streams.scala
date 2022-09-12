import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Transactional}

import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.Logger

object Streams extends App {

  val logger: Logger = Logger("Streams")
  implicit val system: ActorSystem = ActorSystem("transactional-streaming-demo")

  getUserInput match {
    case 0 => transactionAwareStream
    case 1 => transactionUnawareStream
    case 2 => transactionalSourceStream
  }

  def transactionUnawareStream = {
    simpleCommittableSourceFlow(Configuration.transactionUnawareConsumerStreamingConfig, "TRANSACTION UNAWARE STREAM").run()
  }
  def transactionAwareStream = {
    simpleCommittableSourceFlow(Configuration.transactionAwareConsumerStreamingConfig, "TRANSACTION AWARE STREAM").run()
  }
  def transactionalSourceStream = {
    Transactional.source(
      Configuration.transactionUnawareConsumerStreamingConfig,
      Subscriptions.topics(Configuration.conf.kafka.consumerTopics.toSet)
    )
    .to(Sink.foreach(transactionalMessage => {
      logger.info(
        s"""
           |=====================
           | TRANSACTIONAL SOURCE STREAM
           | =====================
           |Reading from topic ${transactionalMessage.record.topic()}
           |Reading record with key: ${transactionalMessage.record.key()}
           |Reading record with value: ${transactionalMessage.record.value()}
           |Complete record: ${transactionalMessage.record}
           |=====================
           |""".stripMargin)
    }))
    .run()
  }
  private def simpleCommittableSourceFlow(settings: ConsumerSettings[String, String], logHeader: String) = {
    Consumer
      .committableSource(
        settings,
        Subscriptions.topics(Configuration.conf.kafka.consumerTopics.toSet)
      )
      .to(Sink.foreach(committableMessage => {
        logger.info(
          s"""
             |=====================
             | ${logHeader}
             | =====================
             |Reading from topic ${committableMessage.record.topic()}
             |Reading record with key: ${committableMessage.record.key()}
             |Reading record with value: ${committableMessage.record.value()}
             |Complete record: ${committableMessage.record}
             |=====================
             |""".stripMargin)
      }
      ))
  }
  def getUserInput: (Int) = {
    def getStreamToCreate(): Int = {
      println(
        s"""
           |What type of stream would you like to start?
           |Enter number associated with the type:
           |0. Transaction Aware Stream
           |1. Transaction Unaware Stream
           |2. Transaction Aware Stream using Transactional Source
           |""".stripMargin)
      val answer = scala.io.StdIn.readInt()
      if((0 to 2).contains(answer)){
        answer
      } else {
        throw new Exception("Out of range")
      }
    }
    UserInputHandler.getUserInput(getStreamToCreate)
  }
}
