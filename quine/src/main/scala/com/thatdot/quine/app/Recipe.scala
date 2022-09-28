package com.thatdot.quine.app

import java.io.File
import java.net.HttpURLConnection.{HTTP_MOVED_PERM, HTTP_MOVED_TEMP}
import java.net.{HttpURLConnection, MalformedURLException, URL, URLEncoder}

import scala.util.Using
import scala.util.control.Exception.catching

import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import endpoints4s.generic.docs

import com.thatdot.quine.app.yaml.parseToJson
import com.thatdot.quine.routes._
import com.thatdot.quine.routes.exts.UjsonAnySchema

import StandingQueryResultOutputUserDef._

@docs("A specification of a Quine Recipe")
final case class Recipe(
  @docs("Schema version (only supported value is 1)") version: Int,
  @docs("Identifies the Recipe but is not necessarily unique") title: String,
  @docs(
    "URL to social profile of the person or organization responsible for this Recipe"
  ) contributor: Option[String],
  @docs("Brief copy about this Recipe") summary: Option[String],
  @docs("Longer form copy about this Recipe") description: Option[String],
  @docs("URL to image asset for this Recipe") iconImage: Option[String],
  @docs("Ingest Streams that load data into the graph") ingestStreams: List[IngestStreamConfiguration],
  @docs(
    "Standing Queries that respond to graph updates by computing aggregates and trigger actions"
  ) standingQueries: List[StandingQueryDefinition],
  @docs("For web UI customization") nodeAppearances: List[UiNodeAppearance],
  @docs("For web UI customization") quickQueries: List[UiNodeQuickQuery],
  @docs("For web UI customization") sampleQueries: List[SampleQuery],
  @docs("Cypher query to be run periodically while Recipe is running") statusQuery: Option[StatusQuery]
)

@docs("A Cypher query to be run periodically while Recipe is running")
final case class StatusQuery(cypherQuery: String)

private object RecipeSchema
    extends endpoints4s.ujson.JsonSchemas
    with endpoints4s.generic.JsonSchemas
    with IngestSchemas
    with StandingQuerySchemas
    with QueryUiConfigurationSchemas
    with UjsonAnySchema {

  implicit lazy val printQuerySchema: JsonSchema[StatusQuery] =
    genericJsonSchema[StatusQuery]

  implicit lazy val recipeSchema: JsonSchema[Recipe] =
    genericJsonSchema[Recipe]
}

object Recipe {

  import RecipeSchema._

  def fromJson(json: ujson.Value): Either[Seq[String], Recipe] = for {
    jsonObj <- json.objOpt.map(ujson.Obj(_)) toRight Seq("Recipe must be an object, got: " + json)
    recipe <- recipeSchema.decoder.decode(jsonObj).toEither
  } yield recipe

  /** Indicates an error due to a missing recipe variable.
    *
    * TODO: consider adding information here about where the error occurred
    *
    * @param name name of the missing variable
    */
  final case class UnboundVariableError(name: String)

