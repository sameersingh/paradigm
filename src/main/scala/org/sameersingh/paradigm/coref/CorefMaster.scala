package cc.refectorie.user.sameer.distrib.coref

import cc.factorie.app.nlp.coref.HierEntity
import akka.actor.ActorRef
import cc.refectorie.user.sameer.distrib.core.{LoadBalancingRemoteWorker, Master}

/**
 * @author sameer
 * @date 5/10/12
 */

abstract class CorefMaster[E <: HierEntity](val queue: CorefQueue[E]) extends Master[EntitySet[E], EntitySet[E]] with LoadBalancingRemoteWorker[EntitySet[E], EntitySet[E]]
