package cc.refectorie.user.sameer.distrib.core

import akka.actor._
import cc.refectorie.user.sameer.distrib.core._
import com.typesafe.config.ConfigFactory
import akka.japi.Creator
import akka.event.Logging

/**
 * @author sameer
 * @date 4/13/12
 */

object TestMasters {

  class TestWorker extends Worker[Job, Job] {
    def doWork(w: Job) = Job(w.i * w.i)
  }

  case class Job(val i: Int) extends Work with Result

  class TestQueue extends Queue[Job] {
    val numbers = (0 until 10).iterator

    def getJob = if (numbers.hasNext) Some(Job(numbers.next)) else None
  }

  class TestMaster extends Master[Job, Job] with LoadBalancingRemoteWorker[Job, Job] {

    val numWorkers = 5

    val queue = new TestQueue

    def props = Props[TestWorker]

    def workerSystemName = "TestWorkerSystem"

    def workerHostnames = Seq("127.0.0.1",  "blake.cs.umass.edu")

    def workerPort = 2554
  }

  def main(args: Array[String]) {
    println("Start Master")
    val system = ActorSystem("TestMasterSystem", ConfigFactory.load(Util.remoteConfig("127.0.0.1", 2552, "DEBUG")))
    println("Creating master")
    val master = system.actorOf(Props[TestMaster], "master")
    println("Sending start to master")
    master ! MasterMessages.Start()
  }
}

object RemoteClientApp {
  def main(args: Array[String]) {
    println("Starting client")
    val system = ActorSystem("TestWorkerSystem", ConfigFactory.load(Util.remoteConfig("blake.cs.umass.edu", 2554, "DEBUG")))
  }
}

object LocalClientApp {
  def main(args: Array[String]) {
    println("Starting client")
    val system = ActorSystem("TestWorkerSystem", ConfigFactory.load(Util.remoteConfig("127.0.0.1", 2554, "DEBUG")))
  }
}
