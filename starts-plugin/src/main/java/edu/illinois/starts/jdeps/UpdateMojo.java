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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

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
        String nonAffectedFilename = getArtifactsDir() + File.separator + "non-affected-tests";
        File nonAffectedFile = new File(nonAffectedFilename);
        if (writeNonAffected && nonAffectedFile.isFile()) {
            try {
                nonAffected.addAll(Files.readAllLines(nonAffectedFile.toPath(), StandardCharsets.UTF_8));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            long end = System.currentTimeMillis();
            logger.log(Level.FINE, "[PROFILE] readFromFile " + nonAffectedFilename + " : "
                    + Writer.millsToSeconds(end - start));
        } else {
            Pair<Set<String>, Set<String>> data = computeChangeData();
            if (data != null) {
                nonAffected = data.getKey();
            }
        }
        if (updateUpdateChecksums) {
            updateForNextRun(nonAffected);
        }

        long end = System.currentTimeMillis();
        System.setProperty("[PROFILE] END-OF-UPDATE-MOJO: ", Long.toString(end));
        logger.log(Level.FINE, "[PROFILE] UPDATE-MOJO-TOTAL: " + Writer.millsToSeconds(end - start));
    }
}
