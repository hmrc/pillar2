package uk.gov.hmrc.pillar2.controllers.actions

import com.google.inject.Inject
import play.api.Logger
import play.api.mvc.{ActionTransformer, Request, WrappedRequest}

import scala.concurrent.{ExecutionContext, Future}

case class Pillar2Request[A](pillar2Id: String, request: Request[A]) extends WrappedRequest[A](request)

class Pillar2HeaderAction @Inject() (implicit val executionContext: ExecutionContext) extends ActionTransformer[Request, Pillar2Request] {

  private val logger = Logger(this.getClass)

  override protected def transform[A](request: Request[A]): Future[Pillar2Request[A]] =
    request.headers.get("X-Pillar2-Id") match {
      case Some(pillar2Id) =>
        Future.successful(Pillar2Request(pillar2Id, request))
      case None =>
        logger.warn("Missing X-Pillar2-Id header in request")
        Future.failed(new Exception("Missing Pillar 2 Header"))
    }
}
