package org.sameersingh.paradigm.coref

import cc.factorie._
import org.sameersingh.paradigm.core._
import collection.mutable.ArrayBuffer
import org.sameersingh.utils.coref.{Entity, MentionRecord}
import org.sameersingh.utils.coref.Proposers.CorefProposer

/**
 * @author sameer
 * @date 5/10/12
 */

abstract class EntitySet[R <: MentionRecord] extends Seq[Entity[R]] with Work with Result {
  def entities: Seq[Entity[R]]

  def length = entities.length

  def apply(idx: Int) = entities.apply(idx)

  def iterator = entities.iterator
}

abstract class CorefWorker[R <: MentionRecord] extends Worker[EntitySet[R], EntitySet[R]] {

  type SamplerType = CorefProposer[R]
  type ModelType = TemplateModel

  def model: ModelType

  def newSampler(model: ModelType, entities: EntitySet[R]): SamplerType

  def numSteps: Int

  def result(sampler: SamplerType): EntitySet[R]

  def doWork(w: EntitySet[R]): EntitySet[R] = {
    preWork(w)
    val sampler = newSampler(model, w)
    sampler.setEntities(w)
    sampler.process(numSteps)
    postWork(sampler, w)
    result(sampler)
  }

  def preWork(entities: EntitySet[R]): Unit = {}

  def postWork(sampler: SamplerType, entities: EntitySet[R]): Unit = {}
}
