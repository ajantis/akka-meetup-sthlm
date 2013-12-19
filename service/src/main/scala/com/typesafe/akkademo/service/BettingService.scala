/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.akkademo.service

import akka.actor.{ ActorRef, ActorLogging, Actor }
import com.typesafe.akkademo.common._
import scala.concurrent.duration._
import com.typesafe.akkademo.common.PlayerBet
import scala.Some

class BettingService extends Actor with ActorLogging {

  import BettingService._

  /**
   * TASKS:
   * Create unique sequence/transaction number
   * Create PlayerBet and call betting processor (remotely)
   * Retrieve all bets from betting processor (remotely)
   * Handle timed out transactions (scheduler)
   * Handle registration message from betting processor
   * Handle crash of/unavailable betting processor
   * Keep any message locally until there is a processor service available
   */

  val ActiveTime = 2000L
  var processor: Option[(Long, ActorRef)] = None
  var currentSequenceId: Int = 0
  var bets: Map[Int, PlayerBet] = Map()

  import context.dispatcher
  context.system.scheduler.schedule(2 seconds, 2 seconds, self, ProcessUnhandledBets)

  def receive = {
    case bet: Bet ⇒
      val pb = createPlayerBet(bet)
      bets = bets.updated(pb.id, pb)
      getProcessor.foreach(_.forward(pb))

    case cmd @ RetrieveBets ⇒
      getProcessor.foreach(_.forward(cmd))

    case RegisterProcessor       ⇒ updateProcessor(sender)

    case ProcessUnhandledBets    ⇒ processUnhandledBets()

    case ConfirmationMessage(id) ⇒ bets = bets - id

  }

  private def createPlayerBet(b: Bet): PlayerBet = PlayerBet(nextSeqId(), b)

  private def nextSeqId(): Int = {
    currentSequenceId += 1
    currentSequenceId
  }

  private def getProcessor: Option[ActorRef] =
    processor.collect {
      case (lastActive, p) if System.currentTimeMillis() - lastActive < ActiveTime ⇒ p
    }

  private def updateProcessor(p: ActorRef): Unit = {
    log.info("Processor is alive!")
    processor = Some((System.currentTimeMillis(), p))
  }

  private def processUnhandledBets(): Unit = {
    log.info(s"We have ${bets.size} unhandled bets..")
    getProcessor.foreach { p ⇒
      bets.values.foreach { p ! _ }
    }
  }
}

object BettingService {
  case object ProcessUnhandledBets
}
