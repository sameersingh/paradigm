package org.sameersingh.paradigm.coref

import org.sameersingh.paradigm.core.Queue
import cc.factorie.app.nlp.coref.HierEntity
import org.sameersingh.utils.coref.MentionRecord

/**
 * @author sameer
 * @date 5/10/12
 */

abstract class CorefQueue[R <: MentionRecord] extends Queue[EntitySet[R], EntitySet[R]]
