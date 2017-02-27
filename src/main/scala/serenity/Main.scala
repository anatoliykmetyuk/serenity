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
    |includes: _includes/
    """.stripMargin}
    .leftMap(_.getMessage)
  })

  def handle(e: Exception): Either[String, Nothing] = {
    e.printStackTrace  // TODO: proper logging
    Left(e.getMessage)
  }

  // TODO: Separate config plugin
  val config: Stuff[Json] = for {
    uc <- userConfig
    dc <- defaultConfig
  } yield dc.deepMerge(uc)

  // TODO: Separate IO plugin
  def readFile(path: String): Stuff[String] = EitherT(Eval.later {
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

  // TODO: Separate solid plugin
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
      val pattern = """\{%\s*include\s*(.*)\s*%\}""".r
      val found   = pattern.findAllIn(s).nonEmpty
      if (found)
        Some( pattern.replaceAllIn(s, m => doInclude(m.group(1), cfg)) )
      else None
    }

    def doInclude(name: String, cfg: Json): String = (for {
      includesPath <- EitherT(Eval.later { cfg.hcursor.get[String]("includes").leftMap(_.getMessage) })
      payload      <- readFile(s"/Users/anatolii/Projects/serenity/$workdirPath/$includesPath/$name")  // TODO: abstract workdirPath into the IO plugin
    } yield payload).value.value.right.get  // TODO: No.
  }

  object variable extends Plugin {
    def apply(s: String, cfg: Json): Option[String] = {
      val pattern = """\{\{\s*(.*)\s*\}\}""".r
      val found   = pattern.findAllIn(s).nonEmpty
      if (found)
        Some( pattern.replaceAllIn(s, m => doVar(m.group(1), cfg)) )
      else None
    }

    def doVar(s: String, cfg: Json): String = (for {
      res <- cfg.hcursor.get[String](s)  // TODO: Paths should be supported
    } yield res).right.get
  }

  val plugins: Stuff[List[Plugin]] = EitherT.pure[Eval, String, List[Plugin]](List(
    highlighter, include, variable
  ))

  def main(args: Array[String]): Unit = {
    val processPosts: Stuff[Unit] = for {
      cfg        <- config
      postsPath  <- EitherT(Eval.later { cfg.hcursor.get[String]("posts"      ).leftMap(_.getMessage) })  // TODO: Abstract this mess
      outputPath <- EitherT(Eval.later { cfg.hcursor.get[String]("destination").leftMap(_.getMessage) })  // TODO: Abstract this mess

      post       <- readFile (s"$workdirPath/$postsPath/$postName.md")
      liquified  <- solid(post, cfg)
      _          <- writePost(liquified, s"$workdirPath/$outputPath/blog/$postName.html")
    } yield ()

    println(processPosts.value.value)
  }
}