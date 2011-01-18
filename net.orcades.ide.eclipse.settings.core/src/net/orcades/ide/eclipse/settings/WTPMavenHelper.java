package net.orcades.ide.eclipse.settings;

import java.util.Map;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;

public class WTPMavenHelper {

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
	 * 
	 * @param buildPluginMap
	 * @param monitor
	 * @param rootFolder
	 * @throws CoreException
	 */
	public static void deployExtraWebResources(
			Map<String, Plugin> buildPluginMap, IProgressMonitor monitor,
			IVirtualFolder rootFolder) throws CoreException {
		if (buildPluginMap
				.containsKey("org.apache.maven.plugins:maven-war-plugin")) {
			Plugin warPlugin = buildPluginMap
					.get("org.apache.maven.plugins:maven-war-plugin");
			Xpp3Dom configuration = (Xpp3Dom) warPlugin.getConfiguration();

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

				rootFolder.createLink(new Path(path), IVirtualResource.FOLDER,
						monitor);

			}
		}
	}

	public static void deployTargetJNLP(Map<String, Plugin> buildPluginMap, IProgressMonitor monitor,
			IVirtualFolder rootFolder) throws CoreException {
		if (buildPluginMap
				.containsKey("org.codehaus.mojo.webstart:webstart-maven-plugin")) {
			IVirtualFolder webstart = rootFolder.getFolder("webstart");

			if (webstart.exists()) {
				rootFolder.removeLink(new Path("target/jnlp"),
						IVirtualResource.FOLDER, monitor);
			}
			webstart.createLink(new Path("target/jnlp"),
					IVirtualResource.FOLDER, monitor);
		}
	}
}
