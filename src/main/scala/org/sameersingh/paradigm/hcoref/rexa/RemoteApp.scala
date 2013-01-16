package org.sameersingh.paradigm.hcoref.rexa

import org.sameersingh.paradigm.hcoref.CorefMaster
import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import org.sameersingh.paradigm.core.{MasterMessages, Util}
import org.sameersingh.paradigm.WorkerSystemConfig

/**
 * @author sameer
 * @date 5/10/12
 */

class RexaMaster(queue: RexaQueue) extends CorefMaster[Rexa.Entity](queue) {
  def numWorkers = Rexa.numWorkers

  override def killWorkerSystemWhenDone = Rexa.masterShouldKillWorkers

  def props = Props[RexaWorker]

  def workerSystemName = Rexa.workerSystem

  def workerPort = Rexa.workerPort

  def workerSystemConfigs = Rexa.workerHosts.map(host => WorkerSystemConfig(workerSystemName, host, workerPort))
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
