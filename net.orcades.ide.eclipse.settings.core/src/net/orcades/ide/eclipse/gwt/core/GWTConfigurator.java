package net.orcades.ide.eclipse.gwt.core;

import java.util.Map;
import java.util.Properties;

import net.orcades.ide.eclipse.settings.ProjectConfigurator;

import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
/**
 *  
 * @author olivier.nouguier@gmail.com
 *
 */
public class GWTConfigurator extends ProjectConfigurator {

	@Override
	public void configure(
			ProjectConfigurationRequest projectConfigurationRequest,
			IProgressMonitor monitor) throws CoreException {
		doConfigure(projectConfigurationRequest.getProject(),
				projectConfigurationRequest.getMavenProjectFacade(), monitor);

	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent event,
			IProgressMonitor monitor) throws CoreException {
		super.mavenProjectChanged(event, monitor);
		doConfigure(event.getMavenProject().getProject(),
				event.getMavenProject(), monitor);
	}

	public void doConfigure(IProject project, IMavenProjectFacade mavenProject,
			IProgressMonitor monitor) throws CoreException {
		
		Properties properties = mavenProject.getMavenProject().getProperties();
		String gwtWarDir = properties.getProperty("gwt.war", "");
		
		Preferences preferences = getPrefences(project, "com.google.gdt.eclipse.core");
		preferences.put("warSrcDir", gwtWarDir);
		preferences.put("warSrcDirIsOutput", "true");
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			console.logError(e.getMessage());
		}
		
		IVirtualComponent component = ComponentCore.createComponent(project);
		if(component == null) {
			console.logError("Not yet a component");
			return;
		}
		
		IVirtualFolder rootFolder = component.getRootFolder();
		Map<String, Plugin> buildPluginMap = mavenProject.getMavenProject()
				.getBuild().getPluginsAsMap();
		if (buildPluginMap.containsKey("org.codehaus.mojo:gwt-maven-plugin")) {

			addNature(project, "com.google.gwt.eclipse.core.gwtNature", monitor);
			addBuildCommand(project,
					"com.google.gdt.eclipse.core.webAppProjectValidator",
					monitor);
			addBuildCommand(project,
					"com.google.gwt.eclipse.core.gwtProjectValidator", monitor);

			rootFolder.createLink(new Path("war"), IVirtualResource.FOLDER,
					monitor);
			
			String gwtVersion = null;
			if(properties.containsKey("gwt.version")) {
				gwtVersion = properties.getProperty("gwt.version");
			}
			insurePresenceOfGWTDependency(monitor, project, gwtVersion);
		} else {
			console.logError("GWT Configurator active but no GWT build MOJO (org.codehaus.mojo:gwt-maven-plugin) found in pom.xml build section!");
		}
	}

	private void insurePresenceOfGWTDependency(IProgressMonitor monitor, IProject project, String gwtVersion)
			throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry prevClasspathEntries[] = javaProject.getRawClasspath();

		for (int i = 0; i < prevClasspathEntries.length; i++) {
			IClasspathEntry iClasspathEntry = prevClasspathEntries[i];
			if ("com.google.gwt.eclipse.core.GWT_CONTAINER"
					.equals(iClasspathEntry.getPath().segment(0))) {
				return;
			}
		}
		StringBuilder gwtLibrairyPath = new StringBuilder("com.google.gwt.eclipse.core.GWT_CONTAINER");
		if(gwtVersion != null) {
			gwtLibrairyPath.append('/').append(gwtVersion);
		}
		IClasspathEntry newClasspathEntries[] = new IClasspathEntry[prevClasspathEntries.length + 1];
		System.arraycopy(prevClasspathEntries, 0, newClasspathEntries, 0,
				prevClasspathEntries.length);
		IClasspathEntry updated = JavaCore.newContainerEntry(new Path(
				gwtLibrairyPath.toString()),
				null, null, false);

		newClasspathEntries[prevClasspathEntries.length] = updated;

		javaProject.setRawClasspath(newClasspathEntries, monitor);
	}

}
