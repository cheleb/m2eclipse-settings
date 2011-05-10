package net.orcades.ide.eclipse.settings;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.wtp.MarkedException;

public class AbstractDependencyConfigurator extends
		org.maven.ide.eclipse.wtp.AbstractDependencyConfigurator {

	public AbstractDependencyConfigurator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void configureDependency(MavenProject arg0, IProject arg1,
			MavenProject arg2, IProject arg3, IProgressMonitor arg4)
			throws MarkedException {
		// TODO Auto-generated method stub

	}

}
