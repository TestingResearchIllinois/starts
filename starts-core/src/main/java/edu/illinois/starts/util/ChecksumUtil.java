/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.illinois.starts.constants.StartsConstants;
import org.ekstazi.data.RegData;
import org.ekstazi.data.TxtStorer;
import org.ekstazi.hash.Hasher;
import org.ekstazi.util.Types;

public class ChecksumUtil implements StartsConstants {
    public static final String JAVAHOME = System.getProperty(JAVA_HOME);
    private static final Logger LOGGER = Logger.getLogger(ChecksumUtil.class.getName());

    Hasher hasher;
    private Map<URL, String> checkSumMap; // map from URL to checksums, to reduce hashing

    public ChecksumUtil(boolean cleanBytes) {
        checkSumMap = new HashMap<>();
        hasher = new Hasher(Hasher.Algorithm.CRC32, 1000, cleanBytes);
    }

    /**
     * This method creates the checksum map only for tests that are affected by changes.
     *
     * @param loader   The classloader from which to find .class files
     * @param testDeps The transitive closure of dependencies for each test
     * @param affected The set of tests that are affected by the changes
     * @return The checksum map
     */
    public static Map<String, Set<RegData>> makeCheckSumMap(ClassLoader loader, Map<String,
            Set<String>> testDeps, Set<String> affected) {
        Map<String, Set<RegData>> checksums = new HashMap<>();
        ChecksumUtil checksumUtil = new ChecksumUtil(true);
        for (String test : affected) {
            checksums.put(test, new HashSet<RegData>());
            URL url = loader.getResource(toClassName(test));
            checksums.get(test).add(checksumUtil.computeChecksumRegData(url));
            long start = System.currentTimeMillis();
            for (String dep : testDeps.get(test)) {
                String className = toClassName(dep);
                if (!Types.isIgnorableInternalName(className)) {
                    url = loader.getResource(className);
                    if (url != null) {
                        if (!isWellKnownUrl(url.toExternalForm())) {
                            checksums.get(test).add(checksumUtil.computeChecksumRegData(url));
                        }
                    } else {
                        // Known benign cases where this can happen: (i) dep is from a shaded jar which is itself on
                        // the classpath; (ii) dep is from an optional jar dependency of a direct jar dependency (e.g.,
                        // users of joda-time-*.jar do not necessarily depend on classes from joda-convert-8.jar
                        LOGGER.log(Level.FINEST, "@@LoadedNullURLForDep: " + dep);
                    }
                }
            }
            long end = System.currentTimeMillis();
            LOGGER.log(Level.FINEST, "LOADED RESOURCES: " + (end - start) + MILLISECOND);
        }
        return checksums;
    }

    /**
     * Check for so-called "well-known" classes that we don't track for RTS purposes.
     * Copied from Ekstazi.
     *
     * @param klas The class we want to check
     * @return true if klas is a well-known class
     */
    public static boolean isWellKnownUrl(String klas) {
        return klas.contains("!/org/junit") || klas.contains("!/junit") || klas.contains("!/org/hamcrest")
                || klas.contains("!/org/apache/maven") || klas.contains(JAVAHOME);
    }

    public static String toClassName(String fqn) {
        return fqn.replace(DOT, File.separator) + CLASS_EXTENSION;
    }

    public static void saveCheckSums(Map<String, Set<RegData>> newCheckSums, String artifactsDir) {
        for (String test : newCheckSums.keySet()) {
            String checksumPath = makeCheckSumPath(test, artifactsDir);
            writeChecksumFile(checksumPath, newCheckSums.get(test));
        }
    }

    public static String makeCheckSumPath(String test, String artifactsDir) {
        return artifactsDir + test + ".clz";
    }

    public static void writeChecksumFile(String filePath, Set<RegData> data) {
        StartsStorer storer = new StartsStorer(true);
        try {
            File file = new File(filePath);
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file, false);
            storer.save(fos, data);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public RegData computeChecksumRegData(URL url) {
        String checksum = getCheckSum(url);
        return new RegData(url.toExternalForm(), checksum);
    }

    public String getCheckSum(URL url) {
        if (!checkSumMap.containsKey(url)) {
            String value = computeSingleCheckSum(url);
            checkSumMap.put(url, value);
        }
        return checkSumMap.get(url);
    }

    public String computeSingleCheckSum(URL url) {
        return hasher.hashURL(url.toExternalForm());
    }

    static class StartsStorer extends TxtStorer {
        public StartsStorer(boolean checkMagicSequence) {
            super(checkMagicSequence);
        }

        public void save(FileOutputStream fos, Set<RegData> data) {
            SortedSet<RegData> sortedData = new TreeSet<>(new RegData.RegComparator());
            sortedData.addAll(data);
            super.extendedSave(fos, sortedData);
        }
    }
}
