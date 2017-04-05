val ScalaVer = "2.12.1"

val Cats          = "0.9.0"
val Shapeless     = "2.3.2"
val KindProjector = "0.9.3"

val Circe     = "0.7.0"
val CirceYaml = "0.5.0"

lazy val commonSettings = Seq(
  name         := "serenity"
, version      := "0.1.0-SNAPSHOT"
, organization := "com.functortech"
, scalaVersion := ScalaVer
, libraryDependencies ++= Seq(
    "org.typelevel"  %% "cats"              % Cats
  , "com.chuusai"    %% "shapeless"         % Shapeless

  , "io.circe" %% "circe-core"    % Circe
  , "io.circe" %% "circe-generic" % Circe
  , "io.circe" %% "circe-parser"  % Circe
  , "io.circe" %% "circe-yaml"    % CirceYaml

  , "commons-io" % "commons-io" % "2.5"
  )
, addCompilerPlugin("org.spire-math" %% "kind-projector" % KindProjector)
, scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:experimental.macros",
      "-unchecked",
      // "-Xfatal-warnings",
      "-Xlint",
      // "-Yinline-warnings",
      "-Ywarn-dead-code",
      "-Xfuture",
      "-Ypartial-unification")
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    initialCommands := "import serenity._"
  )
