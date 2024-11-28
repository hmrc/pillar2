package uk.gov.hmrc.pillar2.controllers

import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.pillar2.connectors.UKTaxReturnConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UKTaxReturnController @Inject()(
    cc: ControllerComponents,
    ukTaxReturnConnector: UKTaxReturnConnector
)(implicit ec: ExecutionContext) extends BackendController(cc) {

  def submitUKTaxReturn(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    ukTaxReturnConnector.submitUKTaxReturn(
      request.body,
      request.headers.get("correlationid"),
      request.headers.get("X-Pillar2-Id"),
      request.headers.get("X-Receipt-Date"),
      request.headers.get("X-Originating-System"),
      request.headers.get("X-Transmitting-System")
    ).map {
      case Right(response) => Ok(response)
      case Left(error) => error
    }
  }
} 