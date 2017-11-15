/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.util.Logger;
import edu.illinois.yasgl.DirectedGraph;
import edu.illinois.yasgl.Edge;
import org.apache.commons.codec.binary.Hex;

/**
 * Utility methods for writing various data to file.
 */
public class Writer implements StartsConstants {
    private static final Logger LOGGER = Logger.getGlobal();

    public static void writeToFile(Collection col, String filename, String artifactsDir) {
        String outFilename = artifactsDir + File.separator + filename;
        writeToFile(col, outFilename);
    }

    public static void writeToFile(Collection col, String filename) {
        try (BufferedWriter writer = getWriter(filename)) {
            if (col.isEmpty()) {
                writer.write(BLANK);
                return;
            }
            for (Object elem : col) {
                writer.write(elem + System.lineSeparator());
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void writeMapToFile(Map map, String filename) {
        try (BufferedWriter writer = getWriter(filename)) {
            if (map.isEmpty()) {
                writer.write(BLANK);
                return;
            }
            for (Object key : map.keySet()) {
                writer.write(key.toString() + COMMA + map.get(key) + System.lineSeparator());
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void writeClassPath(String sfPathString, String artifactsDir) {
        String outFilename = Paths.get(artifactsDir, SF_CLASSPATH).toString();
        try (BufferedWriter writer = getWriter(outFilename)) {
            writer.write(sfPathString + System.lineSeparator());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void writeJarChecksums(String sfPathString, String artifactsDir) {
        String outFilename = Paths.get(artifactsDir, JAR_CHECKSUMS).toString();
        try (BufferedWriter writer = getWriter(outFilename)) {
            String[] jarsList = sfPathString.split(File.pathSeparator);
            for (int i = 0; i < jarsList.length; i++) {
                if (jarsList[i].isEmpty()) {
                    continue;
                }
                writer.write(getJarToChecksumMapping(jarsList[i]));
                writer.write(System.lineSeparator());
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Write the graph to file, together with any new edges (if any) that we get
     * from parsing classes that changed.
     *
     * @param graph         The graph that we want to write
     * @param artifactsDir  The directory in which we are writing STARTS artifacts
     * @param print         Write graph to file if true
     * @param graphFile     The file in which to optionally write the graph
     */
    public static void writeGraph(DirectedGraph<String> graph, String artifactsDir, boolean print, String graphFile) {
        if (print) {
            String outFilename = artifactsDir + File.separator + graphFile;
            try (BufferedWriter writer = getWriter(outFilename)) {
                if (graph == null) {
                    writer.write(BLANK);
                    return;
                }
                // write all the edges in the graph
                for (Edge<String> edge : graph.getEdges()) {
                    writer.write(edge.getSource() + WHITE_SPACE + edge.getDestination() + System.lineSeparator());
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public static void writeDepsToFile(Map<String, Set<String>> deps, String fileName) {
        try (BufferedWriter writer = getWriter(fileName)) {
            for (String key : deps.keySet()) {
                for (String value : deps.get(key)) {
                    writer.write(key + WHITE_SPACE + value + System.lineSeparator());
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void writeTCSimple(Map<String, Set<String>> testDeps, String artifactsDir, String tcFile) {
        String outFilename = artifactsDir + File.separator + tcFile;
        try (BufferedWriter writer = getWriter(outFilename)) {
            for (String test : testDeps.keySet()) {
                writer.write(test + WHITE_SPACE + test);
                Iterator<String> it = testDeps.get(test).iterator();
                while (it.hasNext()) {
                    writer.write(COMMA + it.next());
                }
                writer.newLine();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static BufferedWriter getWriter(String filePath) {
        Path path = Paths.get(filePath);
        BufferedWriter writer = null;
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return writer;
    }

    public static String pathToString(List<String> classPath) {
        long start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = classPath.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(File.pathSeparator);
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINE, PROFILE_UPDATE_FOR_NEXT_RUN_PATHTOSTRING + millsToSeconds(end - start));
        return sb.toString();
    }

    public static List<String> fqnsToExcludePath(Collection<String> fqns) {
        List<String> paths = new ArrayList<>();
        for (String fqn : fqns) {
            if (fqn.isEmpty()) {
                continue;
            }
            paths.add(fqnToExcludePath(fqn));
        }
        Collections.sort(paths);
        return paths;
    }

    public static String fqnToExcludePath(String fqn) {
        return fqn.replace(DOT, File.separator) + ".*";
    }

    public static void writeToLog(Set<String> set, String title, Logger logger) {
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        logger.log(Level.INFO, BLANK);
        logger.log(Level.INFO, STARS_AND_SPACE + title + SPACE_AND_STARTS);

        for (String listItem : list) {
            logger.log(Level.INFO, listItem);
        }

        if (set.isEmpty()) {
            logger.log(Level.INFO, title + FOUND_NO_CLASSES);
        }
    }

    public static String millsToSeconds(long value) {
        return String.format("%.03f", (double) value / 1000.0);
    }

    /**
     * Compute the checksum for the given map and return the jar
     * and the checksum as a string.
     *
     *
     * @param jar  The jar whose checksum we need to compute.
     */
    public static String getJarToChecksumMapping(String jar) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes;
        int bufSize = 65536 * 2;
        try {
            MessageDigest md = MessageDigest.getInstance(MD5);
            InputStream is = Files.newInputStream(Paths.get(jar));
            bytes = new byte[bufSize];
            int size = is.read(bytes, 0, bufSize);
            while (size >= 0) {
                md.update(bytes, 0, size);
                size = is.read(bytes, 0, bufSize);
            }
            String hex = Hex.encodeHexString(md.digest());
            sb.append(jar).append(COMMA).append(hex);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * Convert the result of URL.toExternalForm() on classes in the program to a fully-qualified name.
     * @param url External form of the URL to convert
     * @return A fully-qualified name of the URL
     */
    public static String urlToFQN(String url) {
        // ASSUMPTION: "classes/" rarely occurs in the rest of the path
        return url.split(CLASSES + File.separator)[1].replace(CLASS_TYPE_NAME, BLANK).replace(File.separator, DOT);
    }
}
