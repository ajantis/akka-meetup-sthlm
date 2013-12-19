package com.typesafe.akkademo.processor.service

import akka.actor.Actor
import com.typesafe.akkademo.common.{ ConfirmationMessage, RetrieveBets, PlayerBet }
import com.typesafe.akkademo.processor.repository.{ UnstableResource, ReallyUnstableResource }

class ProcessorWorker(repo: UnstableResource) extends Actor {

  def receive = {
    case b: PlayerBet ⇒
      repo.save(b.id, b.bet.player, b.bet.game, b.bet.amount)
      sender ! ConfirmationMessage(b.id)

    case RetrieveBets ⇒
      sender ! repo.findAll.toList
  }
}