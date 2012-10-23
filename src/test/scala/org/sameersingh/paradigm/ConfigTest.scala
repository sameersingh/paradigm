package org.sameersingh.paradigm

import org.junit._
import Assert._

/**
 * @author sameer
 * @date 10/20/12
 */
@Test
class ConfigTest {

  def config() = {
    WorkerSystemConfig("SYS_NAME", "HOST_NAME", 1000)
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

}