  /** Produces a copy of the Recipe with all tokens substituted with defined values. Only certain
    * predetermined Recipe fields are processed in this way.
    *
    * If a token is undefined, it will be added to the list of failures in the output.
    *
    * @param recipe parsed recipe AST
    * @param values variables that may be substituted
    * @return substituted recipe or all of the substitution errors
    */
  def applySubstitutions(recipe: Recipe, values: Map[String, String]): ValidatedNel[UnboundVariableError, Recipe] = {
    // Implicit classes so that .subs can be used below.
    implicit class Subs(s: String) {
      def subs: ValidatedNel[UnboundVariableError, String] = applySubstitution(s, values)
    }
    implicit class SubCreds(c: AwsCredentials) {
      def subs: ValidatedNel[UnboundVariableError, AwsCredentials] =
        (
          c.region.subs,
          c.accessKeyId.subs,
          c.secretAccessKey.subs
        ).mapN(AwsCredentials(_, _, _))
    }
    implicit class SubStandingQueryOutputSubs(soo: StandingQueryResultOutputUserDef) {
      def subs: ValidatedNel[UnboundVariableError, StandingQueryResultOutputUserDef] = soo match {
        case Drop => Validated.valid(Drop)
        case PostToEndpoint(url, parallelism, onlyPositiveMatchData) =>
          (
            url.subs
          ).map(PostToEndpoint(_, parallelism, onlyPositiveMatchData))
        case WriteToKafka(topic, bootstrapServers, format) =>
          (
            topic.subs,
            bootstrapServers.subs
          ).mapN(WriteToKafka(_, _, format))
        case WriteToSNS(credentials, topic) =>
          (
            credentials.traverse(_.subs),
            topic.subs
          ).mapN(WriteToSNS(_, _))
        case PrintToStandardOut(logLevel, logMode) =>
          Validated.valid(PrintToStandardOut(logLevel, logMode))
        case WriteToFile(path) =>
          (
            path.subs
          ).map(WriteToFile(_))
        case PostToSlack(hookUrl, onlyPositiveMatchData, intervalSeconds) =>
          (
            hookUrl.subs
          ).map(PostToSlack(_, onlyPositiveMatchData, intervalSeconds))
        case StandingQueryResultOutputUserDef
              .CypherQuery(query, parameter, parallelism, andThen, allowAllNodeScan, shouldRetry) =>
          (
            query.subs,
            andThen.traverse(_.subs)
          ).mapN(
            StandingQueryResultOutputUserDef.CypherQuery(
              _,
              parameter,
              parallelism,
              _,
              allowAllNodeScan,
              shouldRetry
            )
          )
        case WriteToKinesis(
              credentials,
              streamName,
              format,
              kinesisParallelism,
              kinesisMaxBatchSize,
              kinesisMaxRecordsPerSecond,
              kinesisMaxBytesPerSecond
            ) =>
          (
            credentials.traverse(_.subs),
            streamName.subs
          ).mapN(
            WriteToKinesis(
              _,
              _,
              format,
              kinesisParallelism,
              kinesisMaxBatchSize,
              kinesisMaxRecordsPerSecond,
              kinesisMaxBytesPerSecond
            )
          )
      }
    }
    implicit class IngestStreamsConfigurationSubs(soo: IngestStreamConfiguration) {
      def subs: ValidatedNel[UnboundVariableError, IngestStreamConfiguration] = soo match {
        case KafkaIngest(
              format,
              topics,
              parallelism,
              bootstrapServers,
              groupId,
              securityProtocol,
              autoCommitIntervalMs,
              autoOffsetReset,
              endingOffset,
              maximumPerSecond
            ) =>
          (
            bootstrapServers.subs
          ).map(
            KafkaIngest(
              format,
              topics,
              parallelism,
              _,
              groupId,
              securityProtocol,
              autoCommitIntervalMs,
              autoOffsetReset,
              endingOffset,
              maximumPerSecond
            )
          )
        case KinesisIngest(
              format,
              streamName,
              shardIds,
              parallelism,
              credentials,
              iteratorType,
              numRetries,
              maximumPerSecond
            ) =>
          (
            streamName.subs,
            credentials.traverse(_.subs)
          ).mapN(
            KinesisIngest(
              format,
              _,
              shardIds,
              parallelism,
              _,
              iteratorType,
              numRetries,
              maximumPerSecond
            )
          )
        case ServerSentEventsIngest(format, url, parallelism, maximumPerSecond) =>
          (
            url.subs
          ).map(ServerSentEventsIngest(format, _, parallelism, maximumPerSecond))
        case SQSIngest(
              format,
              queueUrl,
              readParallelism,
              writeParallelism,
              credentials,
              deleteReadMessages,
              maximumPerSecond
            ) =>
          (
            queueUrl.subs,
            credentials.traverse(_.subs)
          ).mapN(
            SQSIngest(
              format,
              _,
              readParallelism,
              writeParallelism,
              _,
              deleteReadMessages,
              maximumPerSecond
            )
          )
        case WebsocketSimpleStartupIngest(format, wsUrl, initMessages, keepAliveProtocol, parallelism, encoding) =>
          (
            wsUrl.subs,
            initMessages.toList.traverse(_.subs)
          ).mapN(
            WebsocketSimpleStartupIngest(
              format,
              _,
              _,
              keepAliveProtocol,
              parallelism,
              encoding
            )
          )
        case FileIngest(
              format,
              path,
              encoding,
              parallelism,
              maximumLineSize,
              startAtOffset,
              ingestLimit,
              maximumPerSecond,
              fileIngestMode
            ) =>
          (
            path.subs
          ).map(
            FileIngest(
              format,
              _,
              encoding,
              parallelism,
              maximumLineSize,
              startAtOffset,
              ingestLimit,
              maximumPerSecond,
              fileIngestMode
            )
          )
        case i: StandardInputIngest => Validated.valid(i)
        case i: NumberIteratorIngest => Validated.valid(i)
      }
    }

    // Return a copy of the recipe.
    // Selected fields are token substituted by invoking subs.
    (
      recipe.ingestStreams.traverse(_.subs),
      recipe.standingQueries.traverse(sq =>
        for {
          outputsS <- sq.outputs.toList
            .traverse { case (k, v) => v.subs.map(k -> _) }
            .map(_.toMap)
        } yield sq.copy(outputs = outputsS)
      )
    ).mapN((iss, sqs) => recipe.copy(ingestStreams = iss, standingQueries = sqs))
  }

