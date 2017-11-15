/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Invoked after after running selected tests (see lifecycle.xml for details).
 */
@Mojo(name = "starts", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST, lifecycle = "starts")
public class StartsMojo extends RunMojo implements StartsConstants {
    private Logger logger;

    @Parameter(property = "enableMojoExecutor", defaultValue = "true")
    private boolean enableMojoExecutor;
    /**
     * The project currently being build.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager pluginManager;


    public void execute() throws MojoExecutionException {
        long endOfRunMojo = Long.parseLong(System.getProperty(PROFILE_END_OF_RUN_MOJO));
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        long end = System.currentTimeMillis();
        logger.log(Level.FINE, PROFILE_TEST_RUNNING_TIME + Writer.millsToSeconds(end - endOfRunMojo));

        if (enableMojoExecutor) {
            executeMojo(
                    plugin(
                            groupId("edu.illinois"),
                            artifactId("starts-maven-plugin"),
                            version("1.4-SNAPSHOT")
                    ),
                    goal("update"),
                    configuration(),
                    executionEnvironment(mavenProject, mavenSession, pluginManager)
            );
        }
        end = System.currentTimeMillis();
        logger.log(Level.FINE, "[PROFILE] STARTS-MOJO-TOTAL: " + Writer.millsToSeconds(end - endOfRunMojo));
    }
}
