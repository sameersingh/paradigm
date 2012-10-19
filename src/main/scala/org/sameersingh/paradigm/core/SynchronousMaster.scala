package org.sameersingh.paradigm.core

import akka.actor.Terminated

/**
 * Master that performs the work in a synchronous manner, that is, waits for all the workers to finish, before
 * sending new jobs to everyone
 * @author sameer
 * @date 10/18/12
 */
trait SynchronousMaster[W <: Work, R <: Result] extends Master[W, R] {
  // keeps track of whether the current round is the last round or not
  private var lastRound = false

  override protected def receive = {
    case MasterMessages.Start() => log.debug("Start"); start
    case MasterMessages.Done() => log.debug("Done"); done
    case MasterMessages.WorkDone(w: W, r: R) =>
      log.debug("Received WorkDone: (%s, %s)".format(w, r))
      workDone(w, r)
    case Terminated(s) =>
      workersActive -= 1
      log.debug("Terminated, alive: %d".format(workersActive))
      if (workersActive == 0) {
        queue.roundDone()
        if (lastRound) {
          log.debug("Last round over.")
          self ! MasterMessages.Done()
        } else {
          log.debug("Round over, sending next batch of jobs")
          lastRound = !start
        }
      }
    case c: CustomMasterMessage => customHandler(c, sender)
  }
}
