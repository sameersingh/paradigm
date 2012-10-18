package cc.refectorie.user.sameer.distrib.core

import akka.actor._
import akka.remote.RemoteScope
import util.Random

/**
 * @author sameer
 * @date 4/13/12
 */

object MasterMessages {

  case class Start()

  case class Done()

  case class WorkDone[W <: Work, R <: Result](work: W, result: R)

}

trait CustomMasterMessage

abstract class Master[W <: Work, R <: Result] extends Actor with ActorLogging {

  def numWorkers: Int

  var workersActive = 0

  private var wid: Long = 0l

  protected def nextWid: Long = {
    wid += 1
    wid - 1
  }

  def queue: Queue[W]

  def createWorker(w: W, terminatedWorker: ActorRef): ActorRef

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
      val workSent = sendNextWork(s)
      if (workersActive == 0 && !workSent) self ! MasterMessages.Done()
    case c: CustomMasterMessage => customHandler(c, sender)
  }

  /**
   * Start the master, and send jobs to workers
   */
  def start = {
    var i = 0
    while (i < numWorkers && sendNextWork()) {
      i += 1
      //log.debug("Started worker " + i)
    }
  }

  /**
   * No more work to be sent to the workers
   */
  def done = {
    context.system.shutdown()
    //context.stop(self)
  }

  def workDone(w: W, r: R) = {
    queue.done(w)
  }

  def sendNextWork(terminatedWorker: ActorRef = null): Boolean = {
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
  def createWorker(w: W, terminatedWorker: ActorRef) = context.actorOf(props, "local%03d".format(nextWid))
}

/**
 * By default uses the round robin scheduler
 */
trait RemoteWorker[W <: Work, R <: Result] extends Master[W, R] {
  def workerSystemName: String

  def workerHostnames: Seq[String]

  def workerPort: Int

  var i: Int = 0

  def pickHost: String = {
    i += 1
    if (i == workerHostnames.length) i = 0
    workerHostnames(i)
  }

  def createWorker(w: W, terminatedWorker: ActorRef) =
    context.actorOf(
      props.withDeploy(Util.remoteDeploy(workerSystemName, pickHost, workerPort)),
      "worker%04d".format(nextWid))
}

trait RandomRemoteWorker[W <: Work, R <: Result] extends RemoteWorker[W, R] {
  val random: Random = new Random

  override def pickHost: String = workerHostnames(random.nextInt(workerHostnames.length))
}

trait LoadBalancingRemoteWorker[W <: Work, R <: Result] extends RemoteWorker[W, R] {
  override def createWorker(w: W, terminatedWorker: ActorRef) = {
    val host: String = if (terminatedWorker == null) pickHost else terminatedWorker.path.address.host.get
      context.actorOf(
        props.withDeploy(Util.remoteDeploy(workerSystemName, host, workerPort)),
        "worker%04d".format(nextWid))
  }
}
