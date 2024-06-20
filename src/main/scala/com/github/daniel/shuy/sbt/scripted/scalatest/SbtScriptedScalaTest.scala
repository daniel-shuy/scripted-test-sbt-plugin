package com.github.daniel.shuy.sbt.scripted.scalatest

import org.scalatest.{ScriptedScalaTestSuite, Suite}
import sbt._
import sbt.Keys.streams

object SbtScriptedScalaTest extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  sealed abstract class ScriptedTestStacks(
      val shortstacks: Boolean,
      val fullstacks: Boolean
  )
  case object NoStacks extends ScriptedTestStacks(false, false)
  case object ShortStacks extends ScriptedTestStacks(true, false)
  case object FullStacks extends ScriptedTestStacks(true, true)

  object autoImport {
    lazy val scriptedScalaTestDurations: SettingKey[Boolean] = SettingKey(
      "scripted-scalatest-durations",
      "If false, will not display durations of tests."
    )
    lazy val scriptedScalaTestStacks: SettingKey[ScriptedTestStacks] =
      SettingKey(
        "scripted-scalatest-stacks",
        "Length of stack traces to print."
      )
    lazy val scriptedScalaTestStats: SettingKey[Boolean] = SettingKey(
      "scripted-scalatest-stats",
      "If false, will not display various statistics of tests."
    )
    lazy val scriptedScalaTestSpec
        : TaskKey[Option[Suite with ScriptedScalaTestSuiteMixin]] =
      TaskKey("scripted-scalatest-spec", "The ScalaTest Spec.")
    lazy val scriptedScalaTest: TaskKey[Unit] = TaskKey(
      "scripted-scalatest",
      "Executes all ScalaTest tests for SBT plugin."
    )
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    scriptedScalaTestDurations := true,
    scriptedScalaTestStacks := NoStacks,
    scriptedScalaTestStats := true,
    scriptedScalaTestSpec := None,
    scriptedScalaTest := {
      val duration = scriptedScalaTestDurations.value
      val stacks = scriptedScalaTestStacks.value
      val stats = scriptedScalaTestStats.value
      val log = streams.value.log
      // do nothing if not configured
      scriptedScalaTestSpec.value match {
        case Some(suite) =>
          executeScriptedTestsTask(suite, duration, stacks, stats)
        case None =>
          log.warn(s"${scriptedScalaTestSpec.key.label} not configured, no tests will be run...")
      }
    }
  )

  private[this] def executeScriptedTestsTask(
    suite: ScriptedScalaTestSuite,
    showDuration: Boolean,
    stacks: ScriptedTestStacks,
    stats: Boolean,
  ): Unit = {
    val status = suite.executeScripted(
      durations = showDuration,
      shortstacks = stacks.shortstacks,
      fullstacks = stacks.fullstacks,
      stats = stats
    )
    status.waitUntilCompleted()
    if (!status.succeeds()) {
      sys.error("Scripted ScalaTest suite failed!")
    }
  }
}
