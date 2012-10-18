package cc.refectorie.user.sameer.distrib.akka

/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
/*
 * comments like //#<tag> are there for inclusion into docs, please don’t remove
 */

import akka.actor.{ Props, Actor, ActorSystem }
import com.typesafe.config.ConfigFactory
import cc.refectorie.user.sameer.distrib.core.Util

//#actor
class SimpleCalculatorActor extends Actor {
  def receive = {
    case Add(n1, n2) ⇒
      println("Calculating %d + %d".format(n1, n2))
      sender ! AddResult(n1, n2, n1 + n2)
    case Subtract(n1, n2) ⇒
      println("Calculating %d - %d".format(n1, n2))
      sender ! SubtractResult(n1, n2, n1 - n2)
  }
}
//#actor

class CalculatorApplication {
  //#setup
  val system = ActorSystem("CalculatorApplication", ConfigFactory.load(Util.remoteConfig("127.0.0.1", 2552)))
  val actor = system.actorOf(Props[SimpleCalculatorActor], "simpleCalculator")
  //#setup

  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
}

object CalcApp {
  def main(args: Array[String]) {
    new CalculatorApplication
    println("Started Calculator Application - waiting for messages")
  }
}
