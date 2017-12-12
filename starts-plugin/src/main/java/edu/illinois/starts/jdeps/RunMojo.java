/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.maven.AgentLoader;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Prepares for test runs by writing non-affected tests in the excludesFile.
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST)
public class RunMojo extends DiffMojo implements StartsConstants {
    private static final String TARGET = "target";
    /**
     * Set this to "false" to prevent checksums from being persisted to disk. This
     * is useful for "dry runs" where one may want to see the non-affected tests that
     * STARTS writes to the Surefire excludesFile, without updating test dependencies.
     */
    @Parameter(property = "updateRunChecksums", defaultValue = TRUE)
    protected boolean updateRunChecksums;

    /**
     * Set this option to "true" to run all tests, not just the affected ones. This option is useful
     * in cases where one is interested to measure the time to run all tests, while at the
     * same time measuring the times for analyzing what tests to select and reporting the number of
     * tests it would select.
     * Note: Run with "-DstartsLogging=FINER" or "-DstartsLogging=FINEST" so that the "selected-tests"
     * file, which contains the list of tests that would be run if this option is set to false, will
     * be written to disk.
     */
    @Parameter(property = "retestAll", defaultValue = FALSE)
    protected boolean retestAll;

    /**
     * Set this to "true" to save nonAffectedTests to a file on disk. This improves the time for
     * updating test dependencies in offline mode by not running computeChangeData() twice.
     * Note: Running with "-DstartsLogging=FINEST" also saves nonAffectedTests to a file on disk.
     */
    @Parameter(property = "writeNonAffected", defaultValue = "false")
    protected boolean writeNonAffected;

    protected Set<String> nonAffectedTests;
    protected Set<String> changedClasses;
    private Logger logger;

    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        long start = System.currentTimeMillis();
        setChangedAndNonaffected();
        List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTests);
        setIncludesExcludes();
        if (writeNonAffected || logger.getLoggingLevel().intValue() <= Level.FINEST.intValue()) {
            Writer.writeToFile(nonAffectedTests, "non-affected-tests", getArtifactsDir());
        }
        run(excludePaths);
        Set<String> allTests = new HashSet<>(getTestClasses(CHECK_IF_ALL_AFFECTED));
        if (allTests.equals(nonAffectedTests)) {
            logger.log(Level.INFO, STARS_RUN_STARS);
            logger.log(Level.INFO, NO_TESTS_ARE_SELECTED_TO_RUN);
        }
        long end = System.currentTimeMillis();
        System.setProperty(PROFILE_END_OF_RUN_MOJO, Long.toString(end));
        logger.log(Level.FINE, PROFILE_RUN_MOJO_TOTAL + Writer.millsToSeconds(end - start));
    }

    protected void run(List<String> excludePaths) throws MojoExecutionException {
        String sfPathString = getCleanClassPath(Writer.pathToString(getSureFireClassPath().getClassPath()));
        if (retestAll || !isSameClassPath(sfPathString) || !hasSameJarChecksum(sfPathString)) {
            dynamicallyUpdateExcludes(new ArrayList<String>());
            Writer.writeClassPath(sfPathString, artifactsDir);
            Writer.writeJarChecksums(getCleanClassPath(sfPathString), artifactsDir);
        } else {
            dynamicallyUpdateExcludes(excludePaths);
        }
        long startUpdateTime = System.currentTimeMillis();
        if (updateRunChecksums) {
            updateForNextRun(nonAffectedTests);
        }
        long endUpdateTime = System.currentTimeMillis();
        logger.log(Level.FINE, PROFILE_STARTS_MOJO_UPDATE_TIME
                + Writer.millsToSeconds(endUpdateTime - startUpdateTime));
    }

    private void dynamicallyUpdateExcludes(List<String> excludePaths) throws MojoExecutionException {
        if (AgentLoader.loadDynamicAgent()) {
            logger.log(Level.FINEST, "AGENT LOADED!!!");
            System.setProperty(STARTS_EXCLUDE_PROPERTY, Arrays.toString(excludePaths.toArray(new String[0])));
        } else {
            throw new MojoExecutionException("I COULD NOT ATTACH THE AGENT");
        }
    }

    protected void setChangedAndNonaffected() throws MojoExecutionException {
        nonAffectedTests = new HashSet<>();
        changedClasses = new HashSet<>();
        Pair<Set<String>, Set<String>> data = computeChangeData();
        nonAffectedTests = data == null ? new HashSet<String>() : data.getKey();
        changedClasses  = data == null ? new HashSet<String>() : data.getValue();
    }

    private boolean isSameClassPath(String sfPathString) throws MojoExecutionException {
        String oldSfPathFileName = Paths.get(getArtifactsDir(), SF_CLASSPATH).toString();
        if (!new File(oldSfPathFileName).exists()) {
            return false;
        }
        try {
            String cleanOldSfPathFileName = getCleanClassPath(Files.readAllLines(Paths.get(oldSfPathFileName)).get(0));
            Set<String> sfClassPathSet = new HashSet<>(Arrays.asList(sfPathString.split(File.pathSeparator)));
            Set<String> oldSfClassPathSet = new HashSet<>(Arrays.asList(cleanOldSfPathFileName.split(File.pathSeparator)));
            if (sfClassPathSet.equals(oldSfClassPathSet)) {
                return true;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }

    private boolean hasSameJarChecksum(String cleanSfClassPath) throws MojoExecutionException {
        String oldChecksumPathFileName = Paths.get(getArtifactsDir(), JAR_CHECKSUMS).toString();
        boolean noException = true;
        if (!new File(oldChecksumPathFileName).exists()) {
            return false;
        }
        try (BufferedReader fileReader = new BufferedReader(new FileReader(oldChecksumPathFileName))) {
            Map<String, String> checksumMap = new HashMap<>();
            String line;
            while ((line = fileReader.readLine()) != null) {
                String[] elems = line.split(COMMA);
                checksumMap.put(elems[0], elems[1]);
            }
            String[] jars = cleanSfClassPath.split(File.pathSeparator);
            for (int i = 0; i < jars.length; i++) {
                String[] elems = Writer.getJarToChecksumMapping(jars[i]).split(COMMA);
                String oldCS = checksumMap.get(elems[0]);
                if (!elems[1].equals(oldCS)) {
                    return false;
                }
            }
        } catch (IOException ioe) {
            noException = false;
            ioe.printStackTrace();
        }
        return noException;
    }

    private String getCleanClassPath(String cp) {
        String[] paths = cp.split(File.pathSeparator);
        StringBuilder sb = new StringBuilder();
        String classes = File.separator + Paths.get(TARGET, CLASSES);
        String testClasses = File.separator + Paths.get(TARGET, TEST_CLASSES);
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].contains(classes)
                || paths[i].contains(testClasses)
                || paths[i].contains("-SNAPSHOT.jar")) {
                continue;
            }
            if (sb.length() == 0) {
                sb.append(paths[i]);
            } else {
                sb.append(File.pathSeparator).append(paths[i]);
            }
        }
        return sb.toString();
    }
}
