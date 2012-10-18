package org.sameersingh.paradigm.coref.rexa

import org.sameersingh.paradigm.coref.{EntitySet, CorefWorker}
import cc.factorie.{TemplateModel, Model}

/**
 * @author sameer
 * @date 5/10/12
 */

class RexaWorker extends CorefWorker[Rexa.Entity] {
  def model = ClientApp.model

  def newSampler(model: ModelType, entities: EntitySet[Rexa.Entity]) = Rexa.newSampler(model)

  def numSteps = Rexa.numWorkerSteps

  def result(sampler: SamplerType) = new EntitySet[Rexa.Entity] {
    def entities = sampler.getEntities
  }
}

object ClientApp {
  // create the model only once
  val model = Rexa.model
}
