//   Copyright 2014 Commonwealth Bank of Australia
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package au.com.cba.omnia.uniform.core
package version

import _root_.scala.Console.{RED, BOLD, RESET}

import sbt._, Keys._

sealed abstract class GitStatus {
  def show() = this match {
    case NotAGitRepository => "UNTRACKED"
    case Dirty(hash)       => hash + "-SNAPSHOT"
    case Clean(hash)       => hash 
  }
}

case object NotAGitRepository extends GitStatus

case class Clean(hash: String) extends GitStatus

case class Dirty(hash: String) extends GitStatus

/** Logs any STDERR output in bold red and discards STDOUT output */
object OnlyLogStdErr extends ProcessLogger {
  override def buffer[T](f: => T)  = f
  override def error(s: => String) = println(s"${RED}${BOLD}${s}${RESET}")
  override def info(s: => String)  = {}
}

object GitInfo {
  def commish(root: File): GitStatus =
    gitlog(root, "%h")

  def commit(root: File): GitStatus =
    gitlog(root, "%H")

  def gitlog(root: File, format: String): GitStatus = {
    if (isGitRepository(root)) {
      val lastCommit = Process("git log --pretty=format:" + format + " -n  1", Some(root)).lines.head
      if (isGitRepositoryDirty(root)) {
        Dirty(lastCommit)
      } else {
        Clean(lastCommit)
      }
    } else {
      NotAGitRepository
    }
  }

  def isGitRepository(root: File): Boolean =
    Process("git status", Some(root)).!(OnlyLogStdErr) == 0

  def isGitRepositoryDirty(root: File): Boolean =
    Process("git status --porcelain", Some(root)).lines.nonEmpty
}
