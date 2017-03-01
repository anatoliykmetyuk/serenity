package serenity

import cats._
import cats.data._
import cats.syntax.all._
import cats.instances.all._

import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

import _root_.io.circe.yaml.{ parser => yamlParser }
import _root_.io.circe.{ Json, ParsingFailure, JsonObject }


/** Routines to parse various files to Json. */
object parsers extends Parsers
trait Parsers {
  def yaml[R: Either[String, ?] |= ?]: String => Eff[R, Json] = str =>
    if (str.isEmpty) pure( Json.fromJsonObject(JsonObject.empty) )
    else fromEither( yamlParser.parse(str).leftMap(_.getMessage) )

  def yamlWithContent[R: Either[String, ?] |= ?]: String => Eff[R, Json] =
    splitString(_) match { case (headerRaw, contentRaw) => for {
      header  <- yaml[R].apply(headerRaw)
      content <- pure[R, Json]( Json.obj("content" -> Json.fromString(contentRaw)) )
    } yield header.deepMerge(content) }

  def splitString(str: String, startLine: String = "---", endLine: String = "---"): (String, String) =
    if (str.startsWith(s"$startLine\n")) {
      val lines   = str.split("\n").toList.drop(1)
      val header  = lines.takeWhile(_ != endLine).mkString("\n")
      val content = lines.dropWhile(_ != endLine).drop(1).mkString("\n")
      (header, content)
    } else ("", str)
}