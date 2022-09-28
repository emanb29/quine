package com.thatdot.quine.app.routes

import scala.util.{Failure, Success, Try}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout

import com.typesafe.scalalogging.LazyLogging
import org.webjars.WebJarAssetLocator

import com.thatdot.quine.app.BuildInfo
import com.thatdot.quine.graph._
import com.thatdot.quine.gremlin.GremlinQueryRunner
import com.thatdot.quine.model.QuineId

/** Main webserver routes for Quine
  *
  * This is responsible for serving up the REST API as well as static resources.
  *
  * @param graph underlying graph
  * @param serviceState quine application state
  * @param currentConfig rendered JSON config
  * @param ec execution context
  * @param timeout timeout
  */
class QuineAppRoutes(
  val graph: LiteralOpsGraph with CypherOpsGraph with StandingQueryOpsGraph,
  val serviceState: AdministrationRoutesState
    with QueryUiConfigurationState
    with StandingQueryStore
    with IngestStreamState,
  val currentConfig: ujson.Value,
  val timeout: Timeout
) extends BaseAppRoutes
    with QueryUiRoutesImpl
    with WebSocketQueryProtocolServer
    with QueryUiConfigurationRoutesImpl
    with LiteralRoutesImpl
    with AdministrationRoutesImpl
    with IngestRoutesImpl
    with StandingQueryRoutesImpl
    with exts.ServerEntitiesWithExamples
    with com.thatdot.quine.routes.exts.UjsonAnySchema
    with LazyLogging {

  val version = BuildInfo.version
  val gremlin: GremlinQueryRunner = GremlinQueryRunner(graph)(timeout)

  val webJarAssetLocator = new WebJarAssetLocator()

  override def hostIndex(qid: QuineId): Int = 0

  /** Serves up the static assets from resources and for JS/CSS dependencies */
  lazy val staticFilesRoute: Route = {
    Directives.pathEndOrSingleSlash {
      getFromResource("web/quine-ui.html")
    } ~
    Directives.path("dashboard" | "docs") {
      getFromResource("web/quine-ui.html")
    } ~
    Directives.path("quine-ui-startup.js") {
      getFromResource("web/quine-ui-startup.js")
    } ~
    Directives.path("favicon.svg") {
      getFromResource("web/favicon.svg")
    } ~
    Directives.extractUnmatchedPath { path =>
      Try(webJarAssetLocator.getFullPath(path.toString)) match {
        case Success(fullPath) => getFromResource(fullPath)
        case Failure(_: IllegalArgumentException) => reject
        case Failure(err) => failWith(err)
      }
    }
  }

  /** OpenAPI route */
  lazy val openApiRoute: Route = QuineAppOpenApiDocsRoutes(graph).route

  /** Rest API route */
  lazy val apiRoute: Route = {
    queryUiRoutes ~
    queryProtocolWS ~
    queryUiConfigurationRoutes ~
    literalRoutes ~
    administrationRoutes ~
    ingestRoutes ~
    standingQueryRoutes
  }
}
