/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.util.ChecksumUtil;
import edu.illinois.starts.util.Logger;
import edu.illinois.yasgl.DirectedGraph;
import edu.illinois.yasgl.DirectedGraphBuilder;
import org.apache.maven.surefire.booter.Classpath;
import org.ekstazi.util.Types;

/**
 * Utility methods for loading several things from disk.
 */
public class Loadables implements StartsConstants {
    private static final Logger LOGGER = Logger.getGlobal();

    Map<String, Set<String>> deps;
    List<String> extraEdges;
    private List<String> classesToAnalyze;
    private File cache;
    private String sfPathString;
    private DirectedGraph<String> graph;
    private Map<String, Set<String>> transitiveClosure;
    private Set<String> unreached;
    private boolean filterLib;
    private boolean useThirdParty;
    private Classpath surefireClasspath;
    private String artifactsDir;

    public Loadables(List<String> classesToAnalyze, String artifactsDir, String sfPathString,
                     boolean useThirdParty, boolean filterLib, File cache) {
        this.classesToAnalyze = classesToAnalyze;
        this.artifactsDir = artifactsDir;
        this.sfPathString = sfPathString;
        this.filterLib = filterLib;
        this.cache = cache;
        this.useThirdParty = useThirdParty;
    }

    public DirectedGraph<String> getGraph() {
        return graph;
    }

    public Map<String, Set<String>> getTransitiveClosure() {
        return transitiveClosure;
    }

    public Set<String> getUnreached() {
        return unreached;
    }

    public List<String> getClasspathWithNoJars() {
        // There is a cache of all third party libraries, remove third-party jars from jdeps classpath
        // ASSUMPTION: local dependencies (modules in the same mvn project) are directories, not jars
        List<String> localPaths = new ArrayList<>();
        if (surefireClasspath != null) {
            for (String path : surefireClasspath.getClassPath()) {
                if (!path.endsWith(JAR_EXTENSION) && new File(path).exists()) {
                    localPaths.add(path);
                }
            }
        }
        return localPaths;
    }

