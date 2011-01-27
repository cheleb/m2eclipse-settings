package net.orcades.ide.eclipse.settings;

import java.io.File;
import java.util.Map;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;

public class WTPMavenHelper {

	private static final String ORG_CODEHAUS_MOJO_WEBSTART_WEBSTART_MAVEN_PLUGIN = "org.codehaus.mojo.webstart:webstart-maven-plugin";

	/**
	 * 
	 * 
	 * 
	 * &lt;webResources><br />
	 * &nbsp; &lt;resource><br />
	 * &nbsp;&nbsp; &lt;directory>target/elnweb&lt;/directory><br />
	 * &nbsp;&nbsp; &lt;filtering>false&lt;/filtering><br />
	 * &nbsp; &lt;/resource><br />
	 * &lt;/webResources>
	 * 
	 * @param buildDir
	 * 
	 * 
	 * @param buildPluginMap
	 * @param monitor
	 * @param rootFolder
	 * @throws CoreException
	 */
	public static void deployExtraWebResources(String buildDir,
			Map<String, Plugin> buildPluginMap, IProgressMonitor monitor,
			IVirtualFolder rootFolder) throws CoreException {
		if (buildPluginMap
				.containsKey("org.apache.maven.plugins:maven-war-plugin")) {
			Plugin warPlugin = buildPluginMap
					.get("org.apache.maven.plugins:maven-war-plugin");
			Xpp3Dom configuration = (Xpp3Dom) warPlugin.getConfiguration();

			if (configuration == null) {
				return;
			}

			Xpp3Dom webResources[] = configuration.getChildren("webResources");

			for (int i = 0; i < webResources.length; i++) {
				Xpp3Dom webResource = webResources[i];
				Xpp3Dom resource = webResource.getChild("resource");
				if (resource == null) {
					continue;
				}
				Xpp3Dom directory = resource.getChild("directory");
				if (directory == null) {
					continue;
				}
				String path = directory.getValue();
				if (path == null) {
					continue;
				}

				path = getProjectRelativeRelativePath(path, buildDir);

				rootFolder.createLink(new Path(path), IVirtualResource.FOLDER,
						monitor);

			}
		}
	}

	public static String getProjectRelativeRelativePath(String path,
			String buildDir) {
		path = path.replace('\\', '/');
		buildDir = buildDir.replace('\\', '/');
		int indexOfBuildDir = path.indexOf(buildDir);
		if (indexOfBuildDir == 0) {
			path = path.substring(buildDir.length());
			int lastIndexOfDoubleDotSlash = path.lastIndexOf(".."
					+ File.pathSeparator);
			if (lastIndexOfDoubleDotSlash != -1) {
				path = path.substring(lastIndexOfDoubleDotSlash
						+ "../".length());
			}
		}

		return path;
	}

	public static void deployTargetJNLP(String buildDir,
			Map<String, Plugin> buildPluginMap, IProgressMonitor monitor,
			IVirtualFolder rootFolder) throws CoreException {
		if (buildPluginMap
				.containsKey(ORG_CODEHAUS_MOJO_WEBSTART_WEBSTART_MAVEN_PLUGIN)) {
			IVirtualFolder webstart = rootFolder.getFolder("webstart");

			Plugin plugin = buildPluginMap
					.get(ORG_CODEHAUS_MOJO_WEBSTART_WEBSTART_MAVEN_PLUGIN);

			Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();

			if (configuration == null) {
				return;
			}

			Xpp3Dom workDirectory = configuration.getChild("workDirectory");

			if (workDirectory == null) {
				throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Could not find JNLP directory!"));
			}
			String jnlp = getProjectRelativeRelativePath(
					workDirectory.getValue(), buildDir);
			Path jnlpPath = new Path(jnlp);

			if (webstart.exists()) {
				IContainer[] deployedFolders = webstart.getUnderlyingFolders();
				for (int i = 0; i < deployedFolders.length; i++) {
					IContainer deployedFolder = deployedFolders[i];
					webstart.removeLink(deployedFolder.getProjectRelativePath(), IVirtualResource.FOLDER, monitor);
				}
			}
			webstart.createLink(jnlpPath, IVirtualResource.FOLDER, monitor);
		}
	}
}
