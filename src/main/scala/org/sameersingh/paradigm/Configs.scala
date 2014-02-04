package org.sameersingh.paradigm

import java.io.File
import org.sameersingh.utils.misc.Json

/**
 * @author sameer
 * @date 10/18/12
 */

case class WorkerSystemConfig(val systemName: String,
                              val hostname: String,
                              val port: Int) {
  def toJson = Json.generate(this)

  def toJson(filename: String) = Json.generate(this, new File(filename))
}

object WorkerSystemConfig {
  def fromJsonString(str:String): WorkerSystemConfig = Json.parse[WorkerSystemConfig](str)
  def fromJson(filename:String): WorkerSystemConfig = Json.parse[WorkerSystemConfig](new File(filename))

  def fromDir(dirname: String): Seq[WorkerSystemConfig] = {
    val dir = new File(dirname)
    assert(dir.isDirectory)
    dir.listFiles().toSeq.map(f => fromJson(f.getCanonicalPath)).toSeq
  }
}

