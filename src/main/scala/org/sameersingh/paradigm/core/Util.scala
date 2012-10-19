package org.sameersingh.paradigm.core

import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.Deploy._
import akka.remote.RemoteScope._
import akka.actor.AddressFromURIString._
import akka.remote.RemoteScope
import akka.actor.{Deploy, AddressFromURIString}
import akka.event.Logging
import org.sameersingh.paradigm.WorkerSystemConfig

/**
 * @author sameer
 * @date 4/13/12
 */

object Util {

  def remoteConfig(hostname: String, port: Int, logLevel: String = "INFO"): Config =
    ConfigFactory.parseString( """
  akka {
    loglevel = %s

    actor {
      provider = "akka.remote.RemoteActorRefProvider"
    }

    remote {
      netty {
        hostname = "%s"
        port = %d
      }
    }
  }
                               """.format(logLevel.toString, hostname, port))

  def deployConfig(hostnames: Seq[String], port: Int, prefix: String, systemName: String): Config = {
    val sb = new StringBuffer()
    sb append ("akka {actor {deployment {\n        ")
    hostnames.zipWithIndex.foreach(p =>
      sb append ("/machine%03d {remote = \"akka://%s@%s:%d\"}".format(p._2, systemName, p._1, port)))
    sb append ("}}}")
    ConfigFactory.parseString(sb.toString)
  }

  def remoteDeploy(cfg: WorkerSystemConfig): Deploy = remoteDeploy(cfg.systemName, cfg.hostname, cfg.port)

  def remoteDeploy(remoteSystem: String, hostname: String, port: Int) = Deploy(scope = RemoteScope(AddressFromURIString("akka://%s@%s:%d".format(remoteSystem, hostname, port))))
}
