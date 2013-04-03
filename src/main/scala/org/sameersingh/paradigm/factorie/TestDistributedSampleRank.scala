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

  class Emission extends DotTemplateWithStatistics2[Label, Token] {
    setFactorName("Emission")

    override lazy val weights = new DenseTensor2(LabelDomain.size, TokenDomain.size)

    def unroll1(l: Label) = Factor(l, l.token)

    def unroll2(t: Token) = throw new Error("cannot change Token")
  }

  def main(args: Array[String]) {
    val numVars = 10
    val tokens = (0 until numVars).map(i => new Token("w" + (i), "L" + (i % 2)))
    println(tokens.map(t => (t, t.label)).mkString(", "))
    val labels = tokens.map(_.label)

    val system = ActorSystem("Local")
    val model = new TemplateModel(new Emission)
    val trainingActor = system.actorOf(Props(new DistributedSampleRankTrainer[Label](model)))

    val sampler = new VariableSettingsSampler[Label](new TemplateModel(new Emission), new TemplateModel(new HammingTemplate[Label])) with DistributedSampleRank[Label] {
      def trainer = trainingActor
    }

    labels.foreach(_.setRandomly())
    sampler.processAll(labels, 10)
    trainingActor ! SampleRankMessages.Stop()

    // without copying weights
    labels.foreach(_.setRandomly())
    sampler.temperature = 0.001
    println(sampler.model.familiesOfClass[DotFamily].map(_.weights.mkString(", ")))
    for (iter <- 0 until 1) {
      sampler.processAll(labels)
    }
    println(labels.map(_.categoryValue))
    println(sampler.objective.currentScore(labels))

    // copy weights
    for (f <- sampler.model.familiesOfClass[DotFamily]) {
      val fw = model.familiesOfClass[DotFamily].filter(_.factorName == f.factorName).head.weights
      for (i <- fw.activeDomain) {
        f.weights.update(i, fw(i))
      }
    }

    // with copying weights
    println(sampler.model.familiesOfClass[DotFamily].map(_.weights.mkString(", ")))
    labels.foreach(_.setRandomly())
    sampler.temperature = 0.001
    for (iter <- 0 until 1) {
      sampler.processAll(labels)
    }
    println(labels.map(_.categoryValue))
    println(sampler.objective.currentScore(labels))
  }
}