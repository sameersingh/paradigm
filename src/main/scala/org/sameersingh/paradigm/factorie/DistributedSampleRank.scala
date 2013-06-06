package org.sameersingh.paradigm.factorie

import cc.factorie._
import akka.actor.{Props, ActorRef, Actor}
import cc.factorie.optimize.GradientOptimizer
import la._
import util.Accumulator
import scala.collection.mutable.{ArrayBuffer, HashMap}
import collection.mutable

/**
 * @author sameer
 */

object SerializableObjects {

  // class to represent a bunch of weights and gradients
  abstract class Tensor() {
    def active: Seq[(Int, Double)] = Seq.empty

    def assignTo(t: cc.factorie.la.Tensor): Unit = {}
  }

  // sparse tensor for gradients
  case class SparseTensor(elements: Seq[(Int, Double)]) extends Tensor() {
    override def active: Seq[(Int, Double)] = elements

    override def assignTo(t: cc.factorie.la.Tensor): Unit = {
      for ((i, d) <- elements) t(i) = d
    }
  }

  // dense tensor for weights
  case class DenseTensor(elements: Array[Double]) extends Tensor() {
    override def active: Seq[(Int, Double)] = (0 until elements.length).zip(elements).toSeq

    override def assignTo(t: cc.factorie.la.Tensor): Unit = {
      t := elements
    }
  }

  // collection of tensors indexed by a "name"
  case class Tensors(tensors: Map[String, Tensor])

  // gradient in SampleRank, with an objective and a gradient
  case class SampleRankGradient(objective: Double, gradient: Tensors, priority: Option[Double])

}

object SampleRankMessages {

  import SerializableObjects._

  // Message telling Actor to use the gradient to update weights
  case class UseGradient(gradient: SampleRankGradient)

  case class UseGradients(gradients: Seq[SampleRankGradient])

  // Message asking Actor to return the weights
  case class RequestWeights()

  // Message sending weights to the sender
  case class UpdatedWeights(weights: Tensors)

  case class Stop()

}

class GradientAccumulator(namedSlots: TensorSetKeyNames) extends WeightsMapAccumulator {

  val gradient: HashMap[String, HashMap[Int, Double]] = new HashMap

  def accumulate(t: Tensor) = throw new Error("Not implemented.")

  def combine(ta: Accumulator[Tensor]) = throw new Error("Not implemented.")

  def accumulator(family: Weights) = throw new Error("Not implemented.")

  def accumulate(family: Weights, index: Int, value: Double) = throw new Error("Not implemented.")

  def accumulate(index: Int, value: Double) = throw new Error("Not implemented.")

  def accumulate(t: Tensor, factor: Double) = throw new Error("Not implemented.")

  def accumulate(a: Weights, t: Tensor, factor: Double) = {
    val map = gradient.getOrElseUpdate(namedSlots.keyToName(a), new HashMap)
    t.foreachActiveElement((i, d) => map(i) = map.getOrElse(i, 0.0) + d * factor)
  }

  def accumulate(family: Weights, t: Tensor): Unit = accumulate(family, t, 1.0)

  def toSerializable: SerializableObjects.Tensors = {
    SerializableObjects.Tensors(gradient.map(p => (p._1, SerializableObjects.SparseTensor(p._2.toSeq))).toMap)
  }
}

// TODO: We could also introduce "NamedTensorSetKey" that just define a new TensorSetKey and associate with it a name for purposes
// of nicer serialization and distributed message stuff - otherwise you just get your order-specific serialization for free -luke

class DotFamilyKeyNames(families: Seq[DotFamily]) extends TensorSetKeyNames {
  private val namesToKeys = families.map(f => (f.factorName, f.weights)).toMap
  private val keysToNames = families.map(f => (f.weights, f.factorName)).toMap

  def nameToKey(name: String) = namesToKeys(name)

  def keyToName(key: Weights) = keysToNames(key)
}

trait TensorSetKeyNames {
  def nameToKey(name: String): Weights

  def keyToName(slot: Weights): String
}

trait DistributedSampleRank[C] extends ProposalSampler[C] {

  import SerializableObjects._
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout: Timeout = Timeout(5 seconds)

  def trainer: Option[ActorRef]

  def keyNames: TensorSetKeyNames

  def modelWithWeights = model.asInstanceOf[Model with Parameters]

  val numSamplesBetweenWeightRequest: Int = 1000
  val batchSize: Int = 100

  var samplesToRequest = numSamplesBetweenWeightRequest
  var batchedGradients = new ArrayBuffer[SampleRankGradient](batchSize * 2)

