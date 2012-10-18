package org.sameersingh.paradigm.core

/**
 * @author sameer
 * @date 4/13/12
 */

abstract class Queue[W <: Work] {

  def getJob: Option[W]

  def done(w:W): Unit = {}
}
