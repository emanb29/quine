package com.thatdot.quine.app.ingest.serialization

import java.net.URL

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.Timeout

import com.typesafe.scalalogging.LazyLogging

import com.thatdot.quine.graph.cypher
import com.thatdot.quine.graph.cypher.{
  Expr,
  Parameters,
  ProcedureExecutionLocation,
  QueryContext,
  Type,
  UserDefinedProcedure,
  UserDefinedProcedureSignature,
  Value
}
import com.thatdot.quine.model.QuineId
import com.thatdot.quine.util.StringInput.filenameOrUrl

/** Parse a protobuf message into a Cypher map according to a schema provided by a schema cache.
  * Because loading the schema is asynchronous, this must be a procedure rather than a function.
  */
class CypherParseProtobuf(private val cache: ProtobufParser.Cache) extends UserDefinedProcedure with LazyLogging {
  def name: String = "parseProtobuf"

  def canContainUpdates: Boolean = false

  def isIdempotent: Boolean = true

  def canContainAllNodeScan: Boolean = false

  def call(context: QueryContext, arguments: Seq[Value], location: ProcedureExecutionLocation)(implicit
    parameters: Parameters,
    timeout: Timeout
  ): Source[Vector[Value], _] = {
    val (bytes, schemaUrl, typeName): (Array[Byte], URL, String) = arguments match {
      case Seq(Expr.Bytes(bytes, bytesRepresentId), Expr.Str(schemaUrl), Expr.Str(typeName)) =>
        if (bytesRepresentId)
          logger.info(
            s"""Received an ID (${QuineId(bytes).pretty(location.idProvider)}) as a source of
                 |bytes to parse a protobuf value of type: $typeName.""".stripMargin.replace('\n', ' ')
          )
        (bytes, filenameOrUrl(schemaUrl), typeName)
      case _ =>
        throw wrongSignature(arguments)
    }
    Source.future(cache.getFuture(schemaUrl, typeName)).map { parser =>
      Vector(parser.parseBytes(bytes))
    }
  }

  def signature: UserDefinedProcedureSignature = UserDefinedProcedureSignature(
    arguments = Seq("bytes" -> Type.Bytes, "schemaUrl" -> Type.Str, "typeName" -> Type.Str),
    outputs = Seq("value" -> cypher.Type.Map),
    description = "Parses a protobuf message into a Cypher map value"
  )
}
