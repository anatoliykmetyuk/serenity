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


object IO extends IO
trait IO {
  def readFile[R: Eval |= ?](path: Path): Eff[R, String] = for {
    res <- delay {  // TODO: abstract suspended computations
        fs2.io.file.readAll[Task](path, 4096)
          .through(text.utf8Decode)
          .runLog.map { _.mkString }.unsafeRun()
      }
  } yield res

  def writePost[R: Eval |= ?](payload: String, path: Path): Eff[R, Unit] = for {
    _ <- delay {
        path.toFile.getParentFile.mkdirs()

        Stream.emit[Task, String](payload)  // TODO: this can throw exceptions
          .through(text.utf8Encode)
          .through(fs2.io.file.writeAll(path))
          .run.unsafeRun()
      }
  } yield ()

  def readYaml[R](path: Path)(implicit E1: Eval |= R, E2: Either[String, ?] |= R): Eff[R, Json] = for {
    raw    <- readFile(path)
    // TODO: read the "date" metadata from the file name
    parsed <- (if (path.toString.reverse.takeWhile(_ != '.').reverse == "yaml") parsers.yaml[R] else parsers.yamlWithContent[R])(raw)
  } yield parsed

  // TODO: do recursive directory parsing with Matryoshka
  def readYamlDir[R](path: Path)(implicit E1: Eval |= R, E2: Either[String, ?] |= R): Eff[R, Json] = {
    val contents = path.toFile.listFiles.toList
    
    def stripExt(name: String): String =
      if (!name.contains('.')) name else name.reverse.dropWhile(_ != '.').drop(1).reverse

    def processFiles(fs: List[File], parser: Path => Eff[R, Json]): Eff[R, Json] =
      Monad[Eff[R, ?]].tailRecM[(Json, List[File]), Json](Json.fromJsonObject(JsonObject.empty) -> fs) {
        case (acc, f :: fx) => parser(f.toPath).map { res => Left( acc.deepMerge( Json.obj(stripExt(f.getName) -> res) ) -> fx ) }
        case (acc, Nil    ) => pure(Right(acc))
      }

    for {
      files <- processFiles(contents.filter(!_.isDirectory), readYaml   [R])
      dirs  <- processFiles(contents.filter( _.isDirectory), readYamlDir[R])
    } yield files.deepMerge(dirs)
  }
}
