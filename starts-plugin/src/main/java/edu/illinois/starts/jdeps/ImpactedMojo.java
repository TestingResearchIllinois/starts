/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import edu.illinois.yasgl.DirectedGraph;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.booter.Classpath;

/**
 * Find all types that are impacted by a change.
 */
@Mojo(name = "impacted", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class ImpactedMojo extends DiffMojo {
    /**
     * Set this to "true" to update test dependencies on disk. The default value of "false"
     * is useful for "dry runs" where one may want to see the diff without updating
     * the test dependencies.
     */
    @Parameter(property = "updateImpactedChecksums", defaultValue = "false")
    private boolean updateImpactedChecksums;
    private Logger logger;

    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        Pair<Set<String>, Set<String>> data = computeChangeData();
        // 0. Find all classes in program
        List<String> allClasses = getAllClasses();
        Set<String>  impacted = new HashSet<>(allClasses);
        // 1a. Find what changed and what is non-affected
        Set<String> nonAffected = data == null ? new HashSet<String>() : data.getKey();
        Set<String> changed = data == null ? new HashSet<String>() : data.getValue();

        // 1b. Remove nonAffected from all classes to get classes impacted by the change
        impacted.removeAll(nonAffected);

        logger.log(Level.FINEST, "CHANGED: " + changed.toString());
        logger.log(Level.FINEST, "IMPACTED: " + impacted.toString());
        // 2. Optionally update ZLC file for next run, using all classes in the SUT
        if (updateImpactedChecksums) {
            updateForNextRun(allClasses);
        }
        // 3. Print impacted and/or write to file
        Writer.writeToFile(changed, "changed-classes", getArtifactsDir());
        Writer.writeToFile(impacted, "impacted-classes", getArtifactsDir());
    }

    private void updateForNextRun(List<String> allClasses) throws MojoExecutionException {
        long start = System.currentTimeMillis();
        Classpath sfClassPath = getSureFireClassPath();
        String sfPathString = Writer.pathToString(sfClassPath.getClassPath());
        ClassLoader loader = createClassLoader(sfClassPath);
        Result result = prepareForNextRun(sfPathString, sfClassPath, allClasses, new HashSet<String>(), false);
        ZLCHelper zlcHelper = new ZLCHelper();
        zlcHelper.updateZLCFile(result.getTestDeps(), loader, getArtifactsDir(), new HashSet<String>());
        long end = System.currentTimeMillis();
        if (logger.getLoggingLevel().intValue() <= Level.FINEST.intValue()) {
            save(getArtifactsDir(), sfPathString, result.getGraph());
        }
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(total): " + Writer.millsToSeconds(end - start));
    }

    private void save(String artifactsDir, String sfPathString, DirectedGraph<String> graph) {
        RTSUtil.saveForNextRun(artifactsDir, graph, printGraph, graphFile);
        Writer.writeClassPath(sfPathString, artifactsDir);
    }
}
