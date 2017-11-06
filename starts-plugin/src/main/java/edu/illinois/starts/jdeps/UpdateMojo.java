/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Update class and test dependencies.
 */
@Mojo(name = "update", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
public class UpdateMojo extends DiffMojo {
    public void execute() throws MojoExecutionException {
        long start = System.currentTimeMillis();
        Set<String> nonAffected = null;
        String inFilename = getArtifactsDir() + File.separator + "non-affected-tests";
        File inFile = new File(inFilename);
        if (inFile.isFile()) {
            nonAffected = readFromFile(inFile, getArtifactsDir());
            long end = System.currentTimeMillis();
            Logger.getGlobal().log(Level.FINE, "[PROFILE] readFromFile " + inFilename + " : "
                    + Writer.millsToSeconds(end - start));
        } else {
            Pair<Set<String>, Set<String>> data = computeChangeData();
            nonAffected = data == null ? new HashSet<String>() : data.getKey();
            long end = System.currentTimeMillis();
            Logger.getGlobal().log(Level.FINE, "[PROFILE] computeChangeData(): "
                            + Writer.millsToSeconds(end - start));
        }
        //updateDiffChecksums is always true for this mojo
        updateForNextRun(nonAffected);
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
