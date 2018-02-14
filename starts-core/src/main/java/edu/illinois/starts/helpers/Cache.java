/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.util.Logger;

/** Utility methods for dealing with cached files. */

public class Cache implements StartsConstants {
    private static final Logger LOGGER = Logger.getGlobal();
    private static final String GRAPH_EXTENSION = ".graph";

    File jdepsCache;
    String m2Repo;

    private Set<String> cpJars;

    public Cache(File jdepsCache, String m2Repo) {
        this.jdepsCache = jdepsCache;
        this.m2Repo = m2Repo;
    }

    public List<String> loadM2EdgesFromCache(String pathString) {
        if (!jdepsCache.exists()) {
            if (!jdepsCache.mkdir()) {
                throw new RuntimeException("I could not create the jdeps cache: " + jdepsCache.getAbsolutePath());
            }
        }
        //1. get jars from sfClassPath
        cpJars = getJarsFromCP(pathString);
        //2. get the edges for the jars from jdepsCache (if they are in jdepsCache and add them to moreEdges
        Set<String> jarsInCache = getJarsInGraphCache(cpJars);
        HashSet<String> missing = getJarsMissingFromCache(jarsInCache);
        // Some projects depend directly on jars in the standard library, so
        // we want to check there as well
        jarsInCache.addAll(checkMissingJarsInJDKCache(missing));
        return loadCachedEdges(jarsInCache);

    }

    private HashSet<String> getJarsMissingFromCache(Set<String> jarsInCache) {
        HashSet<String> missing = new HashSet<>();
        if (!cpJars.equals(jarsInCache)) {
            missing = new HashSet<>(cpJars);
            missing.removeAll(jarsInCache);
        }
        return missing;
    }

    private Set<String> checkMissingJarsInJDKCache(HashSet<String> missing) {
        Set<String> found = new HashSet<>();
        Set<String> notFound = new HashSet<>();
        for (String jar : missing) {
            File missingFile = new File(jar);
            String fileName = missingFile.getName();
            File jdkJarGraphFile = new File(jdepsCache, fileName.replace(JAR_EXTENSION, GRAPH_EXTENSION));
            if (jdkJarGraphFile.exists()) {
                found.add(jdkJarGraphFile.getName());
            } else {
                notFound.add(jar);
            }
        }
        List<String> newlyCreated = new ArrayList<>();
        for (String jar : notFound) {
            //1. parse with jdeps and store in the cache
            List<String> args = new ArrayList<>(Arrays.asList("-v", jar));
            Writer.writeDepsToFile(RTSUtil.runJdeps(args), createCacheFile(jar).getAbsolutePath());
            newlyCreated.add(jar);
        }
        //2. add newly-created graphs to list of jars that were previously found in cache
        found.addAll(newlyCreated);
        //3. remove newly-created graphs from list of jars that were not found
        notFound.removeAll(newlyCreated);
        if (notFound.size() > 0) {
            throw new RuntimeException("I could not find or create jdeps graphs in any cache: " + notFound);
        }
        return found;
    }

    private List<String> loadCachedEdges(Set<String> jarsInCache) {
        List<String> edges = new ArrayList<>();
        for (String jar : jarsInCache) {
            File cacheFile = createCacheFile(jar);
            LOGGER.log(Level.FINEST, "@@LoadingFromNormalCache: " + cacheFile.getAbsolutePath());
            try {
                List<String> lines = Files.readAllLines(cacheFile.toPath(), Charset.defaultCharset());
                edges.addAll(lines);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        LOGGER.log(Level.FINEST, "@@LoadedCachedEdgesFromJars: ");
        return edges;
    }

    private Set<String> getJarsInGraphCache(Set<String> cpJars) {
        Set<String> inCache = new HashSet<>();
        for (String jar : cpJars) {
            File cacheJar = createCacheFile(jar);
            if (cacheJar.exists()) {
                inCache.add(jar);
            }
        }
        return inCache;
    }

    private File createCacheFile(String jar) {
        String cachePath = jar.replace(m2Repo + File.separator, EMPTY).replace(JAR_EXTENSION, GRAPH_EXTENSION);
        return new File(jdepsCache, cachePath);
    }

    private Set<String> getJarsFromCP(String sfPathString) {
        if (cpJars != null) {
            return cpJars;
        }
        Set<String> jars = new HashSet<>();
        String[] splitCP = sfPathString.split(File.pathSeparator);
        for (int i = 0; i < splitCP.length; i++) {
            if (splitCP[i].endsWith(JAR_EXTENSION)) {
                jars.add(splitCP[i]);
            }
        }
        return jars;
    }
}
