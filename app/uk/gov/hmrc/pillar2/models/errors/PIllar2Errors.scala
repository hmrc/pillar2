package uk.gov.hmrc.pillar2.models.errors

sealed trait Pillar2Error {
  val code: String
  val message: String
}

case object MissingHeaderError extends Pillar2Error {
  val message: String = "Missing X-Pillar2-Id header"
  val code:    String = "001"
}

case class InvalidJsonError(decodeError: String) extends Pillar2Error {
  val code: String = "002"
  val message: String = s"Invalid JSON payload: $decodeError"

}
