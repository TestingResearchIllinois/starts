/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;

import edu.illinois.starts.helpers.FileUtil;
import edu.illinois.starts.helpers.PomUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Restores the excludesFile after running selected tests
 * (see lifecycle.xml for details).
 */
@Mojo(name = "starts", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST, lifecycle = "starts")
public class StartsMojo extends RunMojo {
    @Parameter(property = "restoreExcludes", defaultValue = "true")
    protected boolean restoreExcludes;

    private Logger logger;

    public void execute() throws MojoExecutionException {
        long endOfRunMojo = Long.parseLong(System.getProperty("[PROFILE] END-OF-RUN-MOJO: "));
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        long updateTime;
        long start = System.currentTimeMillis();
        if (updateRunChecksums) {
            File nonAffectedFile = new File(getArtifactsDir(), "non-affected-tests");
            try {
                updateForNextRun(new HashSet(FileUtil.readLinesFromFile(nonAffectedFile)));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            updateTime = System.currentTimeMillis();
            logger.log(Level.FINE, "[PROFILE] STARTS-MOJO-UPDATE-TIME: " + Writer.millsToSeconds(updateTime - start));
        }
        long startRestore = System.currentTimeMillis();
        if (restoreExcludes) {
            Plugin sfPlugin = PomUtil.getSfPlugin(getProject());
            PomUtil.restoreExcludesFile(sfPlugin, getWorkingDirectory());
        }
        long endRestore = System.currentTimeMillis();
        logger.log(Level.FINE, "[PROFILE] TEST-RUNNING-TIME: " + Writer.millsToSeconds(start - endOfRunMojo));
        logger.log(Level.FINE, "[PROFILE] STARTS-MOJO-RESTORE-TIME: " + Writer.millsToSeconds(endRestore - startRestore));
        logger.log(Level.FINE, "[PROFILE] STARTS-MOJO-TOTAL: " + Writer.millsToSeconds(endRestore - start));
    }
}
