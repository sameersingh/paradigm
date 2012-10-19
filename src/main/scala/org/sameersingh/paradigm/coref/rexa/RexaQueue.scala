package org.sameersingh.paradigm.coref.rexa

import org.sameersingh.paradigm.coref.{EntitySet, CorefQueue}


/**
 * @author sameer
 * @date 5/10/12
 */

class RexaQueue extends CorefQueue[Rexa.Entity] {

  // TODO: get the next "canopy" or a batch of entities to sample over
  def getJob: Option[Rexa.Work] = null

  // TODO: store the result in the mongodb, or just note that the canopy is done
  override def done(w: Rexa.Work, r: Rexa.Result) {

  }
}
