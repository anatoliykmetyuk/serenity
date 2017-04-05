package serenity

import java.io.File
import org.apache.commons.io.FileUtils


object Serenity {
  def serenity(where: String): Unit = {
    val from = new File(where)
    val to   = new File(s"_site/$where.html")
    FileUtils.copyFile(from, to)
  }
}