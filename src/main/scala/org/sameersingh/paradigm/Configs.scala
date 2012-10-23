package org.sameersingh.paradigm

import com.codahale.jerkson.Json
import java.io.File

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
}

