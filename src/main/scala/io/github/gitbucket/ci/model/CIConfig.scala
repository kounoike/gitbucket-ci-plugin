package io.github.gitbucket.ci.model

trait CIConfigComponent { self: gitbucket.core.model.Profile =>
  import profile.api._
  import self._

  lazy val CIConfigs = TableQuery[CIConfigs]

  class CIConfigs(tag: Tag) extends Table[CIConfig](tag, "CI_CONFIG") {
    val userName = column[String]("USER_NAME", O PrimaryKey)
    val repositoryName = column[String]("REPOSITORY_NAME")
    val buildScript = column[String]("BUILD_SCRIPT")
    val notification = column[Boolean]("NOTIFICATION")
    val skipWords = column[String]("SKIP_WORDS")
    val runWords = column[String]("RUN_WORDS")
    val artifactsPattern = column[String]("ARTIFACTS_PATTERN")
    def * = (userName, repositoryName, buildScript, notification, skipWords.?, runWords.?, artifactsPattern.?) <> (CIConfig.tupled, CIConfig.unapply)
  }
}

case class CIConfig(
  userName: String,
  repositoryName: String,
  buildScript: String,
  notification: Boolean,
  skipWords: Option[String],
  runWords: Option[String],
  artifactsPattern: Option[String]
){
  lazy val skipWordsSeq: Seq[String] = skipWords.map(_.split(",").map(_.trim).toSeq).getOrElse(Nil)
  lazy val runWordsSeq: Seq[String] = runWords.map(_.split(",").map(_.trim).toSeq).getOrElse(Nil)
}
