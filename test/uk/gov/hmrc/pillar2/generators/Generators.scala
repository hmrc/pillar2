/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.pillar2.generators
// scalafix:off
import org.scalacheck.{Arbitrary, Gen}
// scalafix:on
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.*
import org.scalacheck.Shrink
import uk.gov.hmrc.pillar2.models.subscription.ReadSubscriptionRequestParameters
import wolfendale.scalacheck.regexp.RegexpGen

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.annotation.nowarn
trait Generators extends ModelGenerators {

  given dontShrink: Shrink[String] = Shrink.shrinkAny

  def genIntersperseString(
    gen:        Gen[String],
    value:      String,
    frequencyV: Int = 1,
    frequencyN: Int = 10
  ): Gen[String] = {

    val genValue: Gen[Option[String]] =
      Gen.frequency(frequencyN -> None, frequencyV -> Gen.const(Some(value)))

    for {
      seq1 <- gen
      seq2 <- Gen.listOfN(seq1.length, genValue)
    } yield seq1.toSeq.zip(seq2).foldRight("") {
      case ((n, Some(v)), m) =>
        m + n + v
      case ((n, _), m) =>
        m + n
    }
  }

  def intsInRangeWithCommas(min: Int, max: Int): Gen[String] = {
    val numberGen = choose[Int](min, max)
    genIntersperseString(numberGen.toString, ",")
  }

  def intsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x > Int.MaxValue)

  def intsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x < Int.MinValue)

  def nonNumerics: Gen[String] =
    alphaStr suchThat (_.size > 0)

  @nowarn
  def decimals: Gen[String] =
    arbitrary[BigDecimal]
      .suchThat(_.abs < Int.MaxValue)
      .suchThat(!_.isValidInt)
      .map(_.formatted("%f"))

  def intsBelowValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ < value)

  def intsAboveValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ > value)

  def intsOutsideRange(min: Int, max: Int): Gen[Int] =
    arbitrary[Int] suchThat (x => x < min || x > max)

  def nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  def nonEmptyString: Gen[String] =
    arbitrary[String] suchThat (_.nonEmpty)

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars  <- listOfN(length, arbitrary[Char])
    } yield chars.mkString

  def stringsLongerThan(minLength: Int): Gen[String] = for {
    maxLength <- (minLength * 2).max(100)
    length    <- Gen.chooseNum(minLength + 1, maxLength)
    chars     <- listOfN(length, arbitrary[Char])
  } yield chars.mkString

  def stringsExceptSpecificValues(excluded: Seq[String]): Gen[String] =
    nonEmptyString suchThat (!excluded.contains(_))

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    if xs.isEmpty then {
      throw new IllegalArgumentException("oneOf called on empty collection")
    } else {
      val vector = xs.toVector
      choose(0, vector.size - 1).flatMap(vector(_))
    }

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map { millis =>
      Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  val apiContactNumberRegex = """[A-Z0-9 )/(\-*#+]{1,25}"""
  def validContactNumber: Gen[String] = RegexpGen.from(apiContactNumberRegex)

  val subscriptionIDRegex = "^[X][A-Z][0-9]{13}"
  def validSubscriptionID: Gen[String] = RegexpGen.from(subscriptionIDRegex)

  val safeIDRegex = "^[0-9A-Za-z]{1,15}"
  def validSafeID: Gen[String] = RegexpGen.from(safeIDRegex)

  val utrRegex = "^[0-9]*$"
  def validUtr: Gen[String] = RegexpGen.from(utrRegex)

  val apiOrgName = "^([a-zA-Z0-9_.]{1,105})\\$"
  def validOrgName: Gen[String] = RegexpGen.from(apiOrgName)

  import org.scalacheck.Gen

  val plrReferenceGen: Gen[String] = for {
    length <- Gen.choose(1, 100)
    chars  <- Gen.listOfN(length, Gen.alphaNumChar)
  } yield chars.mkString

  val arbMockId:       Arbitrary[String] = Arbitrary(Gen.uuid.map(_.toString))
  val arbPlrReference: Arbitrary[String] = Arbitrary(plrReferenceGen)

  val readSubscriptionRequestParametersGen: Gen[ReadSubscriptionRequestParameters] = for {
    id           <- Gen.uuid.map(_.toString)
    plrReference <- plrReferenceGen
  } yield ReadSubscriptionRequestParameters(id, plrReference)

  val arbReadSubscriptionRequestParameters: Arbitrary[ReadSubscriptionRequestParameters] = Arbitrary(readSubscriptionRequestParametersGen)

  def email: Gen[String] =
    for {
      localPart  <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
      subdomain  <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
      mainDomain <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
      tld        <- Gen.oneOf(".com", ".org", ".net", ".uk", ".gov")
    } yield s"$localPart@$subdomain.$mainDomain$tld"

  def responseStatusGen: Gen[Int] =
    for {
      status <- Gen.oneOf(200, 400, 201, 404, 409, 422, 500, 503)
    } yield status
}
