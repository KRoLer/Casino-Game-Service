package com.casino.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.casino.actors.RemoteWallet._
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol
import spray.json._


import scala.util.Success

object RemoteWallet {
  def props = Props[RemoteWallet]

  case class Withdraw(playerId: Long, amount: Double)
  case class Withdrawn(balance: Double)
  case class WithdrawError(msg: Option[String], balance: Option[Double])
}

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val withdrawFormatter = jsonFormat2(Withdraw)
  implicit val withdrawnFormatter = jsonFormat1(Withdrawn)
  implicit val withdrawErrorFormatter = jsonFormat2(WithdrawError)
}

class RemoteWallet extends Actor with ActorLogging with Protocols {
  private val http = Http(context.system)
  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer()
  private val config = ConfigFactory.load()
  private val URL = config.getString("service.withdraw.url")

  override def receive: Receive = {
    case withdraw: Withdraw => {
      val requester = sender
      val fut = http.singleRequest(
        HttpRequest(
          HttpMethods.POST,
          Uri(URL),
          entity = HttpEntity(ContentTypes.`application/json`, withdraw.toJson.compactPrint)))

      fut onComplete {
        case Success(HttpResponse(StatusCodes.OK, _, entity, _)) =>
          Unmarshal(entity).to[Withdrawn].onComplete {
            case Success(ok) => requester ! ok
            case _ => requester ! WithdrawError(Some("Malformed response."), None)
          }
        case Success(HttpResponse(StatusCodes.BadRequest, _, entity, _)) =>
          Unmarshal(entity).to[WithdrawError].onComplete {
            case Success(ok) => requester ! ok
            case _ => requester ! WithdrawError(Some("Malformed response."), None)
          }
      }
    }
  }

}
