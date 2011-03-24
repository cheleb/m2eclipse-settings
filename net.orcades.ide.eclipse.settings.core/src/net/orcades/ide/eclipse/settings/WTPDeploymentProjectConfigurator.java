package net.orcades.ide.eclipse.settings;

import java.io.File;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

public class WTPDeploymentProjectConfigurator extends ProjectConfigurator {

	private static final IPath SRC_MAIN_WEBAPP = new Path("src"
			+ File.separator + "main" + File.separator + "webapp");

	public WTPDeploymentProjectConfigurator() {

	}

	@Override
	public void configure(
			ProjectConfigurationRequest projectConfigurationRequest,
			IProgressMonitor monitor) throws CoreException {

		IProject project = projectConfigurationRequest.getProject();

		IVirtualComponent component = ComponentCore.createComponent(project);

		MavenProject mavenProject = projectConfigurationRequest
				.getMavenProject();

		IVirtualFolder rootFolder = component.getRootFolder();

		Plugin warPlugin = mavenProject
				.getPlugin("org.apache.maven.plugins:maven-war-plugin");

		IPath src = null;
		if (warPlugin != null) {
			Xpp3Dom configurationXpp3Dom = (Xpp3Dom) warPlugin
					.getConfiguration();
			if (configurationXpp3Dom != null) {
				Xpp3Dom warSourceDirectoryXpp3Dom = configurationXpp3Dom
						.getChild("warSourceDirectory");
				if (warSourceDirectoryXpp3Dom != null) {
					src = new Path(warSourceDirectoryXpp3Dom.getValue());
				}
			}
		}
		if (src == null) {
			src = SRC_MAIN_WEBAPP;
		}

		IContainer srcFolder = rootFolder.getUnderlyingFolder();
		if (srcFolder.exists()) {
			System.out.println(srcFolder);
		}

		deployWebAppResources(mavenProject, project, monitor, rootFolder, src);

		WTPMavenHelper.deployTargetJNLP(mavenProject, monitor, rootFolder,
				console, src);

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

	private void deployWebAppResources(MavenProject mavenProject,
			IProject project, IProgressMonitor monitor,
			IVirtualFolder rootFolder, IPath src) throws CoreException {

		IVirtualFolder webinfClasses = rootFolder.getFolder("WEB-INF/classes");

		addClassesAndResourcesToWTPDeployment(project, mavenProject,
				webinfClasses, monitor);

		// if (!webinfClasses.exists()) {
		// System.err.println("no WEB-INF/classes");
		// }
		//
		// if (project.getFolder("src/main/java").exists()) {
		// webinfClasses.createLink(new Path("src/main/java"),
		// IVirtualResource.FOLDER, monitor);
		// }
		//
		// List<Resource> resources = mavenProject.getResources();
		//
		// if (resources.isEmpty()) {
		// if (project.getFolder("src/main/resources").exists()) {
		// webinfClasses.createLink(new Path("src/main/resources"),
		// IVirtualResource.FOLDER, monitor);
		// }
		// } else {
		// for (Resource resource : resources) {
		// String path = resource.getDirectory();
		// path = WTPMavenHelper.getProjectRelativeRelativePath(path,
		// basedir);
		// webinfClasses.createLink(new Path(path),
		// IVirtualResource.FOLDER, monitor);
		// }
		// }

		WTPMavenHelper.deployExtraWebResources(mavenProject, monitor,
				rootFolder, src);
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
