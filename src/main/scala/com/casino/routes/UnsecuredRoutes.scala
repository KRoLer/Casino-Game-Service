package com.casino.routes

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import com.casino.actors.Croupier.{BetPlaced, BetsList, PlayerNotFound}
import com.casino.actors.Dealer.{Bet, GeneralError, GetBets}

import scala.util.{Failure, Success}

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val betFormat = jsonFormat3(Bet.apply)
  implicit val betPlaced = jsonFormat1(BetPlaced.apply)
  implicit val generalErrorFormat = jsonFormat1(GeneralError.apply)
}

class UnsecuredRoutes(dealer: ActorRef) extends Protocols {

  implicit val timeout = Timeout(5 seconds)

  def routes: Route = logRequestResult("casino-game-webservice") {
    pathPrefix("api" / "v1") {
      path("bets" / LongNumber) { playerId =>
        get {
          onComplete(dealer ? GetBets(playerId)) {
            case Success(BetsList(list)) => complete(StatusCodes.OK -> list)
            case Success(PlayerNotFound(_, msg)) => complete(StatusCodes.BadRequest -> GeneralError(msg))

            case Success(_) => complete(StatusCodes.BadRequest -> GeneralError())
            case Failure(msg) => complete(StatusCodes.InternalServerError -> GeneralError(msg.getMessage))
          }
        }
      } ~
      path("bet") {
        (post & entity(as[Bet])) {bet =>
          onComplete(dealer ? bet) {
            case Success(bet: BetPlaced) => complete(StatusCodes.OK -> bet)
            case Success(error: GeneralError) => complete(StatusCodes.BadRequest -> error)

            case Success(_) => complete(StatusCodes.BadRequest -> GeneralError())
            case Failure(msg) => complete(StatusCodes.InternalServerError -> GeneralError(msg.getMessage))
          }
        }
      }
    }
  }
}
