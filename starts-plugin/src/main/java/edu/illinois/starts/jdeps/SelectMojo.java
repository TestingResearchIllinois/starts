/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Finds tests affected by a change but does not run them.
 */
@Mojo(name = "select", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class SelectMojo extends DiffMojo {
    /**
     * Set this to "true" to update test dependencies on disk. The default value of
     * "false" is useful for "dry runs" where one may want to see the affected
     * tests, without updating test dependencies.
     */
    @Parameter(property = "updateSelectChecksums", defaultValue = "false")
    private boolean updateSelectChecksums;

    private Logger logger;

    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        long start = System.currentTimeMillis();
        Set<String> affectedTests = computeAffectedTests();
        printResult(affectedTests, "AffectedTests");
        long end = System.currentTimeMillis();
        logger.log(Level.FINE, "[PROFILE] RUN-MOJO-TOTAL: " + Writer.millsToSeconds(end - start));
        logger.log(Level.FINE, "[PROFILE] TEST-RUNNING-TIME: " + 0.0);
    }

    private Set<String> computeAffectedTests() throws MojoExecutionException {
        setIncludesExcludes();
        Set<String> allTests = new HashSet<>(getTestClasses("checkIfAllAffected"));
        Set<String> affectedTests = new HashSet<>(allTests);
        Pair<Set<String>, Set<String>> data = computeChangeData();
        Set<String> nonAffectedTests = data == null ? new HashSet<String>() : data.getKey();
        affectedTests.removeAll(nonAffectedTests);
        if (allTests.equals(nonAffectedTests)) {
            logger.log(Level.INFO, "********** Run **********");
            logger.log(Level.INFO, "No tests are selected to run.");
        }
        long startUpdate = System.currentTimeMillis();
        if (updateSelectChecksums) {
            updateForNextRun(nonAffectedTests);
        }
        long endUpdate = System.currentTimeMillis();
        logger.log(Level.FINE, "[PROFILE] STARTS-MOJO-UPDATE-TIME: " + Writer.millsToSeconds(endUpdate - startUpdate));
        return affectedTests;
    }
}
