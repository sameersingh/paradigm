package org.sameersingh.paradigm.factorie

import cc.factorie._
import la.DenseTensor2
import akka.actor.{Props, ActorSystem, Actor}
import com.typesafe.config.ConfigFactory
import org.sameersingh.paradigm.core.Util

/**
 * @author sameer
 */
object TestSyntheticClassification {

  object TokenDomain extends CategoricalDomain[String]

  class Token(word: String, labelStr: String) extends CategoricalVariable(word) {
    val label = new Label(labelStr, this)

    override def domain = TokenDomain
  }

  object LabelDomain extends CategoricalDomain[String]

  class Label(label: String, val token: Token) extends LabeledCategoricalVariable(label) {
    override def domain = LabelDomain
  }

  class Emission(model: WeightsDef) extends DotTemplateWithStatistics2[Label, Token] {
    setFactorName("Emission")

    val weights = model.Weights(new DenseTensor2(LabelDomain.size, TokenDomain.size))

    def unroll1(l: Label) = Factor(l, l.token)
    def unroll2(t: Token) = throw new Error("cannot change Token")
  }

  def main(args: Array[String]) {
    val numVars = 10
    val tokens = (0 until numVars).map(i => new Token("w" + (i), "L" + (i % 3)))
    println(tokens.map(t => (t, t.label)).mkString(", "))
    val labels = tokens.map(_.label)

    val system = ActorSystem("Local")
    val localModel = new TemplateModel
    val localEmission = new Emission(localModel)
    localModel += localEmission
    val localModelKeyNames = new DotFamilyKeyNames(Seq(localEmission))
    val trainingActor = system.actorOf(Props(new DistributedSampleRankTrainer[Label](localModel, localModelKeyNames)))

    val samplerModel = new TemplateModel
    val samplerEmission = new Emission(samplerModel)
    samplerModel += samplerEmission
    val samplerModelKeyNames = new DotFamilyKeyNames(Seq(samplerEmission))

    val sampler = new VariableSettingsSampler[Label](samplerModel, HammingObjective)
      with DistributedSampleRank[Label] {
      def trainer = Some(trainingActor)
      val keyNames = samplerModelKeyNames
    }

    labels.foreach(_.setRandomly())
    sampler.processAll(labels, 10)
    //sampler.updateWeights
    trainingActor ! SampleRankMessages.Stop()
    while(!trainingActor.isTerminated) {
      Thread.sleep(100)
    }

    // without copying weights
    labels.foreach(_.setRandomly())
    sampler.temperature = 0.001
    println(sampler.modelWithWeights.weightsSet.keys.map(_.value.mkString(", ")))
    for (iter <- 0 until 1) {
      sampler.processAll(labels)
    }
    println(labels.map(_.categoryValue))
    println(sampler.objective.currentScore(labels))

    // copy weights
    for (f <- sampler.modelWithWeights.weightsSet.keys) {
      val fw = localModel.weightsSet.keys.filter(k => localModelKeyNames.keyToName(k) == samplerModelKeyNames.keyToName(f)).head.value
      for (i <- fw.activeDomain) {
        f.value.update(i, fw(i))
      }
    }

    // with copying weights
    println(sampler.modelWithWeights.weightsSet.keys.map(_.value.mkString(", ")))
    labels.foreach(_.setRandomly())
    sampler.temperature = 0.001
    for (iter <- 0 until 1) {
      sampler.processAll(labels)
    }
    println(labels.map(_.categoryValue))
    println(sampler.objective.currentScore(labels))
  }
}