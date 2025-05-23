/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.cors

import jakarta.inject.Inject
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.EssentialFilter
import play.api.mvc.Results
import play.api.routing.sird._
import play.api.routing.Router
import play.api.routing.SimpleRouterImpl
import play.api.Application
import play.filters.cors.CORSFilterSpec._
import play.mvc.Http.HeaderNames._

object CORSFilterSpec {
  class Filters @Inject() (corsFilter: CORSFilter) extends HttpFilters {
    def filters: Seq[EssentialFilter] = Seq(corsFilter)
  }

  class CorsApplicationRouter @Inject() (action: DefaultActionBuilder)
      extends SimpleRouterImpl({
        case p"/error" =>
          action { req => throw sys.error("error") }
        case p"/vary" =>
          action { req => Results.Ok("Hello").withHeaders(VARY -> ACCEPT_ENCODING) }
        case _ => action(Results.Ok)
      })
}

class CORSFilterSpec extends CORSCommonSpec {
  def withApplication[T](conf: Map[String, ? <: Any] = Map.empty)(block: Application => T): T = {
    running(
      _.configure(conf).overrides(
        bind[Router].to[CorsApplicationRouter],
        bind[HttpFilters].to[Filters]
      )
    )(block)
  }

  "The CORSFilter" should {
    val restrictPaths = Map("play.filters.cors.pathPrefixes" -> Seq("/foo", "/bar"))

    "pass through a cors request that doesn't match the path prefixes" in withApplication(conf = restrictPaths) { app =>
      val result = route(app, fakeRequest("GET", "/baz").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      mustBeNoAccessControlResponseHeaders(result)
    }

    "merge vary header" in withApplication() { app =>
      val result = route(app, fakeRequest("GET", "/vary").withHeaders(ORIGIN -> "http://localhost")).get

      status(result) must_== OK
      header(VARY, result) must beSome(s"$ACCEPT_ENCODING,$ORIGIN")
    }

    commonTests
  }
}
