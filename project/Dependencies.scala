object Dependencies {
  val pekkoV = "1.0.3"
  val pekkoHttpV = "1.0.1"
  val pekkoManagementV = "1.0.0"
  val pekkoStreamV = "1.0.3"
  val pekkoKafkaV = "1.0.0"
  val pekkoConnectorsV = "1.0.2"
  val apacheCommonsCompressV = "1.27.1"
  val antlrV = "4.9.2"
  val amazonKinesisClientV = "2.6.0"
  val awsSdkV = "2.26.27"
  val amazonGlueV = "1.1.20"
  val betterMonadicForV = "0.3.1"
  val boopickleV = "1.4.0"
  val bootstrapV = "5.3.3"
  val caffeineV = "3.1.8"
  val cassandraClientV = "4.18.1"
  val catsV = "2.12.0"
  val catsEffectV = "3.5.4"
  val circeYamlV = "0.16.0"
  val commonsCodecV = "1.17.1"
  val commonsTextV = "1.12.0"
  val commonsIoV = "2.16.1"
  val dropwizardMetricsV = "4.2.27"
  val embeddedCassandraV = "4.1.0"
  val endpoints4sDefaultV = "1.11.1"
  val endpoints4sHttpServerV = "1.0.1"
  val endpoints4sOpenapiV = "4.5.1"
  val endpoints4sXhrClientV = "5.3.0"
  val flatbuffersV = "24.3.25"
  val guavaV = "33.3.0-jre"
  val ioniconsV = "2.0.1"
  val jnrPosixV = "3.1.19"
  val jqueryV = "3.6.3"
  val jwtScalaV = "10.0.1"
  // pekko-connectors 1.0.1 requires 3.0.1, which is vulnerable to CVE-2022-34917
  val kafkaClientsV = "3.8.0"
  val kindProjectorV = "0.13.3"
  val logbackConfigV = "0.4.0"
  val logbackV = "1.5.7"
  val logstashLogbackV = "8.0"
  val lz4JavaV = "1.8.0" // Try to keep this in sync w/ the version kafka-client depends on.
  val mapDbV = "3.1.0"
  val memeIdV = "0.8.0"
  val metricsInfluxdbV = "1.1.0"
  val msgPackV = "0.9.8"
  val openApiCirceYamlV = "0.10.0"
  val openCypherV = "9.2.3"
  val parboiledV = "1.4.1"
  val pegdownV = "1.6.0"
  val plotlyV = "1.57.1"
  val pprintV = "0.9.0"
  val protobufV = "3.25.4"
  val protobufCommonV = "2.14.2"
  val pureconfigV = "0.17.7"
  val quineQueryV = "0.1.11"
  val reactPlotlyV = "2.5.1"
  val reactV = "17.0.2"
  val rocksdbV = "9.0.0"
  val scaffeineV = "5.3.0"
  val scalaCheckV = "1.18.0"
  val scalaJavaTimeV = "2.6.0"
  val scalaLoggingV = "3.9.5"
  val scalaParserCombinatorsV = "2.4.0"
  val scalaTestScalaCheckV = "3.2.18.0"
  val scalajsDomV = "2.8.0"
  val scalaTestV = "3.2.19"
  val scalajsMacroTaskExecutorV = "1.1.1"
  val scoptV = "4.1.0"
  val shapelessV = "2.3.12"
  val slinkyV = "0.7.4"
// pekko-connectors-kafka 1.0.0 depends on an older but compatible version with vulnerabilities CVE-2023-34453,
  // CVE-2023-34454, CVE-2023-34455. Pin this until the indirect dependency version is changed to not have these
  // vulnerabilities.
  val snappyV = "1.1.10.5"
  val sugarV = "2.0.6"
  val stoplightElementsV = "7.12.0"
  val tapirRecdocV = "1.10.10"
  val tapirV = "1.10.10"
  val ujsonV = "1.6.0"
  val circeV = "0.14.9"
  val circeOpticsV = "0.15.0"
  val visNetworkV = "8.2.0"
  val webjarsLocatorV = "0.52"
}
