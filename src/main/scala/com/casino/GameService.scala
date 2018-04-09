package com.casino

import akka.actor.{ActorSystem, PoisonPill}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.stream.ActorMaterializer
import com.casino.actors.{Croupier, Dealer}
import com.typesafe.config.ConfigFactory

object GameService extends App {
  implicit val actorSystem = ActorSystem("game-service-cluster")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  val config = ConfigFactory.load()
  val port = args.headOption.getOrElse(config.getInt("akka.remote.netty.tcp.port"))
  val role = config.getStringList("akka.cluster.roles")

  val master = {
    val singletonProps = ClusterSingletonManager.props(
      singletonProps = Croupier.props,
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(actorSystem)
    )
    actorSystem.actorOf(singletonProps, "croupier")
  }

  val proxy = {
    val proxyProps = ClusterSingletonProxy.props(
      singletonManagerPath = "/user/croupier",
      settings = ClusterSingletonProxySettings(actorSystem)
    )
    actorSystem.actorOf(proxyProps, "croupier-proxy")
  }

  val dealer = actorSystem.actorOf(Dealer.props(proxy), s"dealer-$port" )


}
