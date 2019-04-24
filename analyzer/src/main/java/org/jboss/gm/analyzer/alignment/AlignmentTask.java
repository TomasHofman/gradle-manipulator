package org.jboss.gm.analyzer.alignment;

import static org.jboss.gm.common.alignment.AlignmentUtils.getCurrentAlignmentModel;
import static org.jboss.gm.common.alignment.AlignmentUtils.writeUpdatedAlignmentModel;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.tasks.TaskAction;
import org.jboss.gm.common.ProjectVersionFactory;
import org.jboss.gm.common.alignment.AlignmentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;

/**
 * The actual Gradle task that creates the alignment.json file for the whole project
 * (whether it's a single or multi module project)
 */
public class AlignmentTask extends DefaultTask {

    static final String NAME = "generateAlignmentMetadata";

    private static final Logger log = LoggerFactory.getLogger(AlignmentTask.class);

    /**
     * The idea here is for every project to read the current alignment file from disk,
     * add the dependency alignment info for the specific project which for which the task was ran
     * and write the updated model back to disk
     * TODO the idea described above is probably very inefficient so we probably want to explore ways to do it better
     */
    @TaskAction
    public void perform() {
        final Project project = getProject();
        final String projectName = project.getName();
        System.out.println("Starting alignment task for project " + projectName);

        final Collection<ProjectVersionRef> deps = getAllProjectDependencies(project);
        final AlignmentService alignmentService = AlignmentServiceFactory.getAlignmentService(project);
        final String currentProjectVersion = project.getVersion().toString();
        final AlignmentService.Response alignmentResponse = alignmentService.align(
                new AlignmentService.Request(
                        ProjectVersionFactory.withGAV(project.getGroup().toString(), projectName,
                                currentProjectVersion),
                        deps));

        final AlignmentModel alignmentModel = getCurrentAlignmentModel(project);
        final AlignmentModel.Module correspondingModule = alignmentModel.findCorrespondingModule(projectName);

        correspondingModule.setNewVersion(alignmentResponse.getNewProjectVersion());
        updateModuleDependencies(correspondingModule, deps, alignmentResponse);

        writeUpdatedAlignmentModel(project, alignmentModel);
    }

    private Collection<ProjectVersionRef> getAllProjectDependencies(Project project) {
        final Set<ProjectVersionRef> result = new LinkedHashSet<>();
        project.getConfigurations().all(configuration -> configuration.getAllDependencies()
                .forEach(dep -> {
                    if (dep instanceof DefaultSelfResolvingDependency) {
                        log.warn("Ignoring dependency of type {} on project {}", dep.getClass().getName(), project.getName());
                    } else {
                        result.add(
                                ProjectVersionFactory.withGAVAndConfiguration(dep.getGroup(), dep.getName(), dep.getVersion(),
                                        configuration.getName()));
                    }
                }));
        try {
            // Horrible Things
            //
            // Trying to figure out how to access "managed dependencies" that should be aligned. These are probably:
            // 1. imported BOMs
            // 2. explicit managed deps
            //
            // Is it possible to read these data directly from build file?

            Object dmObject = project.getExtensions().getByName("dependencyManagement");
            dmObject.getClass().getClassLoader();

            ClassLoader gradleClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(dmObject.getClass().getClassLoader());

            try {
                try {
                    Thread.currentThread().getContextClassLoader()
                            .loadClass("io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension");
                } catch (ClassNotFoundException e) {
                    log.error("CNF", e);
                }
                DependencyManagementExtension dependencyManagement = (DependencyManagementExtension) dmObject;

                // this contains all the stuff from BOMs (transitive) and explicit managed dependencies
                Map<String, String> managedVersions = dependencyManagement.getManagedVersions();

                // closed API
                // managed dependencies
//                dependencyManagement.dependencyManagementContainer.getGlobalDependencyManagement().getManagedDependencies();
                // imported BOMs
//                dependencyManagement.dependencyManagementContainer.getGlobalDependencyManagement().importedBoms;
            } finally {
                // return the original class loader
                Thread.currentThread().setContextClassLoader(gradleClassLoader);
            }

        } catch (Throwable t) {
            t.printStackTrace();
            log.error("class loading error", t);
            throw t;
        }
        return result;
    }

    private void updateModuleDependencies(AlignmentModel.Module correspondingModule,
            Collection<ProjectVersionRef> allModuleDependencies, AlignmentService.Response alignmentResponse) {

        allModuleDependencies.forEach(d -> {
            final String newDependencyVersion = alignmentResponse.getAlignedVersionOfGav(d);
            if (newDependencyVersion != null) {
                final ProjectVersionRef newVersion = ProjectVersionFactory.withNewVersion(d, newDependencyVersion);
                correspondingModule.getAlignedDependencies().put(d.toString(), newVersion);
            }
        });
    }

}
