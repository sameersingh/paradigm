package org.sameersingh.paradigm.examples

import akka.actor._
import org.sameersingh.paradigm.core._
import com.typesafe.config.ConfigFactory
import org.sameersingh.paradigm.WorkerSystemConfig

/**
 * Simple example of how to run the distributed actors code
 * @author sameer
 * @date 4/13/12
 */

/**
 * Run this on a machine called "blake.cs.umass.edu", change the string to whatever the network accessible name is.
 * This object creates an ActorSystem, and waits for requests for actors from the master
 */
object RemoteClientApp {
  def main(args: Array[String]) {
    println("Starting client")
    val system = ActorSystem("TestWorkerSystem", ConfigFactory.load(Util.remoteConfig("blake.cs.umass.edu", 2554, "DEBUG")))
  }
}

/**
 * Run this on the same machine as the master.
 * This object also creates an ActorSystem, and waits for requests for actors from the master
 */
object LocalClientApp {
  def main(args: Array[String]) {
    println("Starting client")
    val cfg = Util.remoteConfig("127.0.0.1", 2554, "DEBUG")
    println(cfg.toString)
    val system = ActorSystem("TestWorkerSystem", ConfigFactory.load(cfg))
  }
}

object TestMasters {

  /**
   * The integer that the workers take as input, and also return after performing some computations
   * @param i The integer value of the job
   */
  case class Job(val i: Int) extends Work with Result

  /**
   * A simple worker that takes an integer (Job) as an input, and returns its square as an integer (Job)
   */
  class TestWorker extends Worker[Job, Job] {
    def doWork(w: Job) = Job(w.i * w.i)
  }

  /**
   * A Queue object that creates 10 jobs, 0 until 10, and iterates through them one by one.
   */
  class TestQueue extends Queue[Job, Job] {
    val numbers = (0 until 10).iterator

    def getJob = if (numbers.hasNext) Some(Job(numbers.next)) else None
  }

  /**
   * Master that works on 5 workers, and creates workers from two actor systems (as described above).
   * Run this master on the same machine as the LocalClientApp, and it will create and process the queue of integers
   */
  class TestMaster extends Master[Job, Job] with LoadBalancingRemoteWorker[Job, Job] {

    val numWorkers = 1

    override def killWorkerSystemWhenDone = true

    val queue = new TestQueue

    def props = Props[TestWorker]

    def workerSystemName = "TestWorkerSystem"

    def workerPort = 2554

    def workerSystemConfigs = Seq("127.0.0.1").map(host => WorkerSystemConfig(workerSystemName, host, workerPort)) //, "blake.cs.umass.edu"
  }

  class TestSyncMaster extends TestMaster with SynchronousMaster[Job, Job]

  def main(args: Array[String]) {
    val synchronous = false
    println("Start Master")
    val system = ActorSystem("TestMasterSystem", ConfigFactory.load(Util.remoteConfig("127.0.0.1", 2552, "DEBUG")))
    println("Creating master")
    val master = if (synchronous) system.actorOf(Props[TestSyncMaster], "master") else system.actorOf(Props[TestMaster], "master")
    println("Sending start to master")
    master ! MasterMessages.Start()
  }
}
