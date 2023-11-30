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

package uk.gov.hmrc.pillar2.controllers

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pillar2.controllers.Auth.AuthAction
import uk.gov.hmrc.pillar2.controllers.auth.FakeAuthAction
import uk.gov.hmrc.pillar2.generators.Generators
import uk.gov.hmrc.pillar2.helpers.BaseSpec
import uk.gov.hmrc.pillar2.models.UserAnswers
import uk.gov.hmrc.pillar2.models.hods.subscription.common.AmendSubscriptionResponse
import uk.gov.hmrc.pillar2.models.subscription.AmendSubscriptionRequestParameters
import uk.gov.hmrc.pillar2.repositories.RegistrationCacheRepository
import uk.gov.hmrc.pillar2.service.SubscriptionService

import scala.concurrent.{ExecutionContext, Future}
class SubscriptionControllerSpec extends BaseSpec with Generators with ScalaCheckPropertyChecks {
  trait Setup {
    val controller =
      new SubscriptionController(
        mockRgistrationCacheRepository,
        mockSubscriptionService,
        mockAuthAction,
        stubControllerComponents()
      )
  }

  val jsData = Json.parse("""{"value": "field"}""")
  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled" -> "false", "auditing.enabled" -> false)
    )
    .overrides(
      bind[RegistrationCacheRepository].toInstance(mockRgistrationCacheRepository),
      bind[SubscriptionService].toInstance(mockSubscriptionService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  val service =
    new SubscriptionService(
      mockRgistrationCacheRepository,
      mockSubscriptionConnector,
      mockCountryOptions
    )

  override def afterEach(): Unit = {
    reset(mockSubscriptionConnector, mockRgistrationCacheRepository, mockAuthConnector, mockSubscriptionService)
    super.afterEach()
  }

  "SubscriptionController" - {
    /*
    "createSubscription" - {
      "should return BAD_REQUEST when subscriptionRequestParameter is invalid" in new Setup {

        val request =
          FakeRequest(
            POST,
            routes.SubscriptionController.createSubscription.url
          )
            .withJsonBody(Json.parse("""{"value": "field"}"""))

        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }

      "should return BAD_REQUEST when come from Bad request come from EIS" in {

        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(400, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual BAD_REQUEST
        }
      }

      "should return FORBIDDEN when authorisation is invalid" in {
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(403, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual FORBIDDEN
        }
      }

      "should return SERVICE_UNAVAILABLE when EIS is down" in {
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(503, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual SERVICE_UNAVAILABLE
        }
      }

      "should return INTERNAL_SERVER_ERROR when EIS fails" in {
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(BAD_GATEWAY, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual INTERNAL_SERVER_ERROR
        }
      }

      "should return CONFLICT when occurs from EIS" in {
        val errorDetails = ErrorDetails(
          ErrorDetail(
            DateTime.now().toString,
            Some("xx"),
            "409",
            "CONFLICT",
            "",
            Some(SourceFaultDetail(Seq("a", "b")))
          )
        )
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(409, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual CONFLICT
        }
      }

      "should return NOT_FOUND from EIS" in {
        when(mockRgistrationCacheRepository.get(any())(any())).thenReturn(Future.successful(Some(jsData)))
        when(
          mockSubscriptionService
            .sendCreateSubscription(
              any[String](),
              any[Option[String]](),
              any[UserAnswers]()
            )(
              any[HeaderCarrier]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(404, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[SubscriptionRequestParameters]) { subscriptionRequestParameters =>
          val request =
            FakeRequest(
              POST,
              routes.SubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionRequestParameters))

          val result = route(application, request).value
          status(result) mustEqual NOT_FOUND
        }
      }

    }

    "readSubscription" - {

      "return OK when valid data is provided" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen, arbitrary[SubscriptionResponse]) {
          (id: String, plrReference: String, mockSubscriptionResponse: SubscriptionResponse) =>
            stubResponse(
              s"/pillar2/subscription/$plrReference",
              OK
            )

            when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Json.toJson(mockSubscriptionResponse)))

            when(mockRgistrationCacheRepository.upsert(any[String], any[JsValue])(any[ExecutionContext]))
              .thenReturn(Future.successful(()))

            val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
            val result  = route(application, request).value

            status(result) mustBe OK
        }
      }

      "return NotFound HttpResponse when subscription information is not found" in {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val logger = Logger(this.getClass)

          when(mockSubscriptionConnector.getSubscriptionInformation(plrReference))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

          logger.debug(s"Mock set for getSubscriptionInformation with plrReference: $plrReference")

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj("error" -> "Error response from service with status: 404 and body: ")
          }
        }
      }
      "Return UnprocessableEntity HttpResponse when subscription is unprocessable" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val expectedHttpResponse = HttpResponse(status = UNPROCESSABLE_ENTITY, body = Json.obj("error" -> "Unprocessable entity").toString())

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj(
              "error" -> s"Error response from service with status: $UNPROCESSABLE_ENTITY and body: ${expectedHttpResponse.body}"
            )
          }
        }
      }

      "Return InternalServerError HttpResponse for internal server error" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val expectedHttpResponse = HttpResponse(status = INTERNAL_SERVER_ERROR, body = Json.obj("error" -> "Internal server error").toString())

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj(
              "error" -> s"Error response from service with status: $INTERNAL_SERVER_ERROR and body: ${expectedHttpResponse.body}"
            )
          }
        }
      }

      "Return ServiceUnavailable HttpResponse when service is unavailable" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val expectedHttpResponse = HttpResponse(status = SERVICE_UNAVAILABLE, body = Json.obj("error" -> "Service unavailable").toString())

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj("error" -> s"Error response from service with status: $SERVICE_UNAVAILABLE and body: ${expectedHttpResponse.body}")
          }
        }
      }

      "Return InternalServerError HttpResponse for unexpected response" in new Setup {
        forAll(arbMockId.arbitrary, plrReferenceGen) { (mockId, plrReference) =>
          val unexpectedErrorMessage = "Unexpected error occurred"
          val expectedHttpResponse =
            HttpResponse(status = INTERNAL_SERVER_ERROR, body = Json.obj("error" -> unexpectedErrorMessage).toString())

          when(mockSubscriptionConnector.getSubscriptionInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(expectedHttpResponse))

          val resultFuture = service.retrieveSubscriptionInformation(mockId, plrReference)(hc, ec)

          whenReady(resultFuture) { result =>
            result mustBe Json.obj(
              "error" -> s"Error response from service with status: $INTERNAL_SERVER_ERROR and body: ${expectedHttpResponse.body}"
            )
          }
        }
      }

      "should return InternalServerError when an exception occurs" in new Setup {
        val id           = "testId"
        val plrReference = "testPlrReference"

        when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(new Exception("Test exception")))

        val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
        val result  = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustEqual Json.obj("error" -> "Error retrieving subscription information")
      }

      "return InternalServerError when an exception is thrown synchronously" in new Setup {
        val id           = "testId"
        val plrReference = "testPlrReference"

        // Throw an exception synchronously when the method is called
        when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenAnswer(new Answer[Future[HttpResponse]] {
            override def answer(invocation: InvocationOnMock): Future[HttpResponse] =
              throw new Exception("Synchronous exception")
          })

        val request = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
        val result  = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustEqual Json.obj("error" -> "Exception thrown before Future was created")
      }

      "respond with InternalServerError when an exception is thrown synchronously" in new Setup {
        val id              = "testId"
        val plrReference    = "testPlrReference"
        val validParamsJson = Json.obj("id" -> id, "plrReference" -> plrReference)
        val fakeRequest = FakeRequest(GET, routes.SubscriptionController.readSubscription(id, plrReference).url)
          .withBody(validParamsJson)

        when(mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenThrow(new RuntimeException("Synchronous exception"))

        val result = route(application, fakeRequest).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustEqual Json.obj("error" -> "Exception thrown before Future was created")
      }

      "return an InternalServerError when the service call fails" in new Setup {
        val validParamsJson = Json.obj("id" -> "validId", "plrReference" -> "validPlrReference")

        when(
          mockSubscriptionService.retrieveSubscriptionInformation(any[String], any[String])(any[HeaderCarrier], any[ExecutionContext])
        ).thenReturn(Future.failed(new RuntimeException("Service call failed")))

        val fakeRequest = FakeRequest(GET, routes.SubscriptionController.readSubscription("validId", "validPlrReference").url)
          .withJsonBody(validParamsJson)

        val result = route(application, fakeRequest).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustEqual Json.obj("error" -> "Error retrieving subscription information")
      }

    }
     */

    "amendSubscription" - {
      //      "handle a valid request and successful response" in new Setup {
      //        forAll(arbitraryAmendSubscriptionUserAnswers.arbitrary) { userAnswers =>
      //          // Update the mock to return the specific UserAnswers object
      //          when(mockRgistrationCacheRepository.get(userAnswers.id))
      //            .thenReturn(Future.successful(Some(Json.toJson(userAnswers.data))))
      //
      //          // Ensure the mockSubscriptionService is called with the expected UserAnswers
      //          when(mockSubscriptionService.extractAndProcess(userAnswers))
      //            .thenReturn(Future.successful(HttpResponse(200, "")))
      //
      //          val requestJson = Json.toJson(AmendSubscriptionRequestParameters(userAnswers.id))
      //          val fakeRequest = FakeRequest(POST, routes.SubscriptionController.amendSubscription.url)
      //            .withJsonBody(requestJson)
      //
      //          val result = route(application, fakeRequest).value
      //
      //          status(result) mustBe OK
      //        }
      //      }

      //      "handle a valid request and successful response" in new Setup {
      //        // Generate a realistic userAnswersJson
      //        val userAnswersJson = arbitraryAmendSubscriptionUserAnswers.arbitrary.sample
      //          .getOrElse(fail("Unable to generate UserAnswers"))
      //          .data
      //
      //        val userAnswers = UserAnswers("testId", userAnswersJson, Instant.now)
      //
      //        when(controller.getUserAnswers(any[String])).thenReturn(Future.successful(userAnswers))
      //
      //        // Mock the repository to return the expected UserAnswers JSON wrapped in an Option
      //        when(mockRgistrationCacheRepository.get("testId"))
      //          .thenReturn(Future.successful(Some(userAnswersJson)))
      //
      //        // Mock the service call with the specific UserAnswers
      //        when(mockSubscriptionService.extractAndProcess(userAnswers))
      //          .thenReturn(Future.successful(HttpResponse(200, "")))
      //
      //        val requestJson = Json.toJson(AmendSubscriptionRequestParameters("testId"))
      //        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url).withJsonBody(requestJson)
      //
      //        val result = route(application, fakeRequest).value
      //
      //        status(result) mustBe OK
      //      }

      //      "handle a valid request and successful response" in new Setup {
      //
      //        val generatedUserAnswers = arbitraryAmendSubscriptionUserAnswers.arbitrary.sample
      //          .getOrElse(fail("Failed to generate valid UserAnswers for testing"))
      //        val validUserAnswersData = generatedUserAnswers.data
      //        val testId               = generatedUserAnswers.id
      //
      //        when(
      //          mockSubscriptionConnector.amendSubscriptionInformation(any[AmendSubscriptionResponse])(any[HeaderCarrier], any[ExecutionContext])
      //        ).thenReturn(
      //          Future.successful(HttpResponse(200, "Amendment successful"))
      //        )
      //
      //        when(mockRgistrationCacheRepository.get(eqTo(testId))(any[ExecutionContext]))
      //          .thenReturn(Future.successful(Some(validUserAnswersData)))
      //
      //        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
      //          .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))
      //
      //        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(testId))
      //        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
      //          .withJsonBody(requestJson)
      //
      //        val result = route(application, fakeRequest).value
      //
      //        status(result) mustBe OK
      //      }

//      "handle a valid request and successful response" in new Setup {
//        val generatedUserAnswers = arbitraryAmendSubscriptionUserAnswers.arbitrary.sample
//          .getOrElse(fail("Failed to generate valid UserAnswers for testing"))
////        val validUserAnswersData = generatedUserAnswers.data
////        val testId               = generatedUserAnswers.id
//
//        val testId = "38ee7c0f-7cb4-4f7d-82df-790fdedf6fe4"
//        val validUserAnswersData = Json.parse(
//          """{"subMneOrDomestic":"ukAndOther","upeNameRegistration":"ற귆嫂돢◲⼲䢧凉皁撙ᷔ촁毫䴌〫","subPrimaryContactName":"튍䝎婢狂ᨦ敥࣫䍳숳꾔ᓗ돧ꤛ姩絼찦ﾴ泌⮉ອ幸쾖ꕮ뉼꓍讄༁韜趃㰫습큷띩醩eࡖ㵏⎫讲싋횡묷ⓥ穒졂乶竍ᔍ썛ᮎ瞝䜨뻎퉝澘飰ⓡ뛠䳣ᴆĔᅹ诸쳸ﱓ懕舛㕽餣砝佤空홠徱㐘鶗⯬জ랢銺叴ŞⰋ㚠鑽벉呛ꊎ뾟꺗㆜죨쒃퇆ݸ傧鋰븙⺝၍ꍿ飒꬚벗炕喃뫺쿌Я乹ɸ鍵嵠鎐其束翁옍笚ꖎÊⱨ궢⡆൶旂굋","subPrimaryEmail":"ဧ꓍䍗ࡦ킐板鉌淊蛘䨒剗ㆲ缾Ż礜尵旉ฑ࠭","subSecondaryContactName":"䓳ᓬ䵊䀲৤幞","subSecondaryEmail":"踱廽쏲ꟈ椺벵ᨤ궂㸏ߓ䵹ۓ㯀컠跍","subSecondaryCapturePhone":-2147483648,"FmSafeID":"躿ﱹ9⤸䗗ཕ즛옹闂薑쾔뱶繵ﾄᨔᔇ⎿츋竫ⵇ枱俜䂻틆唈ᴝ䓛幒廕麥색⒍찉렫","subFilingMemberDetails":{"safeId":"躿ﱹ9⤸䗗ཕ즛옹闂薑쾔뱶繵ﾄᨔᔇ⎿츋竫ⵇ枱俜䂻틆唈ᴝ䓛幒廕麥색⒍찉렫","customerIdentification1":"ꃙ鍸购剧⛠喋慧f嫛萐₄礓䒢籏刃ڱ는侒睞ㅖد쎲ଣ鷥☙ἢ焏嬞壒阑벻Ñ徸찗Ꮧ콁憹㵮ㆅ䨔䝒틮低鸮鑟嗡⻢⾻䁵䃣䋛꾽㷵ꀅ︃玓⤙ꯃ跁ፕ䚊퓹췃ꮆ灙騪괄梴௲ᮇ埯ⱶ࿼","customerIdentification2":"麝᫄孎⩡蜝蓴碱䵞쮧駕湮稺Ⲷꭃ둡쌾₂볎巶桳ፏ띦뇚㱪Ꭸ띬梠蛲텯涬ʴ﷐륐歜䡂힐幏뇖了氬윷","organisationName":"⅖⡲䱠㉟᥾燂͠쁴ٝ㨲ܢﺹ렴ﾺ잴鎥挸㸳䉀퀬瘝첫￦倲챉泏豳称뎂枢䒣⩫㏼셬따앑㘘ᕴꐋﾀ噼ꣁꘘᏝ찘ꐁ᪕扉"},"subAccountingPeriod":{"startDate":"1933-11-25","endDate":"1940-12-13","duetDate":"2040-05-29"},"subAccountStatus":{"inactive":false},"subRegistrationDate":"1923-12-14","fmDashboard":{"organisationName":"ற귆嫂돢◲⼲䢧凉皁撙ᷔ촁毫䴌〫","registrationDate":"1923-12-14"},"subPrimaryCapturePhone":-2147483648,"subPrimaryPhonePreference":true,"subSecondaryPhonePreference":true,"subAddSecondaryContact":true,"subExtraSubscription":{"formBundleNumber":"ḷꌉ碎䌵槏苙棙뿯蠽쯢흡팪ꋚ魅삾ڻ褜","crn":"왢⎒镼ᒺᒧ슦鎵벺魽낷韽㿬駃䄿絨᤿爹댱醹轀撡╰ᅋಝ分椟쉽茟ૌ뿔络陷ື⌙㣅嶄ꄸ瘾⤁皶桦ཊ낏췇녟瀜捫己ꔝ蔥鎵","utr":"ጒ헫狄函⬯埱錟槎䲗렍颥떍⑄ῐ閼椝ﶒ"},"subRegisteredAddress":{"addressLine1":"膉힕␽뛄ŋ捠젧顖邪ᇳ啝肹䶳塲紅吣ò矘神楽钤鿸ꪦڎ諞㤍㟠晴賋懿鲭⫫ﱖ뮇︢䉝䰣笐㇭轻衜ͧ铠辭⭱칦娕","addressLine3":"㰅緃˺깙ⵄ좫痸؋㻤龦ħड़⫩蔙㻥勸疣蝢⹴⪹ꘄ쏰㾒뽬ጾ暾썦ᡩꛙ趻鴏ᡚ葉쿛罜划ᒽ䎁붷쏿霭䚔珵熒","addressLine4":"띦䭾햏觥쁖軞慜ⶵ냨ń䖫쳵硬篧姄欔寈쪆㩍ʟ㓤᫐镠솮ȴ菒삾唶ᩧ傔弃⧽䧛⛭왯캝剩蚗ᑵ壑끜熸裝㰱ᴟ봂㾈श䕻ꛞ찘㝈揲䳅캥蠦ѷ狨鈃랦껻쳵踴㻑Ⓗ帴觐퇫褐㢹ↄ","postalCode":"侬᭶檂毂﯅수꽂쐸烪蜪ᾪꙎ칑㛊슶㠜ಢ턮薖捱ᒢ氒䯭砅溃氯廇㍸飲ꮑ臸ఏ鼦駽圗ﺹ퓲Ⴙ덌չ긻惘찌佽뾨슽㝃ᤉ㐫㔒","countryCode":"煘❗瀸젽"},"NominateFilingMember":true}"""
//        )
//
//        //        when(
////          mockSubscriptionConnector.amendSubscriptionInformation(any[AmendSubscriptionResponse])(any[HeaderCarrier], any[ExecutionContext])
////        ).thenReturn(
////          Future.successful(HttpResponse(200, "Amendment successful"))
////        )
//
//        when(mockRgistrationCacheRepository.get(eqTo(testId))(any[ExecutionContext]))
//          .thenReturn(Future.successful(Some(validUserAnswersData)))
//
//        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
//          .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))
//
//        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(testId))
//        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
//          .withJsonBody(requestJson)
//
//        // Act
//        val result = route(application, fakeRequest).value
//
//        // Assert
//        status(result) mustBe OK
//
//        // Verifying that the method was called with the expected parameters
//        verify(mockSubscriptionConnector).amendSubscriptionInformation(any[AmendSubscriptionResponse])(any[HeaderCarrier], any[ExecutionContext])
//
//      }

//      "handle a valid request and successful response" in new Setup {
//        val generatedUserAnswers = arbitraryAmendSubscriptionUserAnswers.arbitrary.sample
//          .getOrElse(fail("Failed to generate valid UserAnswers for testing"))
//        val validUserAnswersData = generatedUserAnswers.data
//        val testId               = generatedUserAnswers.id
//
//        // Mocking the repository to return user answers
//        when(mockRgistrationCacheRepository.get(eqTo(testId))(any[ExecutionContext]))
//          .thenReturn(Future.successful(Some(validUserAnswersData)))
//
//        // Mocking the service to return a specific response format
//        when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
//          .thenReturn(Future.successful(HttpResponse(200, Json.obj("result" -> "Amendment successful").toString())))
//
//        // Creating a request with the generated test ID
//        val requestJson = Json.toJson(AmendSubscriptionRequestParameters(testId))
//        val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
//          .withJsonBody(requestJson)
//
//        // Executing the request
//        val result = route(application, fakeRequest).value
//
//        // Checking the response status
//        status(result) mustBe OK
//      }

      "return OK when valid data is provided" in new Setup {
        forAll(arbitraryAmendSubscriptionUserAnswers.arbitrary) { userAnswers: UserAnswers =>
          stubPutResponse(
            s"/pillar2/subscription",
            OK
          )
          val id = userAnswers.id

          when(mockSubscriptionService.extractAndProcess(any[UserAnswers])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(HttpResponse(200, "Amendment successful")))

          when(mockRgistrationCacheRepository.get(any[String])(any[ExecutionContext]))
            .thenReturn(Future.successful(Some(userAnswers.data)))

          val requestJson = Json.toJson(AmendSubscriptionRequestParameters(id))
          val fakeRequest = FakeRequest(PUT, routes.SubscriptionController.amendSubscription.url)
            .withJsonBody(requestJson)

          val result = route(application, fakeRequest).value

          status(result) mustBe OK
        }
      }

      /*
      "handle an invalid JSON format in the request" in {
        val request = FakeRequest(POST, "/amend-subscription")
          .withJsonBody(Json.obj("invalid" -> "data"))
        val result = controller.amendSubscription(request)

        status(result) mustBe BAD_REQUEST
        // Assert the response content if necessary
      }

"handle exceptions thrown by the SubscriptionService" in {
        when(mockRegistrationCacheRepository.get(any[String]))
          .thenReturn(Future.successful(Some(Json.obj())))
        when(mockSubscriptionService.extractAndProcess(any[UserAnswers]))
          .thenReturn(Future.failed(new RuntimeException("Service error")))

        val request = FakeRequest(POST, "/amend-subscription")
          .withJsonBody(Json.toJson(AmendSubscriptionRequestParameters("testId", JsObject(Seq()))))
        val result = controller.amendSubscription(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        // Additional assertions for the error message
      }


       */

    }

  }
}
