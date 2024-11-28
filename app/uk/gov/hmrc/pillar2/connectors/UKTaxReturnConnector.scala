package uk.gov.hmrc.pillar2.connectors

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.pillar2.config.AppConfig
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UKTaxReturnConnector @Inject()(
    http: HttpClient,
    config: AppConfig
)(implicit ec: ExecutionContext) {

  private def headers(
      correlationId: Option[String],
      pillar2Id: Option[String],
      receiptDate: Option[String],
      originatingSystem: Option[String],
      transmittingSystem: Option[String]
  ): Seq[(String, String)] = Seq(
    "correlationid" -> correlationId,
    "X-Pillar2-Id" -> pillar2Id,
    "X-Receipt-Date" -> receiptDate,
    "X-Originating-System" -> originatingSystem,
    "X-Transmitting-System" -> transmittingSystem
  ).collect { case (key, Some(value)) => (key, value) }

  def submitUKTaxReturn(
      payload: JsValue,
      correlationId: Option[String],
      pillar2Id: Option[String],
      receiptDate: Option[String],
      originatingSystem: Option[String],
      transmittingSystem: Option[String]
  )(implicit hc: HeaderCarrier): Future[Either[Result, JsValue]] = {
    val serviceName = "submit-uk-tax-return"
    val url = s"${config.baseUrl(serviceName)}"
    
    http.POST[JsValue, HttpResponse](
      url,
      payload,
      headers(correlationId, pillar2Id, receiptDate, originatingSystem, transmittingSystem)
    ).map { response =>
      response.status match {
        case 201 => Right(response.json)
        case 400 => Left(BadRequest(response.json))
        case 422 => Left(UnprocessableEntity(response.json))
        case 500 => Left(InternalServerError(response.json))
        case status => Left(Status(status)(response.json))
      }
    }
  }
} 