
import sbt._
import sbt.Keys.isSnapshot
import sbt.librarymanagement.URLRepository

import codeartifact.{CodeArtifact, CodeArtifactPlugin}
import codeartifact.CodeArtifactKeys.{codeArtifactResolvers, codeArtifactUrl}
import codeartifact.InternalCodeArtifactKeys.{codeArtifactRepo, codeArtifactToken}

object Repo {

  object Jfrog {
    private val domain = "tubins.jfrog.io"
    private val jFrogRoot = s"https://$domain"
    object Tubins {
      private val pathPrefix = "tubins"

      lazy val sbtDev: MavenRepository = "sbt-dev" at s"$jFrogRoot/$pathPrefix/sbt-dev"

      lazy val sbtRelease: MavenRepository = "sbt-release" at s"$jFrogRoot/$pathPrefix/sbt-release"

      lazy val jvmSnapshot: MavenRepository = "jvm-snapshot" at s"$jFrogRoot/$pathPrefix/jvm-snapshots"

      lazy val jvm: MavenRepository = "jvm-release" at s"$jFrogRoot/$pathPrefix/jvm"
    }

    object Artifactory {
      private val pathPrefix = "artifactory"

      lazy val jvmSnapshots: MavenRepository = "tubi-jvm-snapshot" at s"$jFrogRoot/$pathPrefix/jvm-snapshots"

      lazy val jvm: MavenRepository = "tubi-jvm-release" at s"$jFrogRoot/$pathPrefix/jvm"
    }

    object Credential {
      lazy val fromEnv: Either[String, Credentials] = {
        sys.env
          .get("ARTIFACTORY_USERNAME")
          .zip(sys.env.get("ARTIFACTORY_PASSWORD"))
          .map {
            case (username, password) =>
              Credentials("Artifactory Realm", domain, username, password)
          }
          .headOption
          .toRight("could not build artifactory credentials from the env")
      }

      lazy val fromFile: Either[String, Credentials] = {
        Credentials.loadCredentials(Path.userHome / ".artifactory" / "credentials") match {
          case Right(credentials: DirectCredentials) =>
            Right(credentials)
          case Left(err: String) =>
            Left(s"Could not build artifactory credentials from home directory: $err")
        }
      }
    }
  }

  object Lightbend {
    lazy val mvn: MavenRepository =
      "lightbend-commercial-mvn" at "https://repo.lightbend.com/pass/3hizCqO4VcTjyyqZzmprpQ1Te4CEAdD7S6GZguIH1MjEh07v/commercial-releases"

    lazy val ivy: URLRepository = Resolver.url(
      "lightbend-commercial-ivy",
      url("https://repo.lightbend.com/pass/3hizCqO4VcTjyyqZzmprpQ1Te4CEAdD7S6GZguIH1MjEh07v/commercial-releases")
    )(Resolver.ivyStylePatterns)
  }

  object CodeArtifact {
    val urlBase = "https://tubi-141644937959.d.codeartifact.us-east-2.amazonaws.com/maven"
    val releaseRepo: MavenRepository = "codeartifact-jvm" at s"$urlBase/maven-jvm"
    val snapshotRepo: MavenRepository = "codeartifact-jvm-dev" at s"$urlBase/maven-jvm-dev"
  }
}

object TubiRepoPlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin && CodeArtifactPlugin
  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    codeArtifactUrl := (if (isSnapshot.value) Repo.CodeArtifact.snapshotRepo.root
                        else Repo.CodeArtifact.releaseRepo.root),
    codeArtifactResolvers := List(Repo.CodeArtifact.snapshotRepo.root, Repo.CodeArtifact.releaseRepo.root),
    codeArtifactToken := sys.env
      .get("CODEARTIFACT_AUTH_TOKEN")
      .orElse(
        Credentials.loadCredentials(Path.userHome / ".codeartifact" / "tubi" / "credentials").toOption.map(_.passwd)
      )
      .getOrElse(CodeArtifact.getAuthToken(codeArtifactRepo.value))
  )
}
