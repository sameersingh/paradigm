package org.sameersingh.paradigm.core

/**
 * @author sameer
 * @date 4/13/12
 */

abstract class Queue[W <: Work, R <: Result] {

  /**
   * Get the next job.
   * @return optional job, None means queue is empty, and there's nothing more to do
   */
  def getJob: Option[W]

  /**
   * Work was done, with the given result. Can be used to modify the queue
   * @param w work that was assigned.
   * @param r result of the work w
   */
  def done(w: W, r: R): Unit = {}

  /**
   * A round of work assignment and obtaining results done.
   * Only applicable when the master is synchronous, otherwise never called
   */
  def roundDone(): Unit = {}
}
