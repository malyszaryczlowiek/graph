package io.github.malyszaryczlowiek
package mappers

import org.apache.spark.graphx.{Edge, VertexId, VertexRDD}
import org.apache.spark.sql.Row

class Mappers
object Mappers {

  def toVertex(r: Row): Row => (VertexId, String) = (r: Row) => {
    val userId = r.getAs[String](s"user_id")
    val userNumId = r.getAs[Long](s"user_num_id")
    // ( userNumId , userId)
    vertexEncod
  }

}
