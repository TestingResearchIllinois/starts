/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import edu.illinois.starts.util.Logger;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;

/**
 * Update class and test dependencies.
 */
public class UpdateMojoRunnable implements Runnable {
    static Semaphore mutex = new Semaphore(1);

    /**
     * This should only be set to "true" when the previous run goal set it to true as well.
     * the previous run goal saves nonAffectedTests as a file on disk when this value is true,
     * then the update goal can load the file to prevent running computeChangeData() twice.
     */
    private boolean writeNonAffected;

    /**
     * The plugin currently being executed, which should be starts.
     */
    private Plugin plugin;

    /**
     * The project currently being build.
     */
    private MavenProject project;

    /**
     * The current Maven session.
     */
    private MavenSession session;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager pluginManager;

    private Logger logger;

    public UpdateMojoRunnable(Plugin plugin, MavenProject project, MavenSession session, BuildPluginManager pluginManager,
                              boolean writeNonAffected) {
        this.plugin = plugin;
        this.project = project;
        this.session = session;
        this.pluginManager = pluginManager;
        this.writeNonAffected = writeNonAffected;
    }

    public void run() {
        try {
            executeMojo(
                    plugin,
                    goal("update"),
                    configuration(element(name("writeNonAffected"), String.valueOf(writeNonAffected))),
                    executionEnvironment(project, session, pluginManager)
            );
            mutex.release();
        } catch (MojoExecutionException mee) {
            mee.printStackTrace();
            mutex.release();
        }
    }
}
