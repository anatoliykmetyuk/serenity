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


object Layout extends Layout with Config with IO with Liquid
trait Layout { this: Config with IO with Liquid =>
  def layout(name: String, content: String): Stuff[String] = for {
    cfg             <- config
    layoutsPath     <- EitherT(Eval.later { cfg.hcursor.get[String]("layouts").leftMap(_.getMessage) })
    layoutCfg        = cfg.deepMerge( Json.fromJsonObject(JsonObject.singleton("content", Json.fromString(content))) )
    layoutRaw       <- readFile(s"$workdirPath/$layoutsPath/$name.html")
    (headerCfg, lt)  = maybeSplit(layoutRaw)
    liquified       <- solid(lt, layoutCfg)
    result          <- (for {
                      hcfg    <- headerCfg
                      hlayout <- hcfg.hcursor.get[String]("layout").toOption
                    } yield layout(hlayout, liquified)).getOrElse(point[Stuff, String](liquified))
  } yield result

  def maybeSplit(str: String): (Option[Json], String) = if (str.startsWith("---\n")) {  // TODO: should return Eval (or maybe it is good as is?)
    val lines     = str.split("\n").toList.drop(1)
    val cfg       = yamlParser.parse(lines.takeWhile(_ != "---").mkString("\n")).toOption
    val remainder = lines.dropWhile(_ != "---").drop(1).mkString("\n")
    (cfg, remainder)
  } else (None, str)
}