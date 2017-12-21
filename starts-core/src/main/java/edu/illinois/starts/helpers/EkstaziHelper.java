/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.ekstazi.check.AffectedChecker;

/**
 * Utility methods for interacting with Ekstazi.
 */
public class EkstaziHelper implements StartsConstants {
    public static final Logger LOGGER = Logger.getGlobal();
    public static String notFirstRunMarker = "not-first-run.clz";
    public static String lineSeparator = System.getProperty("line.separator");

    public static Pair<Set<String>, Set<String>> getNonAffectedTests(String artifactsDir) {
        long start = System.currentTimeMillis();
        if (isFirstRun(artifactsDir)) {
            return null;
        }
        ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        PrintStream psOut = new PrintStream(baosOut);
        PrintStream psErr = new PrintStream(baosErr);
        PrintStream oldOut = System.out;
        PrintStream olderr = System.err;
        System.setOut(psOut);
        System.setErr(psErr);
        AffectedChecker.main((String[]) Arrays.asList(artifactsDir).toArray());
        System.out.flush();
        System.err.flush();
        System.setOut(oldOut);
        System.setErr(olderr);
        Set<String> changed = processEkstaziDebugInfo(baosErr, artifactsDir);
        Set<String> nonAffected = new HashSet<>(Arrays.asList(baosOut.toString().split(lineSeparator)));
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);
        return new Pair<>(nonAffected, changed);
    }

    public static Pair<Set<String>, Set<String>> getNonAffectedTests(File basedir) {
        long start = System.currentTimeMillis();
        List<String> nonAffectedFiles = AffectedChecker.findNonAffectedClasses(basedir, getRootDirOption(basedir));
        long end = System.currentTimeMillis();
        Set<String> nonAffectedTests = toFQN(new HashSet<>(nonAffectedFiles));
        Set<String> changed = new HashSet<>();
        LOGGER.log(Level.FINEST, "[TIME]COMPUTING NON-AFFECTED(2): " + (end - start) + MILLISECOND);
        return new Pair<>(nonAffectedTests, changed);
    }

    private static boolean isFirstRun(String artifactsDir) {
        // If the notFirstRunMarker file does not exist, this is a first run
        return !(new File(artifactsDir, notFirstRunMarker).exists());
    }

    /**
     * Process the Ekstazi debug output to get what classes Ekstazi thinks changed, and write those changed classes
     * to file.
     *
     * @param baosErr      Ekstazi Debug Output
     * @param artifactsDir Directory in which we store STARTS artifacts (i.e., ".starts")
     * @return             The (possibly empty) set of changed classes
     */
    private static Set<String> processEkstaziDebugInfo(ByteArrayOutputStream baosErr, String artifactsDir) {
        Set<String> changed = new HashSet<>();
        if (LOGGER.getLoggingLevel().intValue() > Level.FINEST.intValue()) {
            return changed;
        }
        String outFilename = artifactsDir + File.separator + CHANGED_CLASSES;
        for (String line : Arrays.asList(baosErr.toString().split(lineSeparator))) {
            String ekstaziDiffMarker = "::Diff:: ";
            if (line.contains(ekstaziDiffMarker)) {
                changed.add(line.split(ekstaziDiffMarker)[1]);
            }
        }
        try (BufferedWriter writer = Writer.getWriter(outFilename)) {
            for (String file : changed) {
                writer.write(file + lineSeparator);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return changed;
    }

    private static String getRootDirOption(File basedir) {
        return "root.dir=" + getRootDirURI(basedir);
    }

    private static String getRootDirURI(File rootDir) {
        String artifactsDir = rootDir.getAbsolutePath() + File.separator + ".starts";
        return (new File(artifactsDir)).toURI().toString();
    }

    private static Set<String> toFQN(Set<String> diff) {
        Set<String> diffFQNs = new HashSet<>();
        for (String d : diff) {
            diffFQNs.add(d.replace(".java", EMPTY).replace(File.separator, DOT));
        }
        return diffFQNs;
    }
}
