package org.sameersingh.paradigm.util

import collection.mutable.{HashMap, HashSet}

import cc.factorie._
import com.redis.RedisClient
import com.redis.serialization.{Parse, Format}
import java.util.LinkedHashSet
import java.util
import scala.collection.mutable

/**
 * @author sameer
 */
abstract class Store {
  final type Id = String

  def randomUnlocked(): Id

  def randomCanopy(e: Id): String

  def randomUnlockedEntities(canopy: String, max: Int): Iterable[Id]

  def lock(e: Id): Unit

  def unlock(e: Id): Unit

  def remove(e: Id): Unit

  def updateCanopies(e: Id, canopies: Iterable[String])

  def updateAndUnlockCanopies(e: Id, canopies: Iterable[String]) = {
    updateCanopies(e, canopies)
    unlock(e)
  }
}

class InMemStore extends Store {
  val unlocked: HashSet[Id] = new HashSet()
  val entityCanopies: mutable.LinkedHashMap[Id, mutable.LinkedHashSet[String]] = new mutable.LinkedHashMap
  val canopyEntities: mutable.LinkedHashMap[String, mutable.LinkedHashSet[Id]] = new mutable.LinkedHashMap

  def randomUnlocked() = if (unlocked.size > 0) unlocked.sampleUniformly else null

  def randomCanopy(e: Id) = entityCanopies(e).sampleUniformly

  def randomUnlockedEntities(canopy: String, max: Int) = canopyEntities(canopy).shuffle.take(max)

  def lock(e: Id) = unlocked -= e

  def unlock(e: Id) = unlocked += e

  def unlockAll = {
    var count = 0
    for (id <- entityCanopies.keysIterator) {
      count += 1
      unlock(id)
      if (count % 1000 == 0) println("Unlocked %d of %d" format(count, entityCanopies.size))
    }
  }

  def remove(e: Id): Unit = {
    // remove from locking
    unlocked -= e
    // remove from reverse maps
    if (entityCanopies.contains(e)) {
      for (canopy <- entityCanopies(e)) {
        canopyEntities(canopy).remove(e)
      }
      // now remove from forward map
      entityCanopies.remove(e)
    } else {
      // remove from all canopyEntities
      for(ce <- canopyEntities.values) ce.remove(e)
    }
  }

  def updateCanopies(e: Id, canopyStrings: Iterable[String]) {
    val set = entityCanopies.getOrElseUpdate(e, new mutable.LinkedHashSet[String])
    for (canopy <- canopyStrings) {
      set += canopy
      canopyEntities.getOrElseUpdate(canopy, new mutable.LinkedHashSet[Id]) += e
    }
  }
}

class RedisStore(val host: String, val port: Int) extends Store {
  val client = new RedisClient(host, port)

  def unlockedKey = "unlocked"

  def canopiesKey(e: Id): String = "canopies:" + e

  def canopyMembersKey(canopy: String) = "unlocked.members:" + canopy

  def randomUnlocked() = client.spop(unlockedKey).get

  // always called on an unlocked entity? TODO: insert assert here
  def randomCanopy(e: Id) = client.srandmember(canopiesKey(e)).get

  def randomUnlockedEntities(canopy: String, max: Int) = client.srandmember(canopyMembersKey(canopy), max).get.map(_.get)

  def lock(e: Id) {
    client.srem(unlockedKey, e)
    // remove from unlocked.members
    for (canopy <- client.get(canopiesKey(e)))
      client.sadd(canopyMembersKey(canopy), e)
  }

  def unlock(e: Id) {
    client.sadd(unlockedKey, e)
    // add to unlocked.members
    for (canopy <- client.get(canopiesKey(e)))
      client.sadd(canopyMembersKey(canopy), e)
  }

  def remove(e: Id) {
    client.srem(unlockedKey, e)
    // remove from unlocked members
    for (canopy <- client.get(canopiesKey(e)))
      client.srem(canopyMembersKey(canopy), e)
    // remove from canopies?
    client.del(canopiesKey(e))
  }

  def updateCanopies(e: Id, canopies: Iterable[String]) {
    client.sadd(unlockedKey, e)
    for (canopy <- canopies) {
      client.sadd(canopiesKey(e), canopy)
      client.sadd(canopyMembersKey(canopy), e)
    }
  }
}