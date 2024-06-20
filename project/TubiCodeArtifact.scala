import sbt._
import sbt.Keys._

import codeartifact.CodeArtifactKeys.{codeArtifactPublish, codeArtifactUrl}
import codeartifact.CodeArtifactPlugin
import codeartifact.InternalCodeArtifactKeys.{codeArtifactPackage, codeArtifactToken}

object TubiCodeArtifactPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin && CodeArtifactPlugin

  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // Override the `codeArtifactPublish` task to upload artifacts using cURL. The task defined in
    // `CodeArtifactPlugin` uploads artifacts using the `com.lihaoyi:requests` library, in which
    // there may be some hidden bug that fails the upload on github runner.
    codeArtifactPublish := {
      val log = streams.value.log
      val token = codeArtifactToken.value
      val url = codeArtifactUrl.value.stripSuffix("/")
      val pkg = codeArtifactPackage.value
      val basePublishPath = pkg.basePublishPath
      val versionPublishPath = pkg.versionPublishPath

      val files = packagedArtifacts.value.toList
        // Drop Artifact.
        .map { case (_, file) => file }
        // Convert to os.Path.
        .map(file => os.Path(file))
        // Create CodeArtifact file name.
        .map(file => s"$versionPublishPath/${file.last}" -> file)

      val metadataFile = {
        val td = os.temp.dir()
        os.write(td / "maven-metadata.xml", codeArtifactPackage.value.mavenMetadata)
        val file = td / "maven-metadata.xml"
        s"$basePublishPath/${file.last}" -> file
      }

      (files :+ metadataFile).foreach {
        case (fileName, file) =>
          import scala.sys.process._
          log.info(s"Uploading $fileName")
          val cmd = s"curl -sSLD - -u 'aws:***' -H 'Content-Type:application/octet-stream' -T $file $url/$fileName"
          log.info(cmd)
          Process(cmd.replace("aws:***", s"aws:$token")).run(log).exitValue() match {
            case 0         => log.info("Upload succeeded.")
            case exitValue => scala.sys.error(s"Upload failed with exit value: $exitValue")
          }
      }
    }
  )
}