package com.casino.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
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

class Croupier extends PersistentActor with ActorLogging {
  implicit val timeout = Timeout(5 seconds)

  override def persistenceId: String = "croupier-persistent-singleton-actor"

  private var state: mutable.HashMap[Long, List[(Long, Double)]] = mutable.HashMap.empty

  def updateState(evt: PlaceBet) = {
    val bets = state.getOrElse(evt.playerId, List())
    state.update(evt.playerId, (evt.gameId, evt.amount) :: bets)
  }

  override def receiveCommand: Receive = {
    case evt @ PlaceBet(gameId, playerId, amount, balance) => {
      persist(evt){ e =>{
        updateState(e)
        sender ! BetPlaced(e.balance)
      }}
    }
    case ShowBets(pid) if state.contains(pid) => sender ! BetsList(state(pid).map(_._2))
    case ShowBets(id) => sender ! PlayerNotFound(id, s"Player with id: $id not found.")
  }
  override def receiveRecover: Receive = {
    case evt: PlaceBet => updateState(evt); log.info(s"Recovered $evt")
    case SnapshotOffer(_, snapshot: mutable.HashMap[Long, List[(Long, Double)]]) => state = snapshot
  }
}
