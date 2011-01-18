package net.orcades.ide.eclipse.settings;

import java.util.Map;

import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

public class WTPDeploymentProjectConfigurator extends ProjectConfigurator {

	public WTPDeploymentProjectConfigurator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void configure(
			ProjectConfigurationRequest projectConfigurationRequest,
			IProgressMonitor monitor) throws CoreException {

		IProject project = projectConfigurationRequest.getProject();

		IVirtualComponent component = ComponentCore.createComponent(project);

		IVirtualFolder rootFolder = component.getRootFolder();

		Map<String, Plugin> buildPluginMap = projectConfigurationRequest
				.getMavenProject().getBuild().getPluginsAsMap();

		WTPMavenHelper.deployTargetJNLP(buildPluginMap, monitor, rootFolder);

		deployWebAppResources(project, buildPluginMap, monitor, rootFolder);

		customizeWebapp(projectConfigurationRequest, component);

		publishMavenDependency(monitor, project);

	}

	private void customizeWebapp(
			ProjectConfigurationRequest projectConfigurationRequest,
			IVirtualComponent component) {
		component.setMetaProperty("java-output-path", "/target/classes");

		String finalName = projectConfigurationRequest.getMavenProject()
				.getBuild().getFinalName();
		component.setMetaProperty("context-root", finalName);
	}

	private void deployWebAppResources(IProject project,
			Map<String, Plugin> buildPluginMap, IProgressMonitor monitor,
			IVirtualFolder rootFolder) throws CoreException {
		IVirtualFolder webinfClasses = rootFolder.getFolder("WEB-INF/classes");
		if (!webinfClasses.exists()) {
			System.err.println("no WEB-INF/classes");
		} else {
			// REMOVE the wtp src source folder
			// rootFolder.removeLink(new Path("src"), IVirtualResource.NONE,
			// monitor);
			if (project.getFolder("src/main/java").exists()) {
				webinfClasses.createLink(new Path("src/main/java"),
						IVirtualResource.FOLDER, monitor);
			}
			if (project.getFolder("src/main/resources").exists()) {
				webinfClasses.createLink(new Path("src/main/resources"),
						IVirtualResource.FOLDER, monitor);
			}
		}

		WTPMavenHelper.deployExtraWebResources(buildPluginMap, monitor,
				rootFolder);
	}

	private void publishMavenDependency(IProgressMonitor monitor,
			IProject project) throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry classpathEntries[] = javaProject.getRawClasspath();

		for (int i = 0; i < classpathEntries.length; i++) {
			IClasspathEntry iClasspathEntry = classpathEntries[i];

			if ("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER"
					.equals(iClasspathEntry.getPath().lastSegment())) {

				IClasspathAttribute extraAttributes[] = new IClasspathAttribute[1];
				extraAttributes[0] = JavaCore.newClasspathAttribute(
						"org.eclipse.jst.component.dependency", "/WEB-INF/lib");
				IClasspathEntry updated = JavaCore.newContainerEntry(
						iClasspathEntry.getPath(),
						iClasspathEntry.getAccessRules(), extraAttributes,
						iClasspathEntry.isExported());

				classpathEntries[i] = updated;

			}
			javaProject.setRawClasspath(classpathEntries, monitor);
		}
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent event,
			IProgressMonitor monitor) throws CoreException {
		if (event == null) {
			console.logMessage("===> No event");
		} else if (event.getMavenProject() == null) {
			console.logMessage("===> No maven project facade");
		} else if (event.getMavenProject().getMavenProject() == null) {
			console.logMessage("===> No event maven project");
		} else if (event.getMavenProject().getMavenProject().getArtifactId() == null) {
			console.logMessage("===> No artifactId");
		} else {
			console.logMessage(event.getMavenProject().getMavenProject()
					.getArtifactId()
					+ "\n" + event.getSource() + " changed");
		}

	}
}
