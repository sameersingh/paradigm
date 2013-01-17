package org.sameersingh.paradigm.coref

import org.junit._
import Assert._
import org.sameersingh.utils.coref.BasicRecord
import akka.actor.{Props, ActorSystem, Actor}
import com.typesafe.config.ConfigFactory
import org.sameersingh.paradigm.core.Util

/**
 * @author sameer
 */
@Test
class SerializationTest {

  def createEntity(id: Long = 100l) = {
    val mentions = Seq("sameer", "singh").zipWithIndex.map(si => SerMention(new BasicRecord(si._2) {
      override def toString() = si._1
    }, si._2)).toBuffer
    SerEntity(id, mentions)
  }

  @Test
  def testOneEntity(): Unit = {
    val e = createEntity()
    println("master: " + e)
    val a = ActorSystem("TestWorkerSystem", ConfigFactory.load(Util.remoteConfig("127.0.0.1", 2554, "DEBUG"))).actorOf(Props(new Actor {
      protected def receive = { case e: SerEntity[BasicRecord] => println("slave:  " + e) }
    }))
    a ! e
  }

  @Test
  def testEntitySet(): Unit = {
    val e1 = createEntity(100)
    val e2 = createEntity(101)
    val e3 = createEntity(103)
    val es = EntitySet[BasicRecord](Seq(e1,e2,e3))
    println("master: " + es)
    val a = ActorSystem("TestWorkerSystem", ConfigFactory.load(Util.remoteConfig("127.0.0.1", 2555, "DEBUG"))).actorOf(Props(new Actor {
      protected def receive = { case es: EntitySet[BasicRecord] => println("slave:  " + es) }
    }))
    a ! es
  }
}