  def computeGradient(proposals: Seq[Proposal]): Option[SampleRankGradient] = {
    val gradient = new GradientAccumulator(keyNames)
    val (goodObjective, badObjective) = proposals.max2ByDouble(_.objectiveScore)
    val objectiveValue = goodObjective.objectiveScore - badObjective.objectiveScore
    val priority = badObjective.modelScore - goodObjective.modelScore
    if (objectiveValue > 0.0) {
      goodObjective.diff.redo
      model.factorsOfFamilyClass[DotFamily](goodObjective.diff).foreach(
        f => gradient.accumulate(f.family.weights, f.currentStatistics, 1.0))
      goodObjective.diff.undo
      model.factorsOfFamilyClass[DotFamily](goodObjective.diff).foreach(
        f => gradient.accumulate(f.family.weights, f.currentStatistics, -1.0))
      badObjective.diff.redo
      model.factorsOfFamilyClass[DotFamily](badObjective.diff).foreach(
        f => gradient.accumulate(f.family.weights, f.currentStatistics, -1.0))
      badObjective.diff.undo
      model.factorsOfFamilyClass[DotFamily](badObjective.diff).foreach(
        f => gradient.accumulate(f.family.weights, f.currentStatistics, 1.0))
      Some(SampleRankGradient(objectiveValue, gradient.toSerializable, Some(priority)))
    } else None
  }

  def process(ps: Seq[Proposal]): Unit = {
    if (trainer.isDefined) {
      samplesToRequest -= 1
      if (samplesToRequest == 0) {
        updateWeights
        samplesToRequest = numSamplesBetweenWeightRequest
      }
      computeGradient(ps).foreach(g => batchedGradients += g)
      if (batchedGradients.length >= batchSize) {
        trainer.get ! SampleRankMessages.UseGradients(batchedGradients)
        batchedGradients = new ArrayBuffer[SampleRankGradient](batchSize * 2)
      }
    }
  }

  proposalsHooks += process

  def updateWeights {
    if (trainer.isDefined) {
      import scala.concurrent.ExecutionContext.Implicits.global
      print("Requesting updated weights... ")
      def doSomething(w: SampleRankMessages.UpdatedWeights) = {
        println("w: " + w)
        for (f <- modelWithWeights.parameters.keys) {
          println(w.weights)
          w.weights.tensors(keyNames.keyToName(f)).assignTo(f.value)
        }
      }
      val future = (trainer.get ? SampleRankMessages.RequestWeights()) //.mapTo[SampleRankMessages.UpdatedWeights]
      future.foreach(a => doSomething(a.asInstanceOf[SampleRankMessages.UpdatedWeights]))
      println("Done")
    }
  }
}

class DistributedSampleRankUpdater[C](val model: Model with Parameters, keyNames: TensorSetKeyNames, optimizer: GradientOptimizer) extends Actor {
  val modelWeights = model.parameters

  var learningMargin = 1.0

  def updateWeights(grad: SerializableObjects.SampleRankGradient): Unit = {
    val modelScore = model.parameters.keys.foldLeft(0.0)(
      (s, f) => s + grad.gradient.tensors(keyNames.keyToName(f)).active.foldLeft(0.0)(
        (s, id) => s + f.value(id._1) * id._2))
    if (modelScore <= 0.0) {
      val gradientAccumulator = new LocalWeightsMapAccumulator(modelWeights.blankSparseMap)
      for (f <- model.parameters.keys)
        for ((i, d) <- grad.gradient.tensors(keyNames.keyToName(f)).active) gradientAccumulator.accumulate(f, i, d)
      // TODO incorporate margin?
      optimizer.step(modelWeights, gradientAccumulator.tensorSet, -1.0)
    }
  }

  def receive = {
    case m: SampleRankMessages.UseGradient => updateWeights(m.gradient)
    case m: SampleRankMessages.UseGradients => m.gradients.foreach(g => updateWeights(g))
    case m: SampleRankMessages.Stop => context.system.shutdown()
  }
}

class DistributedSampleRankTrainer[C](val model: Model with Parameters, keyNames: TensorSetKeyNames, optimizer: GradientOptimizer) extends Actor {

  import SerializableObjects._

  val queue = new mutable.PriorityQueue[SampleRankGradient]()(new Ordering[SampleRankGradient] {
    def compare(x: SampleRankGradient, y: SampleRankGradient) = x.priority.get.compare(y.priority.get)
  })

  val updatingActor = context.system.actorOf(Props(new DistributedSampleRankUpdater[C](model, keyNames, optimizer)))

  def processGradient(g: SampleRankGradient) {
    if (g.priority.isDefined) {
      queue += g
      updatingActor ! SampleRankMessages.UseGradient(queue.dequeue())
    } else updatingActor ! SampleRankMessages.UseGradient(g)
  }

  def receive = {
    case m: SampleRankMessages.UseGradient => processGradient(m.gradient)
    case m: SampleRankMessages.UseGradients => m.gradients.foreach(g => processGradient(g))
    case m: SampleRankMessages.RequestWeights => {
      println("Received weights request")
      sender ! SampleRankMessages.UpdatedWeights(Tensors(model.parameters.keys.map(
        f => (keyNames.keyToName(f),
              if (f.value.isDense) DenseTensor(f.value.asArray) // or toArray?
              else SparseTensor(f.value.activeElements.toSeq))).toMap))
    }
    case m: SampleRankMessages.Stop => {
      updatingActor ! SampleRankMessages.Stop
      context.system.shutdown()
    }
  }
}