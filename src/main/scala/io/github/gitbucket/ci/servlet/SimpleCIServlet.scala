package io.github.gitbucket.ci.servlet

import org.json4s.DefaultFormats
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.JacksonJsonSupport


import scala.concurrent.ExecutionContext.Implicits.global

class SimpleCIServlet extends ScalatraServlet
  with JacksonJsonSupport with SessionSupport
  with AtmosphereSupport {

  implicit val jsonFormats = DefaultFormats

  atmosphere("/SimpleCI-ws/build/") {
    new AtmosphereClient{
      def receive = {
        case Connected =>
          println(s"WS connected ${uuid}")
        case Disconnected(disconnector, e) =>
          println("WS disconnected")
        case Error(Some(error)) =>
          println(s"WS error ${error}")
        case TextMessage(text) =>
          println("WS TextMessage:" + text)
          broadcast(text)
        case JsonMessage(json) =>
          println(s"WS JsonMessage: ${json}")
          broadcast(json)
        case _ =>
          println("**************UNKNOWN MESSAGE*************")
      }
    }
  }
}
