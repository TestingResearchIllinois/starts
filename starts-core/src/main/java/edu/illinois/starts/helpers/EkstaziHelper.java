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

import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.ekstazi.check.AffectedChecker;

/**
 * Utility methods for interacting with Ekstazi.
 */
public class EkstaziHelper {
    public static final Logger LOGGER = Logger.getGlobal();
    public static String notFirstRunMarker = "not-first-run.clz";

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
        writeEkstaziDebugInfo(baosErr, artifactsDir);
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]COMPUTING NON-AFFECTED: " + (end - start) + "ms");
        Set<String> nonAffected = new HashSet<>(Arrays.asList(baosOut.toString().split("\n")));
        //TODO: parse .clz files to find what changed in the same pass as finding nonaffected tests
        Set<String> changed = new HashSet<>();
        return new Pair<>(nonAffected, changed);
    }

    private static boolean isFirstRun(String artifactsDir) {
        // If the notFirstRunMarker file does not exist, this is a first run
        return !(new File(artifactsDir, notFirstRunMarker).exists());
    }

    public static Set<String> getNonAffectedTests(File basedir) {
        long start = System.currentTimeMillis();
        List<String> nonAffected = AffectedChecker.findNonAffectedClasses(basedir, getRootDirOption(basedir));
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]COMPUTING NON-AFFECTED(2): " + (end - start) + "ms");
        return toFQN(new HashSet<>(nonAffected));
    }

    private static void writeEkstaziDebugInfo(ByteArrayOutputStream baosErr, String artifactsDir) {
        if (LOGGER.getLoggingLevel().intValue() > Level.FINEST.intValue()) {
            return;
        }
        String outFilename = artifactsDir + File.separator + "changed-classes";
        Set<String> changed = new HashSet<>();
        for (String line : Arrays.asList(baosErr.toString().split("\n"))) {
            if (line.contains("::Diff::")) {
                // This assumes that only classes in the application can
                // change, and not classes in jars
                String fqn = Writer.toFQN(line);
                changed.add(fqn);
            }
        }
        try (BufferedWriter writer = Writer.getWriter(outFilename)) {
            for (String fqn : changed) {
                writer.write(fqn + "\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
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
            diffFQNs.add(d.replace(".java", "").replace("/", "."));
        }
        return diffFQNs;
    }
}
