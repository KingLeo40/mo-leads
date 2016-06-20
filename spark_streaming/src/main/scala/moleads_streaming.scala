import kafka.serializer.StringDecoder
import java.util.Date
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.sql._

import org.elasticsearch.spark._
import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._
import com.datastax.driver.core.utils._
import org.json4s._
import org.json4s.jackson.JsonMethods


object MoLeadsStreaming {

    val kafkaBroker = "ec2-52-41-59-147.us-west-2.compute.amazonaws.com:9092"
    val kafkaTopics = "venmo-data"
    val elasticsearchUrl = "ec2-52-41-104-228.us-west-2.compute.amazonaws.com"
    val cassandraHost = "ec2-52-10-45-242.us-west-2.compute.amazonaws.com"

    def main(args: Array[String]) {

        // Create context with 1 second batch interval
        val sparkConf = new SparkConf().setAppName("mo_leads")
        sparkConf.set("es.index.auto.create", "true")
                 .set("es.nodes", elasticsearchUrl)
                 .set("es.mapping.id", "id")
                 .set("spark.cassandra.connection.host", cassandraHost)

        val ssc = new StreamingContext(sparkConf, Seconds(2))

        // Create direct kafka stream with brokers and topics
        val topicsSet = kafkaTopics.split(",").toSet
        val kafkaParams = Map[String, String]("metadata.broker.list" -> kafkaBroker)
        val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet).map(_._2)

        // parse the json records
        val adjacencyRDD = messages.map(
            JsonMethods.parse(_).asInstanceOf[JObject]
        ).map(json => {
            implicit val formats = DefaultFormats
            val id = (json \ "payment_id").extractOpt[Long].getOrElse(0L)
            val time = (json \ "created_time").extractOpt[String].getOrElse("")
            val message = (json \ "message").extractOpt[String].getOrElse("")
            val actor_id = (json \ "actor" \ "id").extractOpt[String].getOrElse("")
            val actor_name = (json \ "actor" \ "name").extractOpt[String].getOrElse("")
            val target_id = (json \\ "target" \ "id").extractOpt[String].getOrElse("")
            val target_name = (json \\ "target" \ "name").extractOpt[String].getOrElse("")
            ActorTargetAdjacency(id, time, message, actor_id, actor_name, target_id, target_name)
        }).filter(_.isValid)
        //adjacencyRDD.print(2)

        // save to Cassandra and ElasticSearch
        adjacencyRDD.foreachRDD{rdd =>
            if (rdd.toLocalIterator.nonEmpty) {
                rdd.saveToCassandra("moleads","adjacency",
                    SomeColumns("id", "time", "message", "actor_id", "actor_name", "target_id", "target_name")
                )
                rdd.saveToEs("moleads/payment")
            }
        }

        // get micro batch word counts and save to Cassandra
        val words = adjacencyRDD.map(_.message).flatMap(
            _.replaceAll("[^a-zA-Z]", " ").split(" ")
        ).map(_.trim).filter(_.length > 2).map(_.toLowerCase)
        val pairs = words.map(word => (word, 1))
        val wordCounts = pairs.reduceByKey(_ + _)
        wordCounts.foreachRDD{ (rdd, time) =>
            val timeDate = new Date(time.milliseconds)
            rdd.map(
                wordCountPair => (Seq(wordCountPair).toMap, "seconds", timeDate)
            ).saveToCassandra("moleads", "word_counts",
                SomeColumns("word_count" append, "period", "time")
            )
        }
        //wordCounts.print()

        // Start the stream computation
        ssc.start()
        ssc.awaitTermination()
    }
}


case class ActorTargetAdjacency(
    id: Long, time: String, message: String, actor_id: String, actor_name: String, target_id: String, target_name: String
) {
    def isValid = {
        id != 0L && time != "" && message != "" && actor_id != "" && target_id != ""
    }
}


/** Lazily instantiated singleton instance of SQLContext */
object SQLContextSingleton {

    @transient  private var instance: SQLContext = _

    def getInstance(sparkContext: SparkContext): SQLContext = {
        if (instance == null) {
            instance = new SQLContext(sparkContext)
        }
        instance
    }
}
