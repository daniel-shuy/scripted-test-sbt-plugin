import ReleaseTransformations._

releaseIgnoreUntrackedFiles := true

// skip Travis CI build
releaseCommitMessage := s"[ci skip] ${releaseCommitMessage.value}"

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ test"),
  // When running scripted tests targeting multiple SBT versions, we must first publish locally for all SBT versions
  releaseStepCommandAndRemaining("^ publishLocal"),
  releaseStepInputTask(scripted),
  setReleaseVersion,
  commitReleaseVersion,
  // don't tag, leave it to git flow
  // tagRelease,
  releaseStepCommandAndRemaining("^ publish"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
