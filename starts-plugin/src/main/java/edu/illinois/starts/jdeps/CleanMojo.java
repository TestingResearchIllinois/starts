/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.File;

import edu.illinois.starts.helpers.FileUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Removes STARTS plugin artifacts.
 */
@Mojo(name = "clean", requiresDirectInvocation = true)
public class CleanMojo extends BaseMojo {
    public void execute() throws MojoExecutionException {
        File directory = new File(getArtifactsDir());
        if (directory.exists()) {
            FileUtil.delete(directory);
        }
    }
}
