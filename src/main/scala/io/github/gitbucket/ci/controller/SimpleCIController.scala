package io.github.gitbucket.ci.controller

import java.io.ByteArrayOutputStream

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.Directory.getRepositoryDir
import gitbucket.core.util.SyntaxSugars.using
import gitbucket.core.util.{JGitUtil, ReferrerAuthenticator, WritableUsersAuthenticator}
import gitbucket.core.util.Implicits._
import io.github.gitbucket.ci.service.{BuildSetting, SimpleCIService}
import org.eclipse.jgit.api.Git
import org.fusesource.jansi.HtmlAnsiOutputStream
import org.json4s.jackson.Serialization
import org.scalatra.Ok

class SimpleCIController extends ControllerBase
  with SimpleCIService with AccountService with RepositoryService
  with ReferrerAuthenticator with WritableUsersAuthenticator {

  get("/:owner/:repository/build")(referrersOnly { repository =>
    gitbucket.ci.html.buildresults(repository,
      hasDeveloperRole(repository.owner, repository.name, context.loginAccount))
  })

  get("/:owner/:repository/build/:buildNumber")(referrersOnly { repository =>
    val buildNumber = params("buildNumber").toLong
    getRunningJobs(repository.owner, repository.name)
      .find { case (job, _) => job.buildNumber == buildNumber }
      .map  { case (job, _) => (job.buildNumber, "running")
    }.orElse {
      getBuildResults(repository.owner, repository.name)
        .find { result => result.buildNumber == buildNumber }
        .map { result => (result.buildNumber, if(result.success) "success" else "failure") }
    }.map { case (buildNumber, status) =>
      gitbucket.ci.html.buildoutput(repository, buildNumber, status)
    } getOrElse NotFound()
  })

  get("/:owner/:repository/build/output/:buildNumber")(referrersOnly { repository =>
    val buildNumber = params("buildNumber").toLong

    getRunningJobs(repository.owner, repository.name)
      .find { case (job, sb) => job.buildNumber == buildNumber }
      .map  { case (job, sb) =>
        contentType = formats("json")
        JobOutput("running", colorize(sb.toString))
    } orElse {
      getBuildResults(repository.owner, repository.name)
        .find { result => result.buildNumber == buildNumber }
        .map  { result =>
          contentType = formats("json")
          JobOutput(if(result.success) "success" else "failure", colorize(result.output))
        }
    } getOrElse NotFound()
  })

  ajaxPost("/:owner/:repository/build/run")(writableUsersOnly { repository =>
    using(Git.open(getRepositoryDir(repository.owner, repository.name))) { git =>
      JGitUtil.getDefaultBranch(git, repository).map { case (objectId, revision) =>
        runBuild("root", "gitbucket", objectId.name, BuildSetting("root", "gitbucket", "sbt compile"))
      }
    }
    Ok()
  })

  ajaxPost("/:owner/:repository/build/kill/:buildNumber")(writableUsersOnly { repository =>
    val buildNumber = params("buildNumber").toLong
    killBuild(repository.owner, repository.name, buildNumber)
    Ok()
  })

  ajaxGet("/:owner/:repository/build/status")(referrersOnly { repository =>
    import gitbucket.core.view.helpers._

    val queuedJobs = getQueuedJobs(repository.owner, repository.name).map { job =>
      JobStatus(
        buildNumber = job.buildNumber,
        status      = "waiting",
        sha         = job.sha,
        startTime   = "",
        endTime     = "" ,
        duration    = ""
      )
    }

    val runningJobs = getRunningJobs(repository.owner, repository.name).map { case (job, _) =>
      JobStatus(
        buildNumber = job.buildNumber,
        status      = "running",
        sha         = job.sha,
        startTime   = job.startTime.map { startTime => datetime(new java.util.Date(startTime)) }.getOrElse(""),
        endTime     = "",
        duration    = ""
      )
    }

    val finishedJobs = getBuildResults(repository.owner, repository.name).map { job =>
      JobStatus(
        buildNumber = job.buildNumber,
        status      = if(job.success) "success" else "failure",
        sha         = job.sha,
        startTime   = datetime(new java.util.Date(job.start)),
        endTime     = datetime(new java.util.Date(job.end)),
        duration    = ((job.end - job.start) / 1000) + "sec"
      )
    }

    contentType = formats("json")
    Serialization.write((queuedJobs ++ runningJobs ++ finishedJobs).sortBy(_.buildNumber * -1))(jsonFormats)
  })

  case class JobOutput(status: String, output: String)
  case class JobStatus(buildNumber: Long, status: String, sha: String, startTime: String, endTime: String, duration: String)

  @throws[java.io.IOException]
  private def colorize(text: String) = {
    using(new ByteArrayOutputStream()){ os =>
      using(new HtmlAnsiOutputStream(os)){ hos =>
        hos.write(text.getBytes("UTF-8"))
      }
      new String(os.toByteArray, "UTF-8")
    }
  }

}
