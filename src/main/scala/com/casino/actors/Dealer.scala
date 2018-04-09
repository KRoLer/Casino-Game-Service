package com.casino.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.pattern.{ask, pipe}
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.casino.actors.Croupier.{PlaceBet, ShowBets}
import com.casino.actors.Dealer.{Bet, GeneralError, GetBets}
import com.casino.actors.RemoteWallet.{Withdraw, Withdrawn}

import scala.concurrent.duration._
import scala.util.Success

object Dealer {
  def props(croupier: ActorRef) = Props(classOf[Dealer],croupier)

  case class Bet (gameId: Long, playerId: Long, amount: Double)
  case class GetBets(playerId: Long)

  case class GeneralError(msg: String)
}
class Dealer (croupier: ActorRef) extends Actor with ActorLogging {
  implicit val timeout = Timeout(5 seconds)
  implicit val executionContext = context.dispatcher

  private val remoteWalletRouter = {
    context.actorOf(ClusterRouterPool(
      RoundRobinPool(10),
      ClusterRouterPoolSettings(
        totalInstances = 30,
        maxInstancesPerNode = 10,
        allowLocalRoutees = true
      )
    ).props(RemoteWallet.props), name = "RemoteWallet-router")
  }

  //TODO: Decide how to handle valid games
  private val activeGames: Set[Long] = Set(1,2,3,4)

  override def receive: Receive = {
    case Bet(gameId, playerId, amount) if activeGames.contains(gameId) => {
      val resp  = remoteWalletRouter ? Withdraw(playerId, amount)
      val requester = sender
      resp onComplete {
        case Success(Withdrawn(balance)) => (croupier ? PlaceBet(gameId, playerId, amount, balance)) pipeTo requester
        case _ => requester ! GeneralError("Incorrect game or wallet error")
      }
    }
    case _: Bet => sender ! GeneralError("Incorrect game or wallet error")

    case GetBets(id) => croupier forward ShowBets(id)
  }
}
