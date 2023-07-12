/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.util.*;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.enums.TransitiveClosureOptions;
import edu.illinois.starts.helpers.EkstaziHelper;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.helpers.ZLCHelperMethods;
import edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder;
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

// import static edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder.buildMethodsGraph;
// import static edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder.methodName2MethodNames;

@Mojo(name = "methods", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class MethodsMojo extends DiffMojo implements StartsConstants {

    private Logger logger;
    private Set<String> impacted;
    private Set<String> changed;
    private Set<String> affected;

    @Parameter(property = "updateMethodsChecksums", defaultValue = FALSE)
    private boolean updateMethodsChecksums;

    /*
     * TODO
     * 1. Build dependency graph
     * 2. Check if method-deps.zlc is built. If not, build it with null values.
     * 3. Compute changes to find changed methods.
     * 4. Find impacted methods.
     */
    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();

        // Build method level static dependencies
        try {
            MethodLevelStaticDepsBuilder.buildMethodsGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<Set<String>> data = ZLCHelperMethods.getChangedData(getArtifactsDir(), cleanBytes);

        changed = data == null ? new HashSet<String>() : data.get(0);
        affected = data == null ? new HashSet<String>() : data.get(1);
        impacted = findImpactedMethods(affected);

        logger.log(Level.FINEST, "CHANGED: " + changed.toString());
        logger.log(Level.FINEST, "IMPACTED: " + impacted.toString());
        // Optionally update methods-deps.zlc
        if (updateMethodsChecksums) {
            this.updateForNextRun(null);
        }

        Writer.writeToFile(changed, "changed-methods", getArtifactsDir());
        Writer.writeToFile(impacted, "impacted-methods", getArtifactsDir());
    }

    protected void updateForNextRun(Set<String> nonAffected) throws MojoExecutionException {
        Classpath sfClassPath = getSureFireClassPath();
        ClassLoader loader = createClassLoader(sfClassPath);
        ZLCHelperMethods.updateZLCFile(MethodLevelStaticDepsBuilder.methodName2MethodNames, loader, getArtifactsDir(),
                nonAffected, useThirdParty, zlcFormat);
    }

    private Set<String> findImpactedMethods(Set<String> affectedMethods) {
        Set<String> impactedMethods = new HashSet<>(affectedMethods);
        Map<String, Set<String>> graph = MethodLevelStaticDepsBuilder.methodName2MethodNames;
        for (String method : affectedMethods) {
            if (graph.containsKey(method)) {
                impactedMethods.addAll(graph.get(method));
            }
        }
        return impactedMethods;
    }
}