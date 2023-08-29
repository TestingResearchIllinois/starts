/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.helpers.ZLCHelperMethods;
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

        if (computeImpactedMethods) {
            runImpactedMethods();
        } else {
            runChangedMethods();
        }
    }

    protected void runChangedMethods() throws MojoExecutionException {
        // Checking if the file of depedencies exists

        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_TEST_DEPS_ZLC_FILE))
                && !Files.exists(Paths.get(getArtifactsDir() + CLASSES_ZLC_FILE))) {
            MethodLevelStaticDepsBuilder.computeMethodsChecksum(loader);
            methodsCheckSum = MethodLevelStaticDepsBuilder.getMethodsCheckSum();
            changedMethods = new HashSet<>();
            newMethods = MethodLevelStaticDepsBuilder.getMethods();
            impactedTestClasses = MethodLevelStaticDepsBuilder.getTests();
            newClasses = MethodLevelStaticDepsBuilder.getClasses();
            changedClasses = new HashSet<>();
            oldClasses = new HashSet<>();

            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "NewMethods: " + newMethods.size());
            logger.log(Level.INFO, "ImpactedTestClasses: " + impactedTestClasses.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            logger.log(Level.INFO, "NewClasses: " + newClasses.size());
            logger.log(Level.INFO, "OldClasses: " + oldClasses.size());

            if (updateMethodsChecksums) {

                ZLCHelperMethods.writeZLCFileHybrid(method2testClasses, methodsCheckSum, classesChecksum, loader,
                        getArtifactsDir(), null, false,
                        zlcFormat);
            }
        } else {
            setChangedAndNonaffectedMethods();
            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "NewMethods: " + newMethods.size());
            logger.log(Level.INFO, "ImpactedTestClasses: " + impactedTestClasses.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            logger.log(Level.INFO, "NewClasses: " + newClasses.size());
            logger.log(Level.INFO, "OldClasses: " + oldClasses.size());
            if (updateMethodsChecksums) {

                ZLCHelperMethods.writeZLCFileHybrid(method2testClasses, methodsCheckSum, classesChecksum, loader,
                        getArtifactsDir(), null, false,
                        zlcFormat);
            }
        }
    }

    protected void runImpactedMethods() throws MojoExecutionException {
        // Checking if the file of depedencies exists

        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_TEST_DEPS_ZLC_FILE))
                && !Files.exists(Paths.get(getArtifactsDir() + CLASSES_ZLC_FILE))) {
            MethodLevelStaticDepsBuilder.computeMethodsChecksum(loader);
            methodsCheckSum = MethodLevelStaticDepsBuilder.getMethodsCheckSum();
            changedMethods = new HashSet<>();
            newMethods = MethodLevelStaticDepsBuilder.getMethods();
            impactedMethods = newMethods;

            impactedTestClasses = MethodLevelStaticDepsBuilder.getTests();
            newClasses = MethodLevelStaticDepsBuilder.getClasses();
            changedClasses = new HashSet<>();
            oldClasses = new HashSet<>();

            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "NewMethods: " + newMethods.size());
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods.size());
            logger.log(Level.INFO, "ImpactedTestClasses: " + impactedTestClasses.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            logger.log(Level.INFO, "NewClasses: " + newClasses.size());
            logger.log(Level.INFO, "OldClasses: " + oldClasses.size());

            if (updateMethodsChecksums) {

                ZLCHelperMethods.writeZLCFileHybrid(method2testClasses, methodsCheckSum, classesChecksum, loader,
                        getArtifactsDir(), null, false,
                        zlcFormat);
            }
        } else {
            setChangedAndNonaffectedMethods();
            computeImpacedMethods();
            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "NewMethods: " + newMethods.size());
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods.size());
            logger.log(Level.INFO, "ImpactedTestClasses: " + impactedTestClasses.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            logger.log(Level.INFO, "NewClasses: " + newClasses.size());
            logger.log(Level.INFO, "OldClasses: " + oldClasses.size());
            if (updateMethodsChecksums) {

                ZLCHelperMethods.writeZLCFileHybrid(method2testClasses, methodsCheckSum, classesChecksum, loader,
                        getArtifactsDir(), null, false,
                        zlcFormat);
            }
        }
    }

    protected void setChangedAndNonaffectedMethods() throws MojoExecutionException {
        List<Set<String>> data = ZLCHelperMethods.getChangedDataHybrid(loader, getArtifactsDir(), cleanBytes,
                classesChecksum, METHODS_TEST_DEPS_ZLC_FILE, CLASSES_ZLC_FILE);
        changedMethods = data == null ? new HashSet<String>() : data.get(0);
        newMethods = data == null ? new HashSet<String>() : data.get(1);
        impactedTestClasses = data == null ? new HashSet<String>() : data.get(2);
        for (String newMethod : newMethods) {
            impactedTestClasses.addAll(method2testClasses.getOrDefault(newMethod, new HashSet<>()));
        }

        changedClasses = data == null ? new HashSet<String>() : data.get(3);
        newClasses = data == null ? new HashSet<String>() : data.get(4);
        oldClasses = data == null ? new HashSet<String>() : data.get(5);
        methodsCheckSum = MethodLevelStaticDepsBuilder.getMethodsCheckSum();
    }

    private void computeImpacedMethods() {
        impactedMethods = new HashSet<>();
        impactedMethods.addAll(findImpactedMethods(changedMethods));
        impactedMethods.addAll(findImpactedMethods(newMethods));
        for (String impactedMethod : impactedMethods) {
            impactedTestClasses.addAll(method2testClasses.getOrDefault(impactedMethod, new HashSet<String>()));
        }
    }

    private Set<String> findImpactedMethods(Set<String> affectedMethods) {
        Set<String> methods = new HashSet<>(affectedMethods);
        for (String method : affectedMethods) {
            methods.addAll(MethodLevelStaticDepsBuilder.getMethodDeps(method));

        }
        return methods;
    }

}