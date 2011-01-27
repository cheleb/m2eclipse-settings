package net.orcades.ide.eclipse.settings;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * 
 * @author onouguie
 *
 */
@Deprecated
public class PublishWTPCheckTask extends PublishTaskDelegate {

	@Override
	public PublishOperation[] getTasks(IServer server, int kind,
			@SuppressWarnings("rawtypes") List modules,
			@SuppressWarnings("rawtypes") List kindList) {
		final Set<IVirtualComponent> virtualComponent = new HashSet<IVirtualComponent>();
		for (Object object : modules) {
			IModule[] moduleArray = (IModule[]) object;
			for (int i = 0; i < moduleArray.length; i++) {
				IModule module = moduleArray[i];

				if (module == null) {
					System.err.println("Module is null.");
					continue;
				}
				if (module.getProject() == null) {
					System.err.println("Module: " + module
							+ "'s project is null.");
					continue;
				}
				IFile pomFile = module.getProject().getFile("pom.xml");
				if (pomFile.exists()) {
					IVirtualComponent vcomponent = ComponentCore
							.createComponent(module.getProject());
					if (virtualComponent.add(vcomponent)) {
						System.out.println("Maven Project");
					}
				}

			}
		}

		for (IVirtualComponent iVirtualComponent : virtualComponent) {
			IMavenProjectFacade facade = MavenPlugin.getDefault()
					.getMavenProjectManager()
					.getProject(iVirtualComponent.getProject());
			MavenProject mavenProject = facade.getMavenProject();
			Build build = mavenProject.getBuild();
			Map<String, Plugin> buildPluginMap = build.getPluginsAsMap();

			IVirtualFolder rootFolder = iVirtualComponent.getRootFolder();

			Properties properties = mavenProject.getProperties();
			
			String buildDir = properties.getProperty("buildDir", mavenProject.getBasedir().getAbsolutePath());
			
			try {
				WTPMavenHelper.deployExtraWebResources(mavenProject.getBuild().getDirectory(), buildPluginMap, null,
						rootFolder);
				WTPMavenHelper.deployTargetJNLP(buildDir, buildPluginMap, null,
						rootFolder);
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
		}

		return super.getTasks(server, kind, modules, kindList);

	}

	@Override
	public PublishOperation[] getTasks(IServer server,
			@SuppressWarnings("rawtypes") List modules) {
		return super.getTasks(server, modules);

	}

}
