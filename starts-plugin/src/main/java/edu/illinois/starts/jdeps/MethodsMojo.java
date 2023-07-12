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
    private Set<String> nonAffected;

    @Parameter(property = "updateMethodsChecksums", defaultValue = FALSE)
    private boolean updateMethodsChecksums;
    
    /*  TODO
    1. Build dependency graph 
    2. Check if method-deps.zlc is built. If not, build it with null values.
    3. Compute changes to find changed methods.
    4. Find impacted methods.
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
        
        Pair<Set<String>, Set<String>> data = computeChangeData(false);

        Set<String> allMethods = getAllMethods(MethodLevelStaticDepsBuilder.methodName2MethodNames);

        impacted = new HashSet<>(allMethods);
        // 1a. Find what changed and what is non-affected
        nonAffected = data == null ? new HashSet<String>() : data.getKey();
        changed = data == null ? new HashSet<String>() : data.getValue();
        
        System.out.println("Non affected: " + nonAffected);
        System.out.println("changed: " + changed);
        System.out.println("all: " + allMethods);

        // 1b. Remove nonAffected from all classes to get classes impacted by the change
        impacted.removeAll(nonAffected);


        logger.log(Level.FINEST, "CHANGED: " + changed.toString());
        logger.log(Level.FINEST, "IMPACTED: " + impacted.toString());
        // Optionally update methods-deps.zlc 
        if (updateMethodsChecksums) {
            this.updateForNextRun(nonAffected);
        }

        Writer.writeToFile(changed, "changed-methods", getArtifactsDir());
        Writer.writeToFile(impacted, "impacted-methods", getArtifactsDir());
    }

    protected Pair<Set<String>, Set<String>> computeChangeData(boolean writeChanged) throws MojoExecutionException {
        long start = System.currentTimeMillis();
        Pair<Set<String>, Set<String>> data = null;
        
        if (depFormat == DependencyFormat.ZLC) {
            ZLCHelperMethods.getChangedData(getArtifactsDir(), cleanBytes);
        }

        // Set<String> changed = data == null ? new HashSet<String>() : data.getValue();
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] COMPUTING CHANGES: " + Writer.millsToSeconds(end - start));
        return data;
    }

    protected void updateForNextRun(Set<String> nonAffected) throws MojoExecutionException {
        Classpath sfClassPath = getSureFireClassPath();
        ClassLoader loader = createClassLoader(sfClassPath);
        ZLCHelperMethods.updateZLCFile(MethodLevelStaticDepsBuilder.methodName2MethodNames, loader, getArtifactsDir(), nonAffected, useThirdParty, zlcFormat);
    }

    private Set<String> getAllMethods(Map<String, Set<String>> methodName2MethodNames) throws MojoExecutionException {
        // Set<String> allMethods = new HashSet<>();
        // for (String methodName : methodName2MethodNames.keySet()) {
        //     if (methodName.contains(">")) {
        //         continue;
        //     }
        //     if (!methodName.endsWith(")")) {
        //         continue;
        //     }
        //     methodName.replace("/", ".");
        //     allMethods.add(methodName);
        // }
        // return allMethods;
        return methodName2MethodNames.keySet();
    }

//     private Set<String> findImpactedMethods(Map<String, Set<String>> methodName2MethodNames, Set<String> changed) {
//         Set<String> impactedMethods = new HashSet<>();
// //        System.out.println("method2method: " + methodName2MethodNames);
//         for (String changedMethod : changed) {
//             changedMethod = changedMethod.replace(".class", "");
// //            System.out.println("changedMethod: " + changedMethod);
//             for (String key : methodName2MethodNames.keySet()) {
//                 if (methodName2MethodNames.get(key).isEmpty()) {
//                     continue;
//                 }
// //                System.out.println("key: " + key);
//                 for (String value : methodName2MethodNames.get(key)) {
//                     value = value.replace("()", "");
// //                    System.out.println("value: " + value);
//                     if (changedMethod.endsWith(value)) {
//                         impactedMethods.add(key);
//                     }
//                 }
//             }
//         }
//         return impactedMethods;
//     }
}