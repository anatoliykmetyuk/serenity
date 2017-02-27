package serenity

import org.scalatest._

class MainSuite extends FlatSpec with Matchers {
  import Main._

  "Solid tags" should "be applied" in {
    process("Scala is {% awesome %}").value shouldBe "Scala is really awesome"
  }

}
