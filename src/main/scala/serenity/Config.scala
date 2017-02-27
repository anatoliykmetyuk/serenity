package serenity

import java.nio.file.{ Paths, Path }
import java.io.File

import cats._
import cats.data._
import cats.syntax.all._
import cats.instances.all._

import fs2.{ Task, Stream, text }

import io.circe.yaml.{ parser => yamlParser }
import io.circe.{ Json, ParsingFailure, JsonObject }

object Config extends Config
trait Config {
  val configPath  = s"$workdirPath/config.yaml"

  val userConfig: Stuff[Json] = EitherT(Eval.later {  // TODO: use monad transformers; suspend to Eval directly
    fs2.io.file.readAll[Task](Paths.get(configPath), 4096)
      .through(text.utf8Decode)
      .runLog.map { _.mkString }
      .map { x => yamlParser.parse(x) } // TODO: simplify
      .unsafeRun()
      .leftMap(_.getMessage)
  })

  val defaultConfig: Stuff[Json] = EitherT(Eval.later {
    yamlParser.parse {"""
    |posts: _posts/
    |includes: _includes/
    |layouts: _layouts/
    """.stripMargin}
    .leftMap(_.getMessage)
  })

  val config: Stuff[Json] = for {
    uc <- userConfig
    dc <- defaultConfig
  } yield dc.deepMerge(uc)

}
