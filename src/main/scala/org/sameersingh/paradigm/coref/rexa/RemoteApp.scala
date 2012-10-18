package cc.refectorie.user.sameer.distrib.coref.rexa

import cc.refectorie.user.sameer.distrib.coref.CorefMaster
import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import cc.refectorie.user.sameer.distrib.core.{MasterMessages, Util}

/**
 * @author sameer
 * @date 5/10/12
 */

class RexaMaster(queue: RexaQueue) extends CorefMaster[Rexa.Entity](queue) {
  def numWorkers = 0

  def props = Props[RexaWorker]

  def workerSystemName = Rexa.workerSystem

  def workerHostnames = Rexa.workerHosts

  def workerPort = Rexa.workerPort
}

object RemoteApp {
  def main(args: Array[String]) {
    // TODO: read config and store in Rexa object

    val queue = new RexaQueue
    println("Start Master")
    val system = ActorSystem(Rexa.masterSystem, ConfigFactory.load(Util.remoteConfig(Rexa.masterHost, Rexa.masterPort, "DEBUG")))
    println("Creating master")
    val master = system.actorOf(Props(new RexaMaster(queue)), "master")
    println("Sending start to master")
    master ! MasterMessages.Start()
  }

}
