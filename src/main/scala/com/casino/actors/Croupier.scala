package com.casino.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.casino.actors.Croupier._

import scala.collection.mutable
import scala.concurrent.duration._
object Croupier {
  def props = Props[Croupier]

  case class PlaceBet(gameId: Long, playerId: Long, amount: Double, balance: Double)
  case class ShowBets(playerId: Long)

  sealed trait CroupierResponse
  case class BetPlaced (balance: Double) extends CroupierResponse
  case class BetsList (bets: List[Double]) extends CroupierResponse
  case class PlayerNotFound (playerID: Long, msg: String) extends CroupierResponse
}

class Croupier extends Actor with ActorLogging {
  implicit val timeout = Timeout(5 seconds)

  private var state: mutable.HashMap[Long, List[(Long, Double)]] = mutable.HashMap.empty

  override def receive: Receive = {
    case PlaceBet(gameId, playerId, amount, balance) => {
      val bets = state.getOrElse(playerId, List())
      state.update(playerId, (gameId, amount) :: bets)
      sender ! BetPlaced(balance)
    }
    case ShowBets(pid) if state.contains(pid) => sender ! BetsList(state(pid).map(_._2))
    case ShowBets(id) => sender ! PlayerNotFound(id, s"Player with id: $id not found.")
  }
}
