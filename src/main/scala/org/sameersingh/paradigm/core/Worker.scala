package org.sameersingh.paradigm.core

import akka.actor.{ActorLogging, ActorRef, Actor}


/**
 * @author sameer
 * @date 4/13/12
 */

object WorkerMessages {

  case class WorkOnThis[W <: Work](w: W)
  case class KillSystem()
}

trait CustomWorkerMessage

abstract class Worker[W <: Work, R <: Result] extends Actor with ActorLogging {
  protected def receive = {
    case WorkerMessages.WorkOnThis(w: W) => {
      log.debug("Received work: %s".format(w))
      val r = doWork(w)
      log.debug("Sending result: %s -> %s".format(w, r))
      sender ! MasterMessages.WorkDone(w, r)
      log.debug("Dying")
      context.stop(self)
    }
    case WorkerMessages.KillSystem() => {
      log.debug("Killing actor and system.")
      context.system.shutdown()
    }
    case c: CustomWorkerMessage => customHandler(c, sender)
  }

  def doWork(w: W): R

  def customHandler(c: CustomWorkerMessage, sender: ActorRef): Unit = {}
}
