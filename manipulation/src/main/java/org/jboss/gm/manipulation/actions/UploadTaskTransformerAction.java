package org.jboss.gm.manipulation.actions;

import org.gradle.api.Action;
import org.gradle.api.artifacts.maven.MavenResolver;
import org.gradle.api.tasks.Upload;
import org.jboss.gm.common.alignment.Project;

/**
 * Fixes pom.xml generation in old "maven" plugin.
 * <p>
 * Adds PomTransformer to all MavenResolver repositories in Upload tasks.
 */
public class UploadTaskTransformerAction implements Action<org.gradle.api.Project> {

    private Project.Module alignmentConfiguration;

    public UploadTaskTransformerAction(Project.Module alignmentConfiguration) {
        this.alignmentConfiguration = alignmentConfiguration;
    }

    @Override
    public void execute(org.gradle.api.Project project) {
        project.getTasks().withType(Upload.class).all(upload -> upload.getRepositories()
                .withType(MavenResolver.class).all(resolver -> {
                    resolver.getPom().withXml(new PomTransformer(alignmentConfiguration));
                }));
    }
}
