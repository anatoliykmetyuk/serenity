val ScalaVer = "2.12.1"

val Cats          = "0.9.0"
val Shapeless     = "2.3.2"
val Scalacheck    = "1.13.4"
val KindProjector = "0.9.3"
val FS2           = "0.9.4"
val Matryoshka    = "0.16.5"

val Scalatest = "3.0.1"
val Circe     = "0.7.0"
val CirceYaml = "0.5.0"
val Eff       = "3.0.4"

val ScalacheckMinTests = 1000

lazy val commonSettings = Seq(
  name    := "serenity"
, version := "0.1.0"
, scalaVersion := ScalaVer
, libraryDependencies ++= Seq(
    "org.typelevel"  %% "cats"              % Cats
  , "org.typelevel"  %% "cats-free"         % Cats
  , "com.chuusai"    %% "shapeless"         % Shapeless
  , "co.fs2"         %% "fs2-core"          % FS2
  , "co.fs2"         %% "fs2-io"            % FS2
  // , "com.slamdata"   %% "matryoshka-core"   % Matryoshka
  , "org.scalacheck" %% "scalacheck"        % Scalacheck  % "test"

  , "org.scalatest"  %% "scalatest" % Scalatest % "test"

  , "io.circe" %% "circe-core"    % Circe
  , "io.circe" %% "circe-generic" % Circe
  , "io.circe" %% "circe-parser"  % Circe
  , "io.circe" %% "circe-yaml"    % CirceYaml

  , "org.atnos" %% "eff" % Eff
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
, testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", ScalacheckMinTests.toString, "-workers", "10", "-verbosity", "1")
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    initialCommands := "import serenity._"
  )
