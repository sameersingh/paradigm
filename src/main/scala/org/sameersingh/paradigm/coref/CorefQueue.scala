package org.sameersingh.paradigm.coref

import cc.factorie._
import org.sameersingh.paradigm.core.Queue
import org.sameersingh.utils.coref.{Entity, MentionRecord}
import collection.mutable.{HashSet, HashMap}

/**
 * @author sameer
 * @date 5/10/12
 */

abstract class CorefQueue[R <: MentionRecord] extends Queue[EntitySet[R], EntitySet[R]]

/**
 * Basic coreference queue that uses a HashMap as an underlying data structure. The returned results are copied into new
 * entities to ensure that we always obtain unique ids.
 * @param initEntities Initial set of entities to infer over
 * @param maxEntitiesPerJob (Maximum) number of entities sent as a single job
 * @param maxMentionsPerJob (Maximum) number of mentions sent as a single job (upto size of a single entity)
 * @param numJobs Total number of jobs to send
 * @tparam R Type of the MentionRecord that defines the entities and the models
 */
class BasicQueue[R <: MentionRecord](initEntities: Seq[Entity[R]],
                                     val maxEntitiesPerJob: Int,
                                     val maxMentionsPerJob: Int,
                                     val numJobs: Int) extends CorefQueue[R] {

  val entities: HashMap[Long, Entity[R]] = new HashMap
  initEntities.foreach(e => entities(e.id) = e)

  val locked = new HashSet[Long]()
  var _jobsSent = 0

  var _entityIndex: Long = entities.keys.max + 1

  // unique id
  def nextNewEntityIndex = {
    _entityIndex += 1
    _entityIndex - 1
  }

  def getJob: Option[EntitySet[R]] = {
    if (_jobsSent >= numJobs) return None
    //println("___ BEFORE SENDING ___")
    //printMap()
    _jobsSent += 1
    var mentionsPicked = 0
    var entitiesPicked = 0
    val es = entities.keys.shuffle()
          .filterNot(id => locked.contains(id)).map(entities(_)).filterNot(_.size == 0)
          .takeWhile(e => {
      val needToAdd = (mentionsPicked < maxMentionsPerJob && entitiesPicked < maxEntitiesPerJob)
      mentionsPicked += e.size
      entitiesPicked += 1
      needToAdd
    })
    println("Job: mentions: %d, entities: %d" format(es.sumInts(_.size), es.size))
    es.foreach(locked += _.id)
    //println("___ AFTER SENDING ___")
    //printMap()
    Some(EntitySet.fromEntities[R](es))
  }

  override def done(w: EntitySet[R], r: EntitySet[R]) {
    // unlock old entities, and remove them
    //println("___ BEFORE AGGREGATING RESULTS ___")
    //printMap()
    val wes = EntitySet.origEntities(w)
    wes.foreach(locked -= _.id)
    wes.foreach(e => entities.remove(e.id))
    //println("___ AFTER UNLOCKING ___")
    //printMap()
    val res = EntitySet.sequentialEntities(r, 0)
    //println("___ RETURNED: (%d, %s)" format(res.length, res.mkString(", ")))
    for (e <- res) {
      //print("Adding %d ..." format(e.size))
      val ne = new Entity[R](nextNewEntityIndex)
      while(e.size != 0) {
        val m = e.mentions.head
        m.setEntity(ne)(null)
      }
      entities(ne.id) = ne
      //println("Done %d, %d" format(ne.size, e.size))
    }
    println("___ AFTER AGGREGATING ___")
    printMap()
  }

  def printMap(): Unit = {
    println("num entities: " + entities.keys.size)
    println("num mentions: " + entities.values.sumInts(_.size))
    for (key <- entities.keys) {
      val e = entities(key)
      println("--- %d%s (%d) ---" format(key, if (locked(key)) "*" else "", e.size))
      for (m <- e.mentions) {
        println("   " + m.record)
      }
    }
  }
}

