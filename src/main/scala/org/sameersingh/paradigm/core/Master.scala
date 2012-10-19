package org.sameersingh.paradigm.core

import akka.actor._
import akka.remote.RemoteScope
import util.Random
import collection.mutable.HashMap
import org.sameersingh.paradigm.WorkerSystemConfig

/**
 * @author sameer
 * @date 4/13/12
 */

object MasterMessages {

  // start the master
  case class Start()

  // master is done, kill it
  case class Done()

  // worker has finished some work, and returned the result (message from worker)
  case class WorkDone[W <: Work, R <: Result](work: W, result: R)

}

trait CustomMasterMessage

abstract class Master[W <: Work, R <: Result] extends Actor with ActorLogging {
  // number of workers that have not terminated
  var workersActive = 0

  private var wid: Long = 0l

  protected def nextWid: Long = {
    wid += 1
    wid - 1
  }

  // total number of workers active at any time
  def numWorkers: Int

  def killWorkerSystemWhenDone: Boolean = false

  def queue: Queue[W, R]

  def createWorker(w: W, terminatedWorker: Option[ActorRef]): ActorRef

  def props: Props

  protected def receive = {
    case MasterMessages.Start() => log.debug("Start"); start
    case MasterMessages.Done() => log.debug("Done"); done
    case MasterMessages.WorkDone(w: W, r: R) =>
      log.debug("Received WorkDone: (%s, %s)".format(w, r))
      workDone(w, r)
    case Terminated(s) =>
      workersActive -= 1
      log.debug("Terminated, alive: %d".format(workersActive))
      val workSent = sendNextWork(Some(s))
      if (workersActive == 0 && !workSent) self ! MasterMessages.Done()
    case c: CustomMasterMessage => customHandler(c, sender)
  }

  /**
   * Start the master, and send jobs to workers
   * @return true if all workers were assigned work, false is queue was emptied before all workers were assigned
   */
  def start: Boolean = {
    var i = 0
    while (i < numWorkers && sendNextWork()) {
      i += 1
      //log.debug("Started worker " + i)
    }
    i == numWorkers
  }

  /**
   * No more work to be sent to the workers
   */
  def done = {
    if (killWorkerSystemWhenDone) killWorkerSystems()
    context.system.shutdown()
    //context.stop(self)
  }

  /**
   * Bring down the worker actor systems
   */
  def killWorkerSystems(): Unit

  def workDone(w: W, r: R) = {
    queue.done(w, r)
  }

  def sendNextWork(terminatedWorker: Option[ActorRef] = None): Boolean = {
    val nextWork = queue.getJob
    if (nextWork.isDefined) {
      val w = nextWork.get
      val worker = createWorker(w, terminatedWorker)
      workersActive += 1
      log.debug("Creating worker: %s, alive = %d".format(w, workersActive))
      context.watch(worker)
      worker ! WorkerMessages.WorkOnThis(w)
      true
    } else false
  }

  def customHandler(c: CustomMasterMessage, sender: ActorRef): Unit = {}
}

trait LocalWorker[W <: Work, R <: Result] extends Master[W, R] {

  def killWorkerSystems() = context.actorOf(props, "local%03d".format(nextWid)) ! WorkerMessages.KillSystem()

  def createWorker(w: W, terminatedWorker: Option[ActorRef]) = context.actorOf(props, "local%03d".format(nextWid))
}

/**
 * By default uses the round robin scheduler
 */
trait RemoteWorker[W <: Work, R <: Result] extends Master[W, R] {
  def workerSystemConfigs: Seq[WorkerSystemConfig]

  lazy val workerSystemConfigMap: HashMap[String, WorkerSystemConfig] = {
    val map = new HashMap[String, WorkerSystemConfig]
    for (wconfig <- workerSystemConfigs)
      if (map.put(wconfig.hostname, wconfig) != None) {
        log.error("Multiple worker configs represent the same host: " + wconfig.hostname)
        throw new Error()
      }
    map
  }

  var i: Int = 0

  def pickHost: WorkerSystemConfig = {
    i += 1
    if (i == workerSystemConfigs.length) i = 0
    workerSystemConfigs(i)
  }

  def killWorkerSystems() =
    for (wconfig <- workerSystemConfigs)
      createWorker(wconfig) ! WorkerMessages.KillSystem()

  def createWorker(wconfig: WorkerSystemConfig) = context.actorOf(props.withDeploy(Util.remoteDeploy(wconfig)), "worker%04d".format(nextWid))

  def createWorker(w: W, terminatedWorker: Option[ActorRef]) = createWorker(pickHost)
}

trait RandomRemoteWorker[W <: Work, R <: Result] extends RemoteWorker[W, R] {
  val random: Random = new Random

  override def pickHost: WorkerSystemConfig = workerSystemConfigs(random.nextInt(workerSystemConfigs.length))
}

/**
 * Perform round robin assignment unless a worker has terminated, in which case, assign it to its host
 */
trait LoadBalancingRemoteWorker[W <: Work, R <: Result] extends RemoteWorker[W, R] {
  override def createWorker(w: W, terminatedWorker: Option[ActorRef]) = {
    val wconfig = if (terminatedWorker.isEmpty) pickHost else workerSystemConfigMap(terminatedWorker.get.path.address.host.get)
    createWorker(wconfig)
  }
}
