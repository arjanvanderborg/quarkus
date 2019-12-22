package io.quarkus.gradle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.quarkus.cli.commands.CreateProject;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.SourceType;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

public class QuarkusPluginFunctionalTest {

    private File projectRoot;

    @BeforeEach
    void setUp(@TempDir File projectRoot) {
        this.projectRoot = projectRoot;
    }

    @Test
    public void canRunListExtensions() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("listExtensions"))
                .withProjectDir(projectRoot)
                .build();

        assertThat(build.task(":listExtensions").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(build.getOutput()).contains("Quarkus - Core");
    }

    @ParameterizedTest(name = "Build {0} project")
    //TODO: Fix Scala build in Windows
    @EnumSource(value = SourceType.class, names = {"JAVA","KOTLIN"})
    public void canBuild(SourceType sourceType) throws IOException {
        createProject(sourceType);

        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("build"))
                .withProjectDir(projectRoot)
                .build();

        assertThat(build.task(":build").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        // gradle build should not build the native image
        assertThat(build.task(":buildNative")).isNull();
    }

    private List<String> arguments(String argument) {
        List<String> arguments = new ArrayList<>();
        arguments.add(argument);
        String mavenRepoLocal = System.getProperty("maven.repo.local", System.getenv("MAVEN_LOCAL_REPO"));
        if (mavenRepoLocal != null) {
            arguments.add("-Dmaven.repo.local=" + mavenRepoLocal);
        }
        return arguments;
    }

    private void createProject(SourceType sourceType) throws IOException {
        assertThat(new CreateProject(new FileProjectWriter(projectRoot))
                           .groupId("com.acme.foo")
                           .artifactId("foo")
                           .version("1.0.0-SNAPSHOT")
                           .buildTool(BuildTool.GRADLE)
                           .sourceType(sourceType)
                           .doCreateProject(new HashMap<>()))
                .withFailMessage("Project was not created")
                .isTrue();
    }
}