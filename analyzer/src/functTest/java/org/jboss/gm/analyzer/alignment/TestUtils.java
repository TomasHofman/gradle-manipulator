package org.jboss.gm.analyzer.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.gradle.api.Project;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.jboss.gm.common.io.ManipulationIO;
import org.jboss.gm.common.model.ManipulationModel;

public final class TestUtils {

    private TestUtils() {
    }

    static ManipulationModel align(File projectRoot, String projectDirName) throws IOException, URISyntaxException {
        return align(projectRoot, projectDirName, false);
    }

    static ManipulationModel align(File projectRoot, String projectDirName, boolean expectFailure)
            throws IOException, URISyntaxException {

        FileUtils.copyDirectory(Paths
                .get(TestUtils.class.getClassLoader().getResource(projectDirName).toURI()).toFile(), projectRoot);
        assertThat(projectRoot.toPath().resolve("build.gradle")).exists();

        final BuildResult buildResult;
        final TaskOutcome outcome;

        final GradleRunner runner = GradleRunner.create()
                .withProjectDir(projectRoot)
                .withArguments("--stacktrace", "--info", AlignmentTask.NAME, "-DrepoRemovalBackup=settings.xml")
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath();

        if (expectFailure) {
            buildResult = runner.buildAndFail();
            outcome = TaskOutcome.FAILED;
        } else {
            outcome = TaskOutcome.SUCCESS;
            buildResult = runner.build();
        }

        assertThat(buildResult.task(":" + AlignmentTask.NAME).getOutcome()).isEqualTo(outcome);

        if (expectFailure) {
            throw new ManipulationUncheckedException(buildResult.getOutput());
        } else {
            return ManipulationIO.readManipulationModel(projectRoot);
        }
    }

    public static String getLine(File projectRoot) throws IOException, ManipulationException {

        List<String> lines = FileUtils.readLines(new File(projectRoot, Project.DEFAULT_BUILD_FILE), Charset.defaultCharset());

        return org.jboss.gm.common.utils.FileUtils.getFirstLine(lines);
    }
}