  /** Extremely simple token substitution language.
    *
    * If the first character in the input string equals '$', then the string
    * represents a token that is to be substituted.
    *
    * The token's value is read from the values map. If the value is not defined,
    * an error occurs.
    *
    * Internal substitutions are not supported.
    *
    * Double leading '$' characters ("$$") escapes token substitution and is
    * interpreted as a single leading '$'.
    */
  def applySubstitution(input: String, values: Map[String, String]): ValidatedNel[UnboundVariableError, String] =
    if (input.startsWith("$")) {
      val key = input.slice(1, input.length)
      if (input.startsWith("$$")) {
        Validated.valid(key)
      } else {
        values.get(key) match {
          case None => Validated.invalid(UnboundVariableError(key)).toValidatedNel
          case Some(value) => Validated.valid(value)
        }
      }
    } else {
      Validated.valid(input)
    }

  /** Synchronously maps a string that identifies a Recipe to the actual Recipe
    * content as a parsed and validated document.
    *
    * The string is resolved as follows:
    * 1. A string that is a valid URL is determined to be a URL
    * 2. A string that is not a valid URL and ends with .json, .yaml, or .yml is determined to be a local file
    * 3. Any other string is determined to be a Recipe canonical name
    *
    * Recipe canonical name is resolved to URL at githubusercontent.com
    * via URL redirect service at recipes.quine.io.
    *
    * Any errors are converted to a sequence of user-facing messages.
    */
  def get(recipeIdentifyingString: String): Either[Seq[String], Recipe] = {
    val recipeFileExtensions = List(".json", ".yaml", ".yml")
    val recipeRedirectServiceUrlPrefix = "https://recipes.quine.io/"
    val requiredRecipeContentUrlPrefix = "https://raw.githubusercontent.com/thatdot/quine/main/"
    for {
      urlToRecipeContent <- catching(classOf[MalformedURLException]).opt(new URL(recipeIdentifyingString)) match {
        case Some(url: URL) =>
          Right(url)
        case None if recipeFileExtensions exists (recipeIdentifyingString.toLowerCase.endsWith(_)) =>
          Right(new File(recipeIdentifyingString).toURI.toURL)
        case None =>
          val recipeIdentifyingStringUrlEncoded: String =
            URLEncoder.encode(recipeIdentifyingString, "UTF-8")
          val urlToRedirectService = new URL(recipeRedirectServiceUrlPrefix + recipeIdentifyingStringUrlEncoded)
          implicit val releaseableHttpURLConnection: Using.Releasable[HttpURLConnection] =
            (resource: HttpURLConnection) => resource.disconnect()
          Using(urlToRedirectService.openConnection.asInstanceOf[HttpURLConnection]) { http: HttpURLConnection =>
            http.setInstanceFollowRedirects(false)
            http.getResponseCode match {
              case HTTP_MOVED_PERM =>
                val location = http.getHeaderField("Location")
                if (!location.startsWith(requiredRecipeContentUrlPrefix))
                  Left(Seq(s"Unexpected redirect URL $location"))
                else
                  Right(new URL(location))
              // Redirect service indicates not found using HTTP 302 Temporary Redirect
              case HTTP_MOVED_TEMP =>
                Left(Seq(s"Recipe $recipeIdentifyingString does not exist; please visit https://quine.io/recipes"))
              case c @ _ => Left(Seq(s"Unexpected response code $c from URL $urlToRedirectService"))
            }
          }.toEither.left.map(e => Seq(e.toString)).joinRight
      }
      json <- Using(urlToRecipeContent.openStream)(parseToJson).toEither.left.map(e => Seq(e.toString))
      recipe <- Recipe.fromJson(json)
      validatedRecipe <- isCurrentVersion(recipe)
    } yield validatedRecipe
  }

  /** Fetch the recipe using the identifying string and then apply substitutions
    *
    * @param recipeIdentifyingString URL, file path, or canonical name of recipe
    * @param values variables for substitution
    * @return either all of the errors, or the parsed and substituted recipe
    */
  def getAndSubstitute(recipeIdentifyingString: String, values: Map[String, String]): Either[Seq[String], Recipe] =
    for {
      recipe <- get(recipeIdentifyingString)
      substitutedRecipe <- applySubstitutions(recipe, values).toEither.left
        .map(_.toList.distinct.map(e => s"Missing required parameter ${e.name}; use --recipe-value ${e.name}="))
    } yield substitutedRecipe

  private val version = 1

  private def isCurrentVersion(recipe: Recipe): Either[Seq[String], Recipe] = Either.cond(
    recipe.version == version,
    recipe,
    Seq(s"The only supported Recipe version number is $version")
  )
}
