import javax.servlet.ServletContext

import gitbucket.core.controller.Context
import gitbucket.core.plugin.{Link, PluginRegistry, ReceiveHook}
import gitbucket.core.service.{RepositoryService, SystemSettingsService}
import io.github.gitbucket.ci.controller.SimpleCIController
import io.github.gitbucket.ci.hook.SimpleCICommitHook
import io.github.gitbucket.ci.service.BuildManager
import io.github.gitbucket.ci.servlet.SimpleCIServlet
import io.github.gitbucket.solidbase.model.Version
import org.scalatra.servlet.RichServletContext

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "ci"
  override val pluginName: String = "CI Plugin"
  override val description: String = "This plugin adds simple CI functionality to GitBucket."
  override val versions: List[Version] = List(new Version("1.0.0"))

  override val assetsMappings = Seq("/ci" -> "/gitbucket/ci/assets")

  override val controllers = Seq(
    "/*" -> new SimpleCIController()
  )

  override val repositoryMenus = Seq(
    (repository: RepositoryService.RepositoryInfo, context: Context) => Some(Link("build", "Build", "/build"))
  )

  override val receiveHooks: Seq[ReceiveHook] = Seq(new SimpleCICommitHook())

  override def initialize(registry: PluginRegistry, context: ServletContext, settings: SystemSettingsService.SystemSettings): Unit = {
    super.initialize(registry, context, settings)
    RichServletContext(context).mount(new SimpleCIServlet, "/*")
  }


  BuildManager.startBuildManager()
}
