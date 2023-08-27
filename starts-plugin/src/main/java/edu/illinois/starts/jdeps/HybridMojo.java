/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelperMethods;
import edu.illinois.starts.maven.AgentLoader;
import edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder;
import edu.illinois.starts.util.Logger;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.booter.Classpath;

@Mojo(name = "hybrid", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class HybridMojo extends DiffMojo {

    private Logger logger;
    private Set<String> changedMethods;
    private Set<String> newMethods;
    private Set<String> impactedMethods;
    private Set<String> newClasses;
    private Set<String> oldClasses;
    private Set<String> changedClasses;
    private Set<String> impactedTestClasses;
    private Set<String> affectedTestClasses;
    private Set<String> nonAffectedTestClasses;
    private Map<String, String> methodsCheckSum;
    private Map<String, String> classesChecksum;
    private Map<String, Set<String>> method2testClasses;
    private ClassLoader loader;

    @Parameter(property = "computeImpactedMethods", defaultValue = TRUE)
    private boolean computeImpactedMethods;

    @Parameter(property = "updateMethodsChecksums", defaultValue = TRUE)
    private boolean updateMethodsChecksums;

    public void setComputeImpactedMethods(boolean computeImpactedMethods) {
        this.computeImpactedMethods = computeImpactedMethods;
    }

    public void setUpdateMethodsChecksums(boolean updateChecksums) {
        this.updateMethodsChecksums = updateChecksums;
    }

    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        Classpath sfClassPath = getSureFireClassPath();
        loader = createClassLoader(sfClassPath);

        // Build method level static dependencies
        try {
            MethodLevelStaticDepsBuilder.buildMethodsGraph();
            method2testClasses = MethodLevelStaticDepsBuilder.computeMethod2testClasses();
            classesChecksum = MethodLevelStaticDepsBuilder.computeClassesChecksums(loader, cleanBytes);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        runMethods(loader);
    }

    protected void runMethods(ClassLoader loader) throws MojoExecutionException {
        // Checking if the file of depedencies exists

        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_TEST_DEPS_ZLC_FILE))
                && !Files.exists(Paths.get(getArtifactsDir() + CLASSES_ZLC_FILE))) {
            MethodLevelStaticDepsBuilder.computeMethodsChecksum(loader);
            methodsCheckSum = MethodLevelStaticDepsBuilder.getMethodsCheckSum();
            changedMethods = MethodLevelStaticDepsBuilder.getMethods();
            affectedTestClasses = MethodLevelStaticDepsBuilder.getTests();
            changedClasses = MethodLevelStaticDepsBuilder.getClasses();

            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses.size());
            ZLCHelperMethods.writeZLCFileHybrid(method2testClasses, methodsCheckSum, classesChecksum, loader,
                    getArtifactsDir(), null, false,
                    zlcFormat);
            dynamicallyUpdateExcludes(new ArrayList<String>());

        } else {
            setChangedAndNonaffectedMethods(loader);
            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses.size());
            ZLCHelperMethods.writeZLCFileHybrid(method2testClasses, methodsCheckSum, classesChecksum, loader,
                    getArtifactsDir(), null, false,
                    zlcFormat);
            List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTestClasses);
            dynamicallyUpdateExcludes(excludePaths);
        }
    }

    protected void setChangedAndNonaffectedMethods(ClassLoader loader) throws MojoExecutionException {
        List<Set<String>> data = ZLCHelperMethods.getChangedDataH(loader, getArtifactsDir(), cleanBytes,
                classesChecksum, METHODS_TEST_DEPS_ZLC_FILE, CLASSES_ZLC_FILE);
        methodsCheckSum = MethodLevelStaticDepsBuilder.getMethodsCheckSum();
        changedClasses = data == null ? new HashSet<String>() : data.get(0);
        changedMethods = data == null ? new HashSet<String>() : data.get(1);
        affectedTestClasses = data == null ? new HashSet<String>() : data.get(2);
        nonAffectedTestClasses = MethodLevelStaticDepsBuilder.getTests();
        nonAffectedTestClasses.removeAll(affectedTestClasses);
    }

    private void dynamicallyUpdateExcludes(List<String> excludePaths) throws MojoExecutionException {
        if (AgentLoader.loadDynamicAgent()) {
            logger.log(Level.FINEST, "AGENT LOADED!!!");
            System.setProperty(STARTS_EXCLUDE_PROPERTY, Arrays.toString(excludePaths.toArray(new String[0])));
        } else {
            throw new MojoExecutionException("I COULD NOT ATTACH THE AGENT");
        }
    }

}