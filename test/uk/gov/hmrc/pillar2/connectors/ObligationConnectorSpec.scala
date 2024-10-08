package uk.gov.hmrc.pillar2.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.obligation.ObligationStatus.Fulfilled
import uk.gov.hmrc.pillar2.models.obligation.{ObligationInformation, ObligationType}

import java.time.LocalDate

class ObligationConnectorSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  override lazy val app: Application = applicationBuilder()
    .configure(
      "microservice.services.get-obligation.port" -> server.port()
    )
    .build()

  val plrReference: String    = "XMPLR0123456789"
  val dateFrom:     LocalDate = LocalDate.now()
  val dateTo:       LocalDate = LocalDate.now().plusYears(2)
  val obligationResponse: ObligationInformation =
    ObligationInformation(ObligationType.UKTR, Fulfilled, LocalDate.now(), LocalDate.now(), LocalDate.now())

  private val errorCodes: Gen[Int] = Gen.oneOf(Seq(203, 204, 400, 403, 500, 501, 502, 503, 504))
  lazy val connector: ObligationConnector =
    app.injector.instanceOf[ObligationConnector]

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  "Obligation Connector" - {

    "must return object when the response was OK" in {
      server.stubFor(
        get(urlEqualTo(s"/get-obligation/$plrReference/${dateFrom.toString}/${dateTo.toString}"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(obligationResponse).toString())
          )
      )
      val result = connector.getObligations(plrReference, dateFrom, dateTo).futureValue
      result mustEqual obligationResponse
    }

    "must throw exception when unexpected body is received" in {
      server.stubFor(
        get(urlEqualTo(s"/get-obligation/$plrReference/${dateFrom.toString}/${dateTo.toString}"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.stringify(Json.obj()))
          )
      )
      val result = connector.getObligations(plrReference, dateFrom, dateTo)
      result.failed.map { ex =>
        ex shouldBe a[JsResultException]
      }
    }

    "must return future failed for non-200 responses" in {
      val errorResponse = errorCodes.sample.value
      stubGetResponse(
        s"/get-obligation/$plrReference/${dateFrom.toString}/${dateTo.toString}",
        errorResponse
      )

      val result = connector.getObligations(plrReference, dateFrom, dateTo).failed
      result.failed.map { ex =>
        ex shouldBe a[HttpException]
      }
    }

  }
}
