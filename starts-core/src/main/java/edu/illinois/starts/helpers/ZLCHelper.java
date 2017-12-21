/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.data.ZLCData;
import edu.illinois.starts.util.ChecksumUtil;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.ekstazi.util.Types;

/**
 * Utility methods for dealing with the .zlc format.
 */
public class ZLCHelper implements StartsConstants {
    public static final String zlcFile = "deps.zlc";
    public static final String STAR_FILE = "file:*";
    private static final Logger LOGGER = Logger.getGlobal();
    private static Map<String, ZLCData> zlcDataMap;
    private static final String NOEXISTING_ZLCFILE_FIRST_RUN = "@NoExistingZLCFile. First Run?";

    public ZLCHelper() {
        zlcDataMap = new HashMap<>();
    }

    // TODO: Uncomment and fix this method. The problem is that it does not track newly added tests correctly
    public static void updateZLCFile(Map<String, Set<String>> testDeps, ClassLoader loader, String artifactsDir,
                                     Set<String> changedClasses, Set<String> newClasses, boolean incrementalUpdate) {
        long start = System.currentTimeMillis();
        File file = new File(artifactsDir, zlcFile);
        if (!file.exists() || !incrementalUpdate) {
            List<ZLCData> zlc = createZLCData(testDeps, loader);
            Writer.writeToFile(zlc, zlcFile, artifactsDir);
        } else {
            List<ZLCData> zlcData = new ArrayList<>();
            if (zlcDataMap != null) {
                // handle changed data
                for (Map.Entry<String, ZLCData> entry : zlcDataMap.entrySet()) {
                    String extForm = entry.getKey();
                    if (changedClasses.contains(extForm)) {
                        String fqn = Writer.urlToFQN(extForm);
                        Set<String> tests = new HashSet<>();
                        for (String test : testDeps.keySet()) {
                            if (testDeps.get(test).contains(fqn)) {
                                tests.add(test);
                            }
                        }
                        if (!tests.isEmpty()) {
                            entry.getValue().setTests(tests);

                        }
                    }
                    zlcData.add(entry.getValue());
                }

                //handle new data
                addZLCData(testDeps, loader, newClasses, zlcData);
            }
            Writer.writeToFile(zlcData, zlcFile, artifactsDir);
        }
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "[PROFILE] updateForNextRun(updateZLCFile): " + Writer.millsToSeconds(end - start));
    }

    private static void addZLCData(Map<String, Set<String>> testDeps, ClassLoader loader,
                                      Set<String> newClasses, List<ZLCData> zlcData) {
        ChecksumUtil checksumUtil = new ChecksumUtil(true);
        for (String dep : newClasses) {
            String klas = ChecksumUtil.toClassName(dep);
            if (Types.isIgnorableInternalName(klas)) {
                continue;
            }
            URL url = loader.getResource(klas);
            if (url == null) {
                throw new NullPointerException("resource cannot be found: " + klas);
            }
            if (ChecksumUtil.isWellKnownUrl(url.toExternalForm())) {
                continue;
            }
            String checksum = checksumUtil.computeSingleCheckSum(url);
            Set<String> tests = new HashSet<>();
            for (String test : testDeps.keySet()) {
                if (testDeps.get(test).contains(dep)) {
                    tests.add(test);
                }
            }
            // must check empty
            if (tests.isEmpty()) {
                continue;
            }
            zlcData.add(new ZLCData(url, checksum, tests));
        }
    }

    public static List<ZLCData> createZLCData(Map<String, Set<String>> testDeps, ClassLoader loader) {
        long start = System.currentTimeMillis();
        List<ZLCData> zlcData = new ArrayList<>();
        Set<String> deps = new HashSet<>();
        // merge all the deps for all tests into a single set
        for (String test : testDeps.keySet()) {
            deps.addAll(testDeps.get(test));
        }

        // for each dep, find its url, checksum and tests that depend on it
        addZLCData(testDeps, loader, deps, zlcData);
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]CREATING ZLC FILE: " + (end - start) + MILLISECOND);
        return zlcData;
    }

    public static Pair<Set<String>, Set<String>> getChangedData(String artifactsDir, boolean cleanBytes) {
        long start = System.currentTimeMillis();
        File zlc = new File(artifactsDir, zlcFile);
        if (!zlc.exists()) {
            LOGGER.log(Level.FINEST, NOEXISTING_ZLCFILE_FIRST_RUN);
            return null;
        }
        Set<String> changedClasses = new HashSet<>();
        Set<String> nonAffected = new HashSet<>();
        Set<String> affected = new HashSet<>();
        Set<String> starTests = new HashSet<>();
        ChecksumUtil checksumUtil = new ChecksumUtil(cleanBytes);
        try {
            List<String> zlcLines = Files.readAllLines(zlc.toPath(), Charset.defaultCharset());
            String firstLine = zlcLines.get(0);
            String space = WHITE_SPACE;

            // check whether the first line is for *
            if (firstLine.startsWith(STAR_FILE)) {
                String[] parts = firstLine.split(space);
                starTests = fromCSV(parts[2]);
                zlcLines.remove(0);
            }

            for (String line : zlcLines) {
                String[] parts = line.split(space);
                String stringURL = parts[0];
                String oldCheckSum = parts[1];
                Set<String> tests = parts.length == 3 ? fromCSV(parts[2]) : new HashSet<String>();
                nonAffected.addAll(tests);
                URL url = new URL(stringURL);
                String newCheckSum = checksumUtil.computeSingleCheckSum(url);
                if (!newCheckSum.equals(oldCheckSum)) {
                    affected.addAll(tests);
                    changedClasses.add(stringURL);
                }
                if (newCheckSum.equals("-1")) {
                    // a class was deleted or auto-generated, no need to track it in zlc
                    LOGGER.log(Level.FINEST, "Ignoring: " + url);
                    continue;
                }
                ZLCData data = new ZLCData(url, newCheckSum, tests);
                zlcDataMap.put(stringURL, data);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        if (!changedClasses.isEmpty()) {
            // there was some change so we need to add all tests that reach star, if any
            affected.addAll(starTests);
        }
        nonAffected.removeAll(affected);
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);
        return new Pair<>(nonAffected, changedClasses);
    }

    private static Set<String> fromCSV(String tests) {
        return new HashSet<>(Arrays.asList(tests.split(COMMA)));
    }

    // get changed classes (not from jar) from given changed data (may contain jar)
    public static Set<String> getChangedClasses(Set<String> changedData) {
        Set<String> changedClasses = new HashSet<>();
        if (changedData == null || changedData.isEmpty()) {
            return changedClasses;
        }
        for (String changed : changedData) {
            if (changed.startsWith("file")) {
                changedClasses.add(Writer.urlToFQN(changed));
            }
        }
        return changedClasses;
    }

    // not actually get all existing classes in the project, but get existing classes related to tests
    // some existing classes in project, which is not transitively related to any tests, will not be returned by this
    public static Set<String> getExistingClasses(String artifactsDir) {
        Set<String> existingClasses = new HashSet<>();
        long start = System.currentTimeMillis();
        File zlc = new File(artifactsDir, zlcFile);
        if (!zlc.exists()) {
            LOGGER.log(Level.FINEST, NOEXISTING_ZLCFILE_FIRST_RUN);
            return existingClasses;
        }
        try {
            List<String> zlcLines = Files.readAllLines(zlc.toPath(), Charset.defaultCharset());
            for (String line : zlcLines) {
                if (line.startsWith("file")) {
                    existingClasses.add(Writer.urlToFQN(line.split(WHITE_SPACE)[0]));
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]COMPUTING EXISTING CLASSES: " + (end - start) + MILLISECOND);
        return existingClasses;
    }
}
