import scala.sys.process._

import com.github.daniel.shuy.sbt.scripted.scalatest.ScriptedScalaTestSuiteMixin
import org.scalatest.Assertions._
import org.scalatest.wordspec.AnyWordSpec

lazy val testFailure = project
  .in(file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "test/sbt-0.13/testFailure",

    sys.props.get("plugin.version") match {
      case Some(pluginVersion) => scriptedLaunchOpts := { scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + pluginVersion)
      }
      case _ => sys.error("""|The system property 'plugin.version' is not defined.
                             |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
    },
    scriptedBufferLog := false,

    scriptedScalaTestStacks := SbtScriptedScalaTest.FullStacks,
    scriptedScalaTestSpec := Some(new AnyWordSpec with ScriptedScalaTestSuiteMixin {
      override val sbtState: State = state.value

      "scripted" should {
        "fail on ScalaTest failure" in {
          val pidFile = new File("target/pid")
          (s"echo ${ProcessHandle.current.pid}" #> pidFile).!!

          assertThrows[sbt.Incomplete](
            Project.extract(sbtState)
              .runInputTask(scripted, "", sbtState))
        }
      }
    })
  )
