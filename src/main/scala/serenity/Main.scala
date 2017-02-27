package serenity

import java.nio.file.{ Paths, Path }
import java.io.File

import cats._
import cats.data._
import cats.syntax.all._
import cats.instances.all._

import fs2.{ Task, Stream, text }

import io.circe.yaml.{ parser => yamlParser }
import io.circe.{ Json, ParsingFailure }

object Main {
  val workdirPath = "workdir"
  val configPath  = s"$workdirPath/config.yaml"
  val postName    = "2017-01-11-rewriting-process-algebra-part-1-introduction-to-process-algebra"

  type Stuff[A] = EitherT[Eval, String, A]  // TODO: name

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
    """.stripMargin}
    .leftMap(_.getMessage)
  })

  def handle(e: Exception): Either[String, Nothing] = {
    e.printStackTrace  // TODO: proper logging
    Left(e.getMessage)
  }

  val config: Stuff[Json] = for {
    uc <- userConfig
    dc <- defaultConfig
  } yield uc.deepMerge(dc)

  def readPost (path: String): Stuff[String] = EitherT(Eval.later {
    try Right(fs2.io.file.readAll[Task](Paths.get(path), 4096)
      .through(text.utf8Decode)  // TODO: abstract file reading routines
      .runLog.map { _.mkString }.unsafeRun())
    catch { case e: Exception => handle(e) }
  })

  def writePost(payload: String, path: String): Stuff[Unit] = EitherT(Eval.later {  // TODO: abstract this EitherT(Eval(...)) pattern
    try {  // TODO: can Try be converted to Either?
      new File(path).getParentFile.mkdirs()

      Right(Stream.emit[Task, String](payload)
        .through(text.utf8Encode)
        .through(fs2.io.file.writeAll(Paths.get(path)))
        .run.unsafeRun())  // TODO: maybe unsafeRun has an alternative that returns Either? 
    }
    catch { case e: Exception => handle(e) }  // TODO: abstract Exception handling
  })

  def main(args: Array[String]): Unit = {
    val processPosts: Stuff[Unit] = for {
      cfg        <- config
      postsPath  <- EitherT.pure[Eval, String, String]("_posts")
      outputPath <- EitherT.pure[Eval, String, String]("_site" )

      post       <- readPost (s"$workdirPath/$postsPath/$postName.md")
      _          <- writePost(post, s"$workdirPath/$outputPath/blog/$postName.html")
    } yield ()

    println(processPosts.value.value)
  }
}