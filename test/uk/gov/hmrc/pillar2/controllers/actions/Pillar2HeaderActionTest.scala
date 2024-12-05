package uk.gov.hmrc.pillar2.controllers.actions

import org.scalatest.EitherValues
import org.scalatest.funsuite.{AnyFunSuite, AnyFunSuiteLike}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.concurrent.ExecutionContext.Implicits._

class Pillar2HeaderActionTest extends AnyFunSuite with EitherValues{

  val classUnderTest = new Pillar2HeaderAction()

  test("X-Pillar2-ID header exists") {
    val request = FakeRequest().withHeaders("X-Pillar2-ID" -> "XXXXXXXXXXXXXXXXX")

    val result = await(classUnderTest.refine(request))

    result.isRight shouldBe true
    result.value.pillar2Id shouldEqual "XXXXXXXXXXXXXXXXX"
  }

  test("X-Pillar2-ID header does not exist") {
    val request = FakeRequest() // with no headers

    val result = intercept[Exception](await(classUnderTest.refine(request)))
    result.getMessage shouldEqual "Missing Pillar 2 Header"
  }

}
