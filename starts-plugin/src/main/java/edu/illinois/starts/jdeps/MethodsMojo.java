/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.helpers.ZLCHelperMethods;
import edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder;
import edu.illinois.starts.util.ChecksumUtil;
import edu.illinois.starts.util.Logger;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.booter.Classpath;

@Mojo(name = "methods", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class MethodsMojo extends DiffMojo {

    private Logger logger;
    private Set<String> changedMethods;
    private Set<String> newMethods;
    private Set<String> impactedMethods;
    private Set<String> newClasses;
    private Set<String> oldClasses;
    private Set<String> changedClasses;
    private Set<String> impactedTestClasses;
    private Set<String> nonImpactedTestClasses;
    private Set<String> nonAffectedMethods;
    private Map<String, String> methodsCheckSum;
    private Map<String, Set<String>> method2testClasses;
    private ClassLoader loader;

    /**
     * Set this to "true" to compute impaced methods as well. False indicated only
     * changed methods will be compute.
     */
    @Parameter(property = "computeImpactedMethods", defaultValue = TRUE)
    private boolean computeImpactedMethods;

    @Parameter(property = "updateMethodsChecksums", defaultValue = TRUE)
    private boolean updateMethodsChecksums;

    public void setUpdateMethodsChecksums(boolean updateChecksums) {
        this.updateMethodsChecksums = updateChecksums;
    }

    public void setComputeImpactedMethods(boolean computeImpactedMethods) {
        this.computeImpactedMethods = computeImpactedMethods;
    }

    public Set<String> getAffectedMethods() {
        Set<String> affectedMethods = new HashSet<>();
        affectedMethods.addAll(changedMethods);
        affectedMethods.addAll(newMethods);
        return Collections.unmodifiableSet(affectedMethods);
    }

    public Set<String> getImpactedMethods() {
        return Collections.unmodifiableSet(impactedMethods);
    }

    public Set<String> getNewClasses() {
        return Collections.unmodifiableSet(newClasses);
    }

    public Set<String> getOldClasses() {
        return Collections.unmodifiableSet(oldClasses);
    }

    public Set<String> getChangedClasses() throws MojoExecutionException {
        Set<String> changedC = new HashSet<>();
        for (String c : changedClasses) {

            URL url = loader.getResource(ChecksumUtil.toClassName(c));
            String extForm = url.toExternalForm();
            changedC.add(extForm);
        }
        return Collections.unmodifiableSet(changedC);
    }

    public Set<String> getNonAffectedMethods() {
        return Collections.unmodifiableSet(nonAffectedMethods);
    }

    /*
     * It first creates the graphs by calling buildMethodsGraph()
     * Then it computes and gets methodsCheckSums, method2testClasses
     */
    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        Classpath sfClassPath = getSureFireClassPath();
        loader = createClassLoader(sfClassPath);

        // Build method level static dependencies
        try {
            MethodLevelStaticDepsBuilder.buildMethodsGraph();
            methodsCheckSum = MethodLevelStaticDepsBuilder.computeMethodsChecksum(loader);
            method2testClasses = MethodLevelStaticDepsBuilder.computeMethod2testClasses();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        runMethods(computeImpactedMethods);
    }

    protected void runMethods(boolean impacted) throws MojoExecutionException {
        // Checking if the file of depedencies exists
        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_TEST_DEPS_ZLC_FILE))) {
            changedMethods = new HashSet<>();
            newMethods = MethodLevelStaticDepsBuilder.getMethods();
            impactedTestClasses = MethodLevelStaticDepsBuilder.getTests();
            oldClasses = new HashSet<>();
            changedClasses = new HashSet<>();
            newClasses = MethodLevelStaticDepsBuilder.getClasses();
            nonAffectedMethods = new HashSet<>();

            if (impacted) {
                impactedMethods = newMethods;
            }

            if (updateMethodsChecksums) {
                ZLCHelperMethods.writeZLCFile(method2testClasses, methodsCheckSum, null, loader, getArtifactsDir(),
                        null,
                        false,
                        zlcFormat, false);
            }
        } else {
            setChangedMethods();

            if (impacted) {
                computeImpacedMethods();
                computeImpacedTestClasses();
            }

            if (updateMethodsChecksums) {
                ZLCHelperMethods.writeZLCFile(method2testClasses, methodsCheckSum, null, loader, getArtifactsDir(),
                        null,
                        false,
                        zlcFormat, false);
            }
        }

        logInfoStatements(impacted);
    }

    private void logInfoStatements(boolean impacted) {
        logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
        logger.log(Level.INFO, "NewMethods: " + newMethods.size());
        logger.log(Level.INFO, "ImpactedTestClasses: " + impactedTestClasses.size());
        logger.log(Level.INFO, "NewClasses: " + newClasses.size());
        logger.log(Level.INFO, "OldClasses: " + oldClasses.size());
        logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());

        if (impacted) {
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods.size());
        }
    }

    protected void setChangedMethods() throws MojoExecutionException {
        List<Set<String>> data = ZLCHelperMethods.getChangedDataMethods(getArtifactsDir(), cleanBytes, methodsCheckSum,
                METHODS_TEST_DEPS_ZLC_FILE);
        changedMethods = data == null ? new HashSet<String>() : data.get(0);
        newMethods = data == null ? new HashSet<String>() : data.get(1);

        impactedTestClasses = data == null ? new HashSet<String>() : data.get(2);
        for (String newMethod : newMethods) {
            impactedTestClasses.addAll(method2testClasses.getOrDefault(newMethod, new HashSet<>()));
        }

        oldClasses = data == null ? new HashSet<String>() : data.get(3);
        changedClasses = data == null ? new HashSet<String>() : data.get(4);
        newClasses = MethodLevelStaticDepsBuilder.getClasses();
        newClasses.removeAll(oldClasses);
        // nonAffectedTestClasses = MethodLevelStaticDepsBuilder.getTests();
        // nonAffectedTestClasses.removeAll(affectedTestClasses);
        nonAffectedMethods = MethodLevelStaticDepsBuilder.getMethods();
        nonAffectedMethods.removeAll(changedMethods);
        nonAffectedMethods.removeAll(newMethods);
    }

    private void computeImpacedTestClasses() {
        for (String impactedMethod : impactedMethods) {
            impactedTestClasses.addAll(method2testClasses.getOrDefault(impactedMethod, new HashSet<>()));

        }
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
