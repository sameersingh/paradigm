package org.sameersingh.paradigm.akka

/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
/*
 * comments like //#<tag> are there for inclusion into docs, please don’t remove
 */

import com.typesafe.config.ConfigFactory
import scala.util.Random
import akka.actor._
import akka.remote.RemoteScope
import org.sameersingh.paradigm.core.Util

class CreationApplication {
  //#setup
  val remoteConfig = Util.remoteConfig("127.0.0.1", 2554)
  //val config = Util.deployConfig(Seq("127.0.0.1"), 2552, "machine", "CalculatorApplication")
  println(remoteConfig)
  val system = ActorSystem("RemoteCreation", ConfigFactory.load(remoteConfig)) //
  //val system = ActorSystem("RemoteCreation", ConfigFactory.load.withFallback(remoteConfig)) //
  val localActor = system.actorOf(Props[CreationActor], "creationActor")
  val remoteActor = system.actorOf(Props[AdvancedCalculatorActor].withDeploy(Util.remoteDeploy("CalculatorApplication", "127.0.0.1", 2552)))
  //val remoteActor = system.actorOf(Props[AdvancedCalculatorActor], "advancedCalculator")

  def doSomething(op: MathOp) = {
    localActor !(remoteActor, op)
  }

  //#setup

  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
}

//#actor
class CreationActor extends Actor {
  def receive = {
    case (actor: ActorRef, op: MathOp) ⇒ actor ! op
    case result: MathResult ⇒ result match {
      case MultiplicationResult(n1, n2, r) ⇒ println("Mul result: %d * %d = %d".format(n1, n2, r))
      case DivisionResult(n1, n2, r) ⇒ println("Div result: %.0f / %d = %.2f".format(n1, n2, r))
    }
  }
}

//#actor

object CreationApp {
  def main(args: Array[String]) {
    val app = new CreationApplication
    println("Started Creation Application")
    while (true) {
      if (Random.nextInt(100) % 2 == 0) app.doSomething(Multiply(Random.nextInt(20), Random.nextInt(20)))
      else app.doSomething(Divide(Random.nextInt(10000), (Random.nextInt(99) + 1)))

      Thread.sleep(200)
    }
  }
}
