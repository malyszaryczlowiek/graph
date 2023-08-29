package io.github.malyszaryczlowiek
package runners


import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.spark.sql.{KeyValueGroupedDataset, Row, SparkSession}
import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.sql.types.{StructField, StructType}

import scala.util.Random // graphx._
// To make some of the examples work we will also need RDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types._

import config.AppConfig.dbConfig
import mappers.Mappers._
import encoders.GraphEncoders._



class DbGraphRunner {

  private val logger: Logger = LogManager.getLogger(classOf[GraphRunner])

  def run(): Unit = {

    logger.trace(s"Starting app")

    val sparkSession = SparkSession
      .builder
      .appName("SparkGraphX")
      // two config below added to solve
      // Initial job has not accepted any resources;
      // check your cluster UI to ensure that workers
      // are registered and have sufficient resources
      //      .config("spark.shuffle.service.enabled", "false")
      //      .config("spark.dynamicAllocation.enabled", "false")
      .master("local[2]")
      //  .master("spark://spark-master:7077")    // option for cluster  spark://spark-master:7077
      .getOrCreate()

    val sc = sparkSession.sparkContext

    /*
    vertexy to będą użytkownicy
    edges to będą czaty - połączenia między nimi.
    */


    val usersSchema = StructType(
      StructField("user_id",          StringType, nullable = false)
        :: StructField("user_num_id", LongType,   nullable = false)
        :: Nil)


    val users = sparkSession.read
      .format("jdbc")
      .option("url",      dbConfig.dbUrlWithSchema)
      .option("dbtable",  "users")
      .option("user",     dbConfig.user)
      .option("password", dbConfig.pass)
      .schema( usersSchema )
      .load()


    //val vertices: VertexRDD[String] =
    val vertices: RDD[(VertexId, String)] = users.rdd.map( toVertex ) // todo to jeszcze trzeba napisac w pliku Mappers
      // users.map( toVertex )( vertexEncoder ).rdd// rdd.map( toVertex )



    val schema = StructType(
      StructField("user_id", StringType, nullable = false)
        :: StructField("chat_id", StringType, nullable = false)
        :: Nil)


    val usersAndChats = sparkSession.read
      .format("jdbc")
      .option("url",      dbConfig.dbUrlWithSchema)
      .option("dbtable",  "users_chats")
      .option("user",     dbConfig.user)
      .option("password", dbConfig.pass)
      .schema( schema )
      .load()


    // to co będzie trzeba obliczyć to jaki procent połączeń obejmuje
    // użytkowników oddalonych od siebie o więcej niż 6 użytkowników

    // algorytm
    /*
    iterujemy po użytkownikach
    mamy użytkownika start i teraz dla każdego startującego
    iterujemy po kolejnym użytkowniku i sparwdzamy czy liczba węzłów jest mniejsza
    lub równa 6 jeśli tak to dodajemy do
     - lower
    jeśli nie jest to dodajemy do
     - upper

    do takich obliczeń należy użyć GraphOps
     */


    // for using syntax: $"column_name"
    import sparkSession.implicits._

    // val groupedChats = usersAndChats.groupBy( $"chat_id")
    val groupedChats2: KeyValueGroupedDataset[String, Row] =
      usersAndChats.groupByKey(r => r.getAs[String](s"chat_id"))


    val edges: RDD[Edge[String]] = groupedChats2.flatMapGroups( (chatId: String, rowIterator: Iterator[Row]) => {
      // i tutaj trzeba po iteratorze wyłuskać wszystkie user_num_id z row'ów
      // i utworzyć listę par z user_num_id
      // następnie tę listę należy flatMappowac aby uzyskać właściwą RDD
      val userNumIds = List.empty[Long]
      while (rowIterator.hasNext) {
        val r = rowIterator.next()
        r.getAs[VertexId](s"user_num_id") :: userNumIds
      }
      val u = userNumIds.distinct
      // i teraz z tej listy tworzymy pary niepowtarzające się

      u.zip(u).map( t => {
        if (t._1 < t._2) t
        else (t._2, t._1)
      }).distinct.map( t => (t._1, t._2, chatId))
    }).map(t => Edge(t._1, t._2, t._3)).rdd


    // teraz tworzymy graf
    val graph: Graph[String, String] = Graph(vertices, edges)


    val ops = new GraphOps[String, String](graph)


    val pageRanked  = graph.pageRank(0.001)
    val pageRankRev = graph.reverse.pageRank(0.001).vertices

    val pg = pageRanked.joinVertices(pageRankRev)((vid: VertexId, pg1: Double, pg2: Double) => (pg1 + pg2) / 2)



    pg.vertices
      .toDS()
      .foreach((vidAndValye) => {
        val vid   = vidAndValye._1
        val value = vidAndValye._2
        // TODO i to pg teraz trzeba zapisać do DB


      })







  }




}
