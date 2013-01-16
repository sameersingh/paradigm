package org.sameersingh.paradigm.hcoref

import cc.factorie.app.nlp.coref.HierEntity
import akka.actor.ActorRef
import org.sameersingh.paradigm.core.{LoadBalancingRemoteWorker, Master}

/**
 * @author sameer
 * @date 5/10/12
 */

abstract class CorefMaster[E <: HierEntity](val queue: CorefQueue[E])
      extends Master[EntitySet[E], EntitySet[E]]
      with LoadBalancingRemoteWorker[EntitySet[E], EntitySet[E]]
