package org.sameersingh.paradigm.coref

import cc.factorie._
import org.sameersingh.paradigm.core._
import collection.mutable.ArrayBuffer
import org.sameersingh.utils.coref.{Mention, Entity, MentionRecord}
import org.sameersingh.utils.coref.Proposers.CorefProposer

/**
 * @author sameer
 * @date 5/10/12
 */

case class SerMention[R <: MentionRecord](r: R, trueIndex: Int)

case class SerEntity[R <: MentionRecord](id: Long, mentions: Seq[SerMention[R]])

case class EntitySet[R <: MentionRecord](val fixed: Seq[SerEntity[R]]) extends Work with Result {
  def numEntities = fixed.size
  def numMentions = fixed.sumInts(_.mentions.length)
}

object EntitySet {
  def fromEntities[R <: MentionRecord](entities: Seq[Entity[R]]): EntitySet[R] =
    EntitySet(entities.map(e => SerEntity(e.id, e.mentions.map(m => SerMention(m.record, m.trueEntityIndex.value)).toSeq)))

  // entities with the original ids, empty or not
  def origEntities[R <: MentionRecord](es: EntitySet[R]) = {
    es.fixed.map(entity => {
      val e = new Entity[R](entity.id)
      for (m <- entity.mentions) e += new Mention[R](m.r, m.trueIndex, e)
      e
    }).toSeq
  }

  // non-empty entities numbered sequentially, with optional of adding additional entities
  def sequentialEntities[R <: MentionRecord](es: EntitySet[R], numAdditional: Int = 10) = {
    var entityId = 0
    def nextEid = { entityId += 1; entityId - 1}
    es.fixed.filterNot(_.mentions.size == 0).map(entity => {
      val e = new Entity[R](nextEid)
      for (m <- entity.mentions) e += new Mention[R](m.r, m.trueIndex, e)
      e
    }) ++ (0 until numAdditional).map(i => new Entity[R](nextEid)).toSeq
  }
}

abstract class CorefWorker[R <: MentionRecord] extends Worker[EntitySet[R], EntitySet[R]] {

  type SamplerType = CorefProposer[R]
  type ModelType = TemplateModel

  def model: ModelType

  def newSampler(model: ModelType, entities: EntitySet[R]): SamplerType

  def numSteps: Int

  def result(sampler: SamplerType): EntitySet[R] = EntitySet.fromEntities[R](sampler.entities)

  def doWork(w: EntitySet[R]): EntitySet[R] = {
    preWork(w)
    log.debug("Creating sampler")
    val sampler = newSampler(model, w)
    log.debug("Starting sampler")
    try {
      sampler.process(numSteps)
    } catch {
      case e: Error => if (!e.getMessage.equals("No proposal made changes in 10 tries.")) throw new Error("some other err: " + e.getMessage)
      else log.debug("Not changing anymore")
    }
    log.debug("Sampler finished")
    postWork(sampler, w)
    result(sampler)
  }

  def preWork(entities: EntitySet[R]): Unit = {}

  def postWork(sampler: SamplerType, entities: EntitySet[R]): Unit = {}
}
