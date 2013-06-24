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

  class Emission(model: Parameters) extends DotTemplateWithStatistics2[Label, Token] {
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

    implicit val random = cc.factorie.random

    val system = ActorSystem("Local")
    val localModel = new TemplateModel with Parameters {
      this += new Emission(this)
    }
    //localModel += localEmission
    val localModelKeyNames = new DotFamilyKeyNames(localModel.templates.map(_.asInstanceOf[DotFamily]))
    val trainingActor = system.actorOf(Props(new DistributedSampleRankTrainer[Label](localModel, localModelKeyNames, new optimize.AdaGrad(1.0, 0.1))))

    val samplerModel = new TemplateModel with Parameters {
      this += new Emission(this)
    }
    //samplerModel += samplerEmission
    val samplerModelKeyNames = new DotFamilyKeyNames(samplerModel.templates.map(_.asInstanceOf[DotFamily]))

    val sampler = new VariableSettingsSampler[Label](samplerModel, HammingObjective)
          with DistributedSampleRank[Label] {
      def trainer = Some(trainingActor)

      val keyNames = samplerModelKeyNames
    }

    labels.foreach(_.setRandomly(random, null))
    sampler.processAll(labels, 10)
    //sampler.updateWeights
    trainingActor ! SampleRankMessages.Stop()
    while (!trainingActor.isTerminated) {
      Thread.sleep(100)
    }

    // without copying weights
    labels.foreach(_.setRandomly(random, null))
    sampler.temperature = 0.001
    println(sampler.modelWithWeights.parameters.keys.map(_.value.mkString(", ")))
    for (iter <- 0 until 1) {
      sampler.processAll(labels)
    }
    println(labels.map(_.categoryValue))
    println(sampler.objective.currentScore(labels))

    // copy weights
    for (f <- sampler.modelWithWeights.parameters.keys) {
      val fw = localModel.parameters.keys.filter(k => localModelKeyNames.keyToName(k) == samplerModelKeyNames.keyToName(f)).head.value
      for (i <- fw.activeDomain) {
        f.value.update(i, fw(i))
      }
    }

    // with copying weights
    println(sampler.modelWithWeights.parameters.keys.map(_.value.mkString(", ")))
    labels.foreach(_.setRandomly(random, null))
    sampler.temperature = 0.001
    for (iter <- 0 until 1) {
      sampler.processAll(labels)
    }
    println(labels.map(_.categoryValue))
    println(sampler.objective.currentScore(labels))
  }
}