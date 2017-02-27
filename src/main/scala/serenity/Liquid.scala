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


object Liquid extends Liquid with IO
trait Liquid { this: IO =>
  def solid(payload: String, cfg: Json): Stuff[String] = plugins.flatMap { plgns =>
    EitherT[Eval, String, String](  // TODO: seriously abstract this lifting of Eval to EitherT, and also exception handling
      Monad[Eval].tailRecM[String, String](payload) { p => Eval.later {
        val (res, changed) = (plgns: List[Plugin]).foldLeft[(String, Boolean)]((p, false)) { case ((str, changed), p) => p(str, cfg) match {
          case Some(newString) => (newString, true   )
          case None            => (str      , changed)
        }}
        if (changed) Left (res)
        else         Right(res)
      }}.map(x => Right(x))
    )
  }

  val plugins: Stuff[List[Plugin]] = EitherT.pure[Eval, String, List[Plugin]](List(
    highlighter, include, variable
  ))
}

trait Plugin {
  def apply(s: String, cfg: Json): Option[String] // TODO: must retrun Eval; try to express Option with Writer that logs whether there was an application.
}

object highlighter extends Plugin {
  // TODO: parse tags with Fastparse
  def apply(s: String, cfg: Json): Option[String] = {
    val pattern = """\{% highlight (.*) %\}""".r
    val found   = pattern.findAllIn(s).nonEmpty
    if (found)
      Some(pattern.replaceAllIn(s, m => s"Highlighter '${m.group(1)}' is applied here."))
    else None
  }
}

object include extends Plugin {
  def apply(s: String, cfg: Json): Option[String] = { // TODO: DRY
    val pattern = """\{%\s+include\s+(.*)\s+%\}""".r
    val found   = pattern.findAllIn(s).nonEmpty
    if (found)
      Some( pattern.replaceAllIn(s, m => doInclude(m.group(1), cfg)) )
    else None
  }

  def doInclude(name: String, cfg: Json): String = (for {
    includesPath <- EitherT(Eval.later { cfg.hcursor.get[String]("includes").leftMap(_.getMessage) })
    payload      <- IO.readFile(s"$workdirPath/$includesPath/$name")  // TODO: abstract workdirPath into the IO plugin
  } yield payload).value.value.right.get  // TODO: No.
}

object variable extends Plugin {
  def apply(s: String, cfg: Json): Option[String] = {
    val pattern = """\{\{\s+(.*)\s+\}\}""".r
    val found   = pattern.findAllIn(s).nonEmpty
    if (found)
      Some( pattern.replaceAllIn(s, m => doVar(m.group(1), cfg)) )
    else None
  }

  def doVar(s: String, cfg: Json): String = (for {
    res <- cfg.hcursor.get[String](s)  // TODO: Paths should be supported
  } yield res).right.get
}
