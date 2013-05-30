package org.sameersingh.paradigm.hcoref.rexa

import org.sameersingh.paradigm.hcoref.EntitySet
import cc.factorie.app.nlp.hcoref.HierCorefSampler
import cc.factorie.{TemplateModel, Model, DiffList}
import cc.factorie.app.bib.AuthorEntity

/**
 * @author sameer
 * @date 5/10/12
 */

object Rexa {

  type Entity = AuthorEntity

  type Work = EntitySet[Entity]

  type Result = EntitySet[Entity]

  type Sampler = HierCorefSampler[Entity]

  val numWorkers = 5

  val workerSystem = "TestWorkerSystem"
  val workerHosts = Seq("avon6.cs.umass.edu", "avon5.cs.umass.edu")
  val workerPort = 2554

  val masterSystem = "TestMasterSystem"
  val masterHost = "blake.cs.umass.edu"
  val masterPort = 2552

  val numWorkerSteps = 100

  val masterShouldKillWorkers = false

  def newSampler(model: TemplateModel) =
  // TODO Use a specific sampler
    new HierCorefSampler[Entity](model) {
      def newEntity = null

      def sampleAttributes(e: Rexa.Entity)(implicit d: DiffList) {}

      protected def initializeAttributesOfNewRoot(e1: Rexa.Entity, e2: Rexa.Entity, parent: Rexa.Entity)(implicit d: DiffList) {}
    }

  def model: TemplateModel = null
}
