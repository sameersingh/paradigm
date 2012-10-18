package cc.refectorie.user.sameer.distrib.coref

import cc.refectorie.user.sameer.distrib.core.Queue
import cc.factorie.app.nlp.coref.HierEntity

/**
 * @author sameer
 * @date 5/10/12
 */

abstract class CorefQueue[E <: HierEntity] extends Queue[EntitySet[E]]