    public Loadables create(List<String> moreEdges, Classpath sfClassPath,
                            boolean computeUnreached) {
        setSurefireClasspath(sfClassPath);
        LOGGER.log(Level.FINEST, "More: " + moreEdges.size());
        extraEdges = moreEdges;
        long startTime = System.currentTimeMillis();
        deps = getDepMap(sfPathString, classesToAnalyze);
        long jdepsTime = System.currentTimeMillis();
        graph = makeGraph(deps, extraEdges);
        long graphBuildingTime = System.currentTimeMillis();
        transitiveClosure = getTransitiveClosurePerClass(graph, classesToAnalyze);
        long transitiveClosureTime = System.currentTimeMillis();
        if (computeUnreached) {
            unreached = findUnreached(deps, transitiveClosure);
            LOGGER.log(Level.INFO, "UNREACHED(count): " + unreached.size());
        }
        long findUnreachedTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "[PROFILE] createLoadable(runJDeps): " + Writer.millsToSeconds(jdepsTime - startTime));
        LOGGER.log(Level.FINE, "[PROFILE] createLoadable(buildGraph): "
                + Writer.millsToSeconds(graphBuildingTime - jdepsTime));
        LOGGER.log(Level.FINE, "[PROFILE] createLoadable(transitiveClosure): "
                + Writer.millsToSeconds(transitiveClosureTime - graphBuildingTime));
        LOGGER.log(Level.FINE, "[PROFILE] createLoadable(findUnreached): "
                + Writer.millsToSeconds(endTime - findUnreachedTime));
        LOGGER.log(Level.FINE, "[PROFILE] createLoadable(TOTAL): " + Writer.millsToSeconds(endTime - startTime));
        LOGGER.log(Level.INFO, "STARTS:Nodes: " + graph.getVertices().size());
        LOGGER.log(Level.INFO, "STARTS:Edges: " + graph.getEdges().size());
        return this;
    }

    /**
     * This method takes (i) the dependencies that jdeps found and (i) the map from tests to reachable
     * types in the graph, and uses these to find types jdeps found but which are not reachable by any test.
     * @param deps      The dependencies that jdeps found.
     * @param testDeps  The map from test to types that can be reached in the graph.
     * @return          The set of types that are not reachable by any test in the graph.
     */
    private Set<String> findUnreached(Map<String, Set<String>> deps,
                                      Map<String, Set<String>> testDeps) {
        Set<String> allClasses = new HashSet<>();
        for (String loc : deps.keySet()) {
            // 1. jdeps finds no dependencies for a class if the class' dependencies were not analyzed (e.g., no -R)
            // 2. every class in the CUT has non-empty jdeps dependency; they , at least, depend on java.lang.Object
            // 3. isWellKnownUrl will ignore classes from junit, hamcrest, maven, etc; we don't want to track those
            // 4. isIgnorableInternalName will ignore classes from standard library, mockito, jacoco
            String className = ChecksumUtil.toClassName(loc);
            if (!deps.get(loc).isEmpty()
                    || !ChecksumUtil.isWellKnownUrl(className)
                    || !Types.isIgnorableInternalName(className)) {
                // this means that this a class we want to track, either because it is in the CUT
                // or in some jar that we are tracking
                allClasses.add(loc);
            }
        }
        LOGGER.log(Level.INFO, "ALL(count): " + allClasses.size());
        Set<String> reached = new HashSet<>(testDeps.keySet());
        for (String test : testDeps.keySet()) {
            reached.addAll(testDeps.get(test));
        }
        // remove the reached classes from allClasses to get the unreached classes.
        allClasses.removeAll(reached);
        return allClasses;
    }

    private DirectedGraph<String> makeGraph(Map<String, Set<String>> deps,
                                            List<String> moreEdges) {
        DirectedGraphBuilder<String> builder = getBuilderFromDeps(deps);
        addEdgesToGraphBuilder(builder, moreEdges);
        return builder.build();
    }

    private DirectedGraphBuilder<String> getBuilderFromDeps(Map<String, Set<String>> deps) {
        DirectedGraphBuilder<String> builder = new DirectedGraphBuilder<>();
        for (String key : deps.keySet()) {
            for (String dep : deps.get(key)) {
                builder.addEdge(key, dep);
            }
        }
        return builder;
    }

    public Map<String, Set<String>> getDepMap(String pathToUse, List<String> classes)
            throws IllegalArgumentException {
        if (classes.isEmpty()) {
            //There are no test classes, no need to waste time with jdeps
            return null;
        }
        List<String> args = new ArrayList<>(Arrays.asList("-v"));
        if (filterLib) {
            // TODO: We need a cleaner/generic way to add filters
            args.addAll(Arrays.asList("-filter", "java.*|sun.*"));
        }
        List<String> localPaths = getClasspathWithNoJars();
        if (localPaths.isEmpty()) {
            throw new IllegalArgumentException("JDEPS cannot run with an empty classpath.");
        }
        String jdepsClassPath;
        if ((!cache.exists() || (cache.isDirectory() && cache.list().length == 0)) && useThirdParty) {
            //There is no cache of jdeps graphs, so we want to run jdeps recursively with the entire surefire classpath
            LOGGER.log(Level.WARNING, "Should jdeps cache really be empty? Running in recursive mode.");
            args.add("-R");
            jdepsClassPath = pathToUse;
        } else {
            jdepsClassPath = Writer.pathToString(localPaths);
        }
        args.addAll(Arrays.asList("-cp", jdepsClassPath));
        args.addAll(localPaths);
        LOGGER.log(Level.FINEST, "JDEPS CMD: " + args);
        Map<String, Set<String>> depMap = RTSUtil.runJdeps(args);
        if (LOGGER.getLoggingLevel().intValue() == Level.FINEST.intValue()) {
            Writer.writeMapToFile(depMap, artifactsDir + File.separator + "jdeps-out");
        }
        return depMap;
    }

    private void addEdgesToGraphBuilder(DirectedGraphBuilder<String> builder, List<String> edges) {
        for (String edge : edges) {
            String[] parts = edge.split(WHITE_SPACE);
            if (parts.length != 2) {
                LOGGER.log(Level.SEVERE, "@@BrokenEdge: " + edge);
                continue;
            }
            String src = parts[0].intern();
            String dest = parts[1].intern();
            builder.addEdge(src, dest);
        }
    }

    public static Map<String, Set<String>> getTransitiveClosurePerClass(DirectedGraph<String> tcGraph,
                                                                  List<String> classesToAnalyze) {
        Map<String, Set<String>> tcPerTest = new HashMap<>();
        for (String test : classesToAnalyze) {
            Set<String> deps = YasglHelper.computeReachabilityFromChangedClasses(
                    new HashSet<>(Arrays.asList(test)), tcGraph);
            deps.add(test);
            tcPerTest.put(test, deps);
        }
        return tcPerTest;
    }

    public void setSurefireClasspath(Classpath surefireClasspath) {
        this.surefireClasspath = surefireClasspath;
    }
}
