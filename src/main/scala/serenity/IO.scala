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


object IO extends IO
trait IO {
  def readFile(path: String): Stuff[String] = EitherT(Eval.later {
    println(s"Processing '$path'")
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
}