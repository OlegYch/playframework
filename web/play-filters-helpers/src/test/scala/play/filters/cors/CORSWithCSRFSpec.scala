/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.cors

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import jakarta.inject.Inject
import play.api.http.ContentTypes
import play.api.http.HttpFilters
import play.api.http.SecretConfiguration
import play.api.http.SessionConfiguration
import play.api.inject.bind
import play.api.libs.crypto.DefaultCSRFTokenSigner
import play.api.libs.crypto.DefaultCookieSigner
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.EssentialFilter
import play.api.mvc.Results
import play.api.routing.sird._
import play.api.routing.Router
import play.api.Application
import play.filters.cors.CORSWithCSRFSpec.CORSWithCSRFRouter
import play.filters.csrf._

object CORSWithCSRFSpec {
  class Filters @Inject() (corsFilter: CORSFilter, csrfFilter: CSRFFilter) extends HttpFilters {
    def filters = Seq(corsFilter, csrfFilter)
  }

  class FiltersWithoutCors @Inject() (csrfFilter: CSRFFilter) extends HttpFilters {
    def filters: Seq[EssentialFilter] = Seq(csrfFilter)
  }

  class CORSWithCSRFRouter @Inject() (action: DefaultActionBuilder) extends Router {
    private val signer = {
      val secretConfiguration = SecretConfiguration("0123456789abcdef", None)
      val clock               = Clock.fixed(Instant.ofEpochMilli(0L), ZoneId.systemDefault)
      val signer              = new DefaultCookieSigner(secretConfiguration)
      new DefaultCSRFTokenSigner(signer, clock)
    }

    private val sessionConfiguration = SessionConfiguration()

    override def routes = {
      case p"/error" =>
        action { req => throw sys.error("error") }
      case _ =>
        val csrfCheck = CSRFCheck(play.filters.csrf.CSRFConfig(), signer, sessionConfiguration)
        csrfCheck(action(Results.Ok), CSRF.DefaultErrorHandler)
    }
    override def withPrefix(prefix: String): Router           = this
    override def documentation: Seq[(String, String, String)] = Seq.empty
  }
}

class CORSWithCSRFSpec extends CORSCommonSpec {
  def withApp[T](
      filters: Class[? <: HttpFilters] = classOf[CORSWithCSRFSpec.Filters],
      conf: Map[String, ? <: Any] = Map()
  )(block: Application => T): T = {
    running(
      _.configure(conf).overrides(
        bind[Router].to[CORSWithCSRFRouter],
        bind[HttpFilters].to(filters)
      )
    )(block)
  }

  def withApplication[T](conf: Map[String, ?] = Map.empty)(block: Application => T) =
    withApp(classOf[CORSWithCSRFSpec.Filters], conf)(block)

  private def corsRequest =
    fakeRequest("POST", "/baz")
      .withHeaders(
        ORIGIN       -> "http://localhost",
        CONTENT_TYPE -> ContentTypes.FORM,
        COOKIE       -> "foo=bar"
      )
      .withBody("foo=1&bar=2")

  "The CORSFilter" should {
    "Mark CORS requests so the CSRF filter will let them through" in withApp() { app =>
      val result = route(app, corsRequest).get

      status(result) must_== OK
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beSome
    }

    "Forbid CSRF requests when CORS filter is not installed" in withApp(classOf[CORSWithCSRFSpec.FiltersWithoutCors]) {
      app =>
        val result = route(app, corsRequest).get

        status(result) must_== FORBIDDEN
        header(ACCESS_CONTROL_ALLOW_ORIGIN, result) must beNone
    }

    commonTests
  }
}
