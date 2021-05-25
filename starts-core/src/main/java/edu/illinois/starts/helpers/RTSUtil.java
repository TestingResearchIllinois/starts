/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.maven.AgentLoader;
import edu.illinois.starts.util.ChecksumUtil;
import edu.illinois.starts.util.Logger;
import edu.illinois.yasgl.DirectedGraph;
import org.apache.maven.plugin.MojoExecutionException;
import org.ekstazi.data.RegData;

/**
 * Some utility methods that are needed for RTS.
 */
public class RTSUtil implements StartsConstants {
    private static final Logger LOGGER = Logger.getGlobal();
    // Name of tools.jar in JDK
    private static final String TOOLS_JAR_NAME = "tools.jar";
    // Name of tools.jar on Mac in JDK
    private static final String CLASSES_JAR_NAME = "classes.jar";

    public static void saveForNextRun(String artifactsDir, DirectedGraph<String> graph,
                                      boolean printGraph, String graphFile) {
        long start = System.currentTimeMillis();
        Writer.writeGraph(graph, artifactsDir, printGraph, graphFile);
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]WRITING FILES: " + (end - start) + MILLISECOND);
    }

    public static void computeAndSaveNewCheckSums(String artifactsDir,
                                                  Set<String> affectedTests,
                                                  Map<String, Set<String>> testDeps,
                                                  ClassLoader loader) throws MojoExecutionException {
        long start;
        long end;
        start = System.currentTimeMillis();
        Map<String, Set<RegData>> newCheckSums = ChecksumUtil.makeCheckSumMap(loader, testDeps, affectedTests);
        end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]UPDATING CHECKSUMS: " + (end - start) + MILLISECOND);
        start = System.currentTimeMillis();
        ChecksumUtil.saveCheckSums(newCheckSums, artifactsDir);
        try {
            new File(artifactsDir, EkstaziHelper.notFirstRunMarker).createNewFile();
        } catch (IOException ioe) {
            throw new MojoExecutionException(ioe.getMessage());
        }
        end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]RE-SAVING CHECKSUMS: " + (end - start) + MILLISECOND);
    }

    /**
     * This method computes the affected tests and sets the "changed" field to
     * contain the set of dependencies that changed since the last run.
     */
    public static Set<String> computeAffectedTests(HashSet<String> allTests, Set<String> nonAffected,
                                                   Map<String, Set<String>> testDeps) {
        long start = System.currentTimeMillis();
        Set<String> affectedTests = new HashSet<>(allTests);
        affectedTests.removeAll(nonAffected);
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]COMPUTING AFFECTED: " + (end - start) + MILLISECOND);
        return affectedTests;
    }

    public static Map<String, Set<String>> runJdeps(List<String> args) {
        LOGGER.log(Level.FINE, "JDEPS ARGS:" + args);

        StringWriter output = AgentLoader.loadAndRunJdeps(args);
        // jdeps can return an empty output when run on .jar files with no .class files
        return output.getBuffer().length() != 0 ? getDepsFromJdepsOutput(output) : new HashMap<String, Set<String>>();
    }

    public static Map<String, Set<String>> getDepsFromJdepsOutput(StringWriter jdepsOutput) {
        Map<String, Set<String>> deps = new HashMap<>();
        List<String> lines = Arrays.asList(jdepsOutput.toString().split(System.lineSeparator()));
        for (String line : lines) {
            String[] parts = line.split("->");
            String left = parts[0].trim();
            if (left.startsWith(CLASSES) || left.startsWith(TEST_CLASSES) || left.endsWith(JAR_EXTENSION)) {
                continue;
            }
            String right = parts[1].trim().split(WHITE_SPACE)[0];
            if (deps.keySet().contains(left)) {
                deps.get(left).add(right);
            } else {
                deps.put(left, new HashSet<>(Arrays.asList(right)));
            }
        }
        return deps;
    }
}
