name := "kafka-transactions-investigation"

version := "0.1"
val AkkaVersion = "2.6.18"

fork := true
connectInput in run := true

libraryDependencies ++= Seq(
//  Config
  "com.github.pureconfig" %% "pureconfig" % "0.17.1",
//   Kafka
  "org.apache.kafka" %% "kafka" % "3.2.0",
  "org.apache.kafka" % "kafka-streams" % "3.2.0",
//  Logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
//  Akka
  "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion

)

scalaVersion := "2.13.8"
