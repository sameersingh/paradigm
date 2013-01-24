package org.sameersingh.paradigm

import org.junit._
import Assert._
import util.Random

/**
 * @author sameer
 * @date 10/20/12
 */
@Test
class ConfigTest {

  val random = new Random(0)

  def config() = {
    WorkerSystemConfig("SYS_NAME", "HOST_NAME", 1000 + random.nextInt(100))
  }

  @Test
  def testWrite(): Unit = {
    val testFile = java.io.File.createTempFile("test", "exps")
    println(testFile.getCanonicalPath)
    println(config)
    println(config.toJson)
    config.toJson(testFile.getCanonicalPath)
    val config2 = WorkerSystemConfig.fromJson(testFile.getCanonicalPath)
    println(config2)
    println(config2.toJson)
  }

  @Test
  def testWrites(): Unit = {
    val testDir = new java.io.File(java.io.File.createTempFile("test", "exps").getCanonicalPath + "dir")
    testDir.mkdir()
    println(testDir.getCanonicalPath)
    println(config)
    println(config.toJson)
    config.toJson(testDir.getCanonicalPath + "/cfg1")
    config.toJson(testDir.getCanonicalPath + "/cfg2")
    config.toJson(testDir.getCanonicalPath + "/cfg3")
    val configs = WorkerSystemConfig.fromDir(testDir.getCanonicalPath)
    println(configs)
  }

}
