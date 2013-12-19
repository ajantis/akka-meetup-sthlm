/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.akkademo.processor.service

import akka.actor._
import com.typesafe.akkademo.common.{ RegisterProcessor, RetrieveBets, PlayerBet }
import com.typesafe.akkademo.processor.repository.{ DatabaseFailureException, ReallyUnstableResource, UnstableResource }
import akka.actor.SupervisorStrategy.Resume
import scala.concurrent.duration._
import akka.actor.OneForOneStrategy

class BettingProcessor extends Actor with ActorLogging {
  import BettingProcessor._

  val service = createService
  lazy val worker = createWorker

  implicit val repo: () ⇒ UnstableResource = () ⇒ createRepo

  import context.dispatcher
  val scheduler = context.system.scheduler.schedule(1 second, 1 second, self, SendHeartbeatService)

  /**
   * TASKS :
   * Send remote registration message to service
   * Create worker for dangerous task (using UnstableRepository actor with ReallyUnstableResource)
   * Supervise worker -> handle errors
   * Send confirmation message back to Betting service
   */

  override val supervisorStrategy = OneForOneStrategy() {
    case _: RuntimeException         ⇒ Resume
    case _: DatabaseFailureException ⇒ Resume
  }

  override def postStop() {
    scheduler.cancel()
  }

  def receive = {
    case cmd: PlayerBet       ⇒ worker.forward(cmd)
    case cmd @ RetrieveBets   ⇒ worker.forward(cmd)
    case SendHeartbeatService ⇒ service ! RegisterProcessor
  }

  def createRepo: UnstableResource = new ReallyUnstableResource

  def createWorker(implicit resource: () ⇒ UnstableResource): ActorRef =
    context.actorOf(Props.apply(new ProcessorWorker(resource)), "worker")

  def createService = context.actorFor(context.system.settings.config.getString("betting-service-actor"))
}

object BettingProcessor {
  case object SendHeartbeatService
  case object ProcessorHeartbeat
}
