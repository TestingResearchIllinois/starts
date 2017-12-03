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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.SurefirePlugin;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Update class and test dependencies.
 */
public class UpdateMojoRunnable extends SurefirePlugin implements Runnable {
    static Semaphore mutex = new Semaphore(1);

    /**
     * This should only be set to "true" when the previous run goal set it to true as well.
     * the previous run goal saves nonAffectedTests as a file on disk when this value is true,
     * then the update goal can load the file to prevent running computeChangeData() twice.
     */
    private boolean writeNonAffected;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager pluginManager;

    private Logger logger;

    public UpdateMojoRunnable(boolean writeNonAffected) {
        this.writeNonAffected = writeNonAffected;
    }

    public void run() {
        try {
            executeMojo(
                    plugin(
                            groupId(getProject().getGroupId()),
                            artifactId(getProject().getArtifactId()),
                            version(getProject().getVersion())
                    ),
                    goal("update"),
                    configuration(element(name("writeNonAffected"), String.valueOf(writeNonAffected))),
                    executionEnvironment(getProject(), getSession(), pluginManager)
            );
            mutex.release();
        } catch (MojoExecutionException mee) {
            mee.printStackTrace();
        }
    }
}
