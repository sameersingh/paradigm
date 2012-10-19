package org.sameersingh.paradigm.examples

import akka.actor._
import org.sameersingh.paradigm.core._
import com.typesafe.config.ConfigFactory

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
    val system = ActorSystem("TestWorkerSystem", ConfigFactory.load(Util.remoteConfig("127.0.0.1", 2554, "DEBUG")))
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
  class TestQueue extends Queue[Job] {
    val numbers = (0 until 10).iterator

    def getJob = if (numbers.hasNext) Some(Job(numbers.next)) else None
  }

  /**
   * Master that works on 5 workers, and creates workers from two actor systems (as described above).
   * Run this master on the same machine as the LocalClientApp, and it will create and process the queue of integers
   */
  class TestMaster extends Master[Job, Job] with LoadBalancingRemoteWorker[Job, Job] {

    val numWorkers = 5

    val queue = new TestQueue

    def props = Props[TestWorker]

    def workerSystemName = "TestWorkerSystem"

    def workerHostnames = Seq("127.0.0.1", "blake.cs.umass.edu")

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
