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

import all._

object Main {
  val postName    = "2017-01-11-rewriting-process-algebra-part-1-introduction-to-process-algebra"
  
  def main(args: Array[String]): Unit = {
    val processPosts: Stuff[Unit] = for {
      cfg        <- config
      postsPath  <- EitherT(Eval.later { cfg.hcursor.get[String]("posts"      ).leftMap(_.getMessage) })  // TODO: Abstract this mess
      outputPath <- EitherT(Eval.later { cfg.hcursor.get[String]("destination").leftMap(_.getMessage) })  // TODO: Abstract this mess
      // TODO: IMPORTANT: this is actually processFile(pathOnTheLineBelow) - read the file with optional YAML config, use that config in `solid`, if there's a `layout` entry in the config, lay it out. Repeat untill nothing changes.
      // TODO: How should the local yaml config be related to the liquid tags of the enclosing templates? Where this config is needed? It contains some metadata - tags, categories etc - so it will probably be needed from other plugins...
      // TODO: Make a routine to read the file into (Option[Json], HTML). Format-specific parsers kick in here; for *.html it is identity.
      // TODO: Then have a `process` routine: process(file, cfg): (Config, HTML). Read the file and a apply liquid tags to it.
      // TODO: Then have `layout`. Layout is recursive. If it detects `layout` entry in the config, it calls process(layout, cfg + "content" -> currentFile).
      post       <- readFile(s"$workdirPath/$postsPath/$postName.md")  // TODO: readFileWithOptionalYamlConfigHeader
      liquified  <- solid(post, cfg)
      laidOut    <- layout("post", liquified)  // TODO: IMPORTANT: turns out, the layout is specified explicitly for posts too...
      
      _          <- writePost(laidOut, s"$workdirPath/$outputPath/blog/$postName.html")
    } yield ()

    println(processPosts.value.value)
  }
}