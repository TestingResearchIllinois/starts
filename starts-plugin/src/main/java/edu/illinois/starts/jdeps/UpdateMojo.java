/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;

/**
 * Update class and test dependencies.
 */
@Mojo(name = "update", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
public class UpdateMojo extends DiffMojo {
    /**
     * Set this to "false" to prevent checksums from being persisted to disk when
     * updateRunChecksums is false and to prevent running updateForNextRun() twice
     * when updateRunChecksums is true.
     */
    @Parameter(property = "updateUpdateChecksums", defaultValue = "true")
    private boolean updateUpdateChecksums;

    /**
     * This should only be set to "true" when the previous run goal set it to true as well.
     * the previous run goal saves nonAffectedTests as a file on disk when this value is true,
     * then the update goal can load the file to prevent running computeChangeData() twice.
     */
    @Parameter(property = "writeNonAffected", defaultValue = "false")
    private boolean writeNonAffected;


    private Logger logger;

    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        long start = System.currentTimeMillis();
        logger = Logger.getGlobal();
        logger.log(Level.INFO, "********** Update **********");

        Set<String> nonAffected = new HashSet<>();
        String filenameNonAffected = getArtifactsDir() + File.separator + "non-affected-tests";
        File fileNonAffected = new File(filenameNonAffected);
        if (writeNonAffected && fileNonAffected.isFile()) {
            try {
                List<String> testNames = Files.readAllLines(fileNonAffected.toPath(), StandardCharsets.UTF_8);
                nonAffected.addAll(testNames);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            long end = System.currentTimeMillis();
            logger.log(Level.FINE, "[PROFILE] readFromFile " + filenameNonAffected + " : "
                    + Writer.millsToSeconds(end - start));
        } else {
            Pair<Set<String>, Set<String>> data = computeChangeData();
            if(data != null) nonAffected = data.getKey();
            long end = System.currentTimeMillis();
            logger.log(Level.FINE, "[PROFILE] computeChangeData(): "
                            + Writer.millsToSeconds(end - start));
        }
        if (updateUpdateChecksums) {
            updateForNextRun(nonAffected);
        }

        long end = System.currentTimeMillis();
        System.setProperty("[PROFILE] END-OF-UPDATE-MOJO: ", Long.toString(end));
        logger.log(Level.FINE, "[PROFILE] UPDATE-MOJO-TOTAL: " + Writer.millsToSeconds(end - start));
    }

    public Set<String> readFromFile(File inFile, String artifactsDir) {
        Set<String> result = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inFile))) {
            String className = null;
            while ((className = reader.readLine()) != null) {
                result.add(className);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return result;
    }
}
