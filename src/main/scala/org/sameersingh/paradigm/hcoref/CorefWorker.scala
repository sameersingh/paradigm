package org.sameersingh.paradigm.hcoref

import cc.factorie._
import app.nlp.coref.{HierEntity, HierCorefSampler, Entity}
import org.sameersingh.paradigm.core._
import collection.mutable.ArrayBuffer

/**
 * @author sameer
 * @date 5/10/12
 */

abstract class EntitySet[E <: HierEntity] extends Seq[E] with Work with Result {
  def entities: Seq[E]

  def length = entities.length

  def apply(idx: Int) = entities.apply(idx)

  def iterator = entities.iterator
}

abstract class CorefWorker[E <: HierEntity] extends Worker[EntitySet[E], EntitySet[E]] {

  type SamplerType = HierCorefSampler[E]
  type ModelType = TemplateModel

  def model: ModelType

  def newSampler(model: ModelType, entities: EntitySet[E]): SamplerType

  def numSteps: Int

  def result(sampler: SamplerType): EntitySet[E]

  def doWork(w: EntitySet[E]): EntitySet[E] = {
    preWork(w)
    val sampler = newSampler(model, w)
    sampler.setEntities(w)
    sampler.process(numSteps)
    postWork(sampler, w)
    result(sampler)
  }

  def preWork(entities: EntitySet[E]): Unit = {}

  def postWork(sampler: SamplerType, entities: EntitySet[E]): Unit = {}
}
