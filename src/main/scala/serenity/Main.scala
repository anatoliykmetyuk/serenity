package serenity

import java.nio.file.{ Paths, Path }
import java.io.File

import cats._
import cats.data._
import cats.syntax.all._
import cats.instances.all._

import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

import fs2.{ Task, Stream, text }

import _root_.io.circe.yaml.{ parser => yamlParser }
import _root_.io.circe.{ Json, ParsingFailure, JsonObject }

import serenity.IO._

object Main {
  val workdir  = "workdir/"
  val postName = s"$workdir/_posts/2017-01-11-rewriting-process-algebra-part-1-introduction-to-process-algebra.md"
  val includes = s"$workdir/_includes"

  def main(args: Array[String]): Unit = {
    val processPosts: Effects[Unit] = for {
      foo <- readYamlDir[Stack](Paths.get(workdir))
      _   <- delay[Stack, Unit] { println(foo) }
    } yield ()

    processPosts.runEval.runEither.run
  }
}