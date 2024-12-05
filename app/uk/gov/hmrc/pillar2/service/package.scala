package uk.gov.hmrc.pillar2

import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Results._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pillar2.models.hip.ErrorSummary.result_500
import uk.gov.hmrc.pillar2.models.hip.{ApiFailureResponse, ApiSuccessResponse, ErrorSummary}
import uk.gov.hmrc.pillar2.models.errors._

import scala.concurrent.Future

package object service {

  private[service] def convertToApiResult(response: HttpResponse): Future[ApiSuccessResponse] =
    response.status match {
      case 201 =>
        response.json.validate[ApiSuccessResponse] match {
          case JsSuccess(success, _) => Future.successful(success)
          case JsError(error)        => Future.failed(InvalidJsonError(error.toString))
        }
      case 422 =>
        response.json.validate[ApiFailureResponse] match {
          case JsSuccess(apiFailure, _) => UnprocessableEntity(Json.toJson(ErrorSummary(apiFailure.errors.code, apiFailure.errors.text)))
          case JsError(_)               => result_500
        }
      case _ =>
        result_500 // TODO: This loses a lot of infomation on what the error actually is and we rely on the implicit logs provided by play logging, maybe set this up to decode the message
    }

}
