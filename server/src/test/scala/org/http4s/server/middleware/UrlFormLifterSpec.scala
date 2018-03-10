package org.http4s
package server
package middleware

import cats.effect._
import cats.syntax.applicative._
import org.http4s.dsl.io._

class UrlFormLifterSpec extends Http4sSpec {
  val urlForm = UrlForm("foo" -> "bar")

  val service = UrlFormLifter(HttpService[IO] {
    case r @ POST -> _ =>
      r.uri.multiParams.get("foo") match {
        case Some(ps) =>
          Ok(ps.mkString(","))
        case None =>
          BadRequest("No Foo")
      }
  })

  "UrlFormLifter" should {
    "Add application/x-www-form-urlencoded bodies to the query params" in {
      val req = Request[IO](method = POST).withEntity(urlForm).pure[IO]
      req.flatMap(service.orNotFound.run) must returnStatus(Ok)
    }

    "Add application/x-www-form-urlencoded bodies after query params" in {
      val req =
        Request[IO](method = Method.POST, uri = Uri.uri("/foo?foo=biz"))
          .withEntity(urlForm)
          .pure[IO]
      req.flatMap(service.orNotFound.run) must returnStatus(Ok)
      req.flatMap(service.orNotFound.run) must returnBody("biz,bar")
    }

    "Ignore Requests that don't have application/x-www-form-urlencoded bodies" in {
      val req = Request[IO](method = Method.POST).withEntity("foo").pure[IO]
      req.flatMap(service.orNotFound.run) must returnStatus(BadRequest)
    }
  }
}
