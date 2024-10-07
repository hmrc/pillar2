package uk.gov.hmrc.pillar2.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pillar2.connectors.ObligationConnector
import uk.gov.hmrc.pillar2.controllers.ObligationControllerSpec._
import uk.gov.hmrc.pillar2.controllers.auth.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.obligation.ObligationStatus.Fulfilled
import uk.gov.hmrc.pillar2.models.obligation.{ObligationInformation, ObligationType}

import java.time.LocalDate
import scala.concurrent.Future

class ObligationControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {

  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[ObligationConnector].toInstance(mockObligationConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  "Obligation Controller" - {

    "return 200 with obligation data when connector returns us obligation information" in {
      when(mockObligationConnector.getObligations(any(), any(), any())(any(), any())).thenReturn(Future.successful(obligationResponse))

      val request =
        FakeRequest(GET, routes.ObligationController.getObligation(PlrReference, dateFrom.toString, dateTo.toString).url)

      val result = route(application, request).value
      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(obligationResponse)
    }

    "return 404 when cannot find successful response" in {
      when(mockObligationConnector.getObligations(any(), any(), any())(any(), any())).thenReturn(Future.failed(new Exception("wrong")))

      val request =
        FakeRequest(GET, routes.ObligationController.getObligation(PlrReference, dateFrom.toString, dateTo.toString).url)

      val result = route(application, request).value
      status(result) mustEqual NOT_FOUND
    }
  }

}

object ObligationControllerSpec {
  val PlrReference = "XMPLR0123456789"
  val dateFrom: LocalDate = LocalDate.now()
  val dateTo:   LocalDate = LocalDate.now().plusYears(2)
  val obligationResponse: ObligationInformation =
    ObligationInformation(ObligationType.UKTR, Fulfilled, LocalDate.now(), LocalDate.now(), LocalDate.now())
}
