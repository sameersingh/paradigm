package org.sameersingh.paradigm.coref

import cc.factorie.app.nlp.coref.HierEntity
import akka.actor.ActorRef
import org.sameersingh.paradigm.core.{LoadBalancingRemoteWorker, Master}
import org.sameersingh.utils.coref.MentionRecord

/**
 * @author sameer
 * @date 5/10/12
 */

abstract class CorefMaster[R <: MentionRecord](val queue: CorefQueue[R])
      extends Master[EntitySet[R], EntitySet[R]]
      with LoadBalancingRemoteWorker[EntitySet[R], EntitySet[R]]
