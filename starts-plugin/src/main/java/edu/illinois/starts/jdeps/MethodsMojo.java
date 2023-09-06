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
    private Set<String> nonImpactedTestClasses; // This may not be needed at all
    private Set<String> nonAffectedMethods; // This may not be needed at all
    private Map<String, String> methodsCheckSum;
    private Map<String, Set<String>> methodToTestClasses;
    private ClassLoader loader;

    /**
     * Set this to "true" to compute impacted methods as well. False indicates only
     * changed methods will be computed.
     */
    @Parameter(property = "computeImpactedMethods", defaultValue = TRUE)
    private boolean computeImpactedMethods;

    /**
     * Set this to "true" to save the new checksums of changed methods in the zlc
     * file.
     */
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

    /**
     * This method first builds the method-level static dependencies by calling
     * MethodLevelStaticDepsBuilder.buildMethodsGraph().
     * Then, it computes and retrieves the methods' checksums and the mapping
     * between methods and test classes by calling
     * MethodLevelStaticDepsBuilder.computeMethodsChecksum(ClassLoader) and
     * MethodLevelStaticDepsBuilder.computeMethod2testClasses() respectively.
     * Finally, it computes the changed (and impacted) methods by calling
     * runMethods(boolean).
     */
    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        Classpath sfClassPath = getSureFireClassPath();
        loader = createClassLoader(sfClassPath);

        // Build method level static dependencies
        try {
            MethodLevelStaticDepsBuilder.buildMethodsGraph();
            methodToTestClasses = MethodLevelStaticDepsBuilder.computeMethodToTestClasses();
            methodsCheckSum = MethodLevelStaticDepsBuilder.computeMethodsChecksum(loader);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        runMethods(computeImpactedMethods);
    }

    /**
     * This method handles the main logic of the mojo for method-level analysis.
     * It checks if the file of dependencies exists and sets the changed
     * methods accordingly. (First run doesn't have the file of dependencies)
     * If the file does not exist, it sets the changed methods, new methods,
     * impacted test classes, old classes, changed classes, new classes and
     * non-affected methods.
     * If the file exists, it sets the changed methods and computes the impacted
     * methods and impacted test classes if impacted is true.
     * It also updates the methods checksums in the dependency file if
     * updateMethodsChecksums is true.
     *
     * @param impacted a boolean value indicating whether to compute impacted
     *                 methods and impacted test classes
     * @throws MojoExecutionException if an exception occurs while setting changed
     *                                methods
     */
    protected void runMethods(boolean impacted) throws MojoExecutionException {
        // Checking if the file of depedencies exists
        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_TEST_DEPS_ZLC_FILE))) {
            changedMethods = new HashSet<>();
            newMethods = MethodLevelStaticDepsBuilder.computeMethods();
            impactedTestClasses = MethodLevelStaticDepsBuilder.computeTestClasses();
            oldClasses = new HashSet<>();
            changedClasses = new HashSet<>();
            newClasses = MethodLevelStaticDepsBuilder.getClasses();
            nonAffectedMethods = new HashSet<>();

            if (impacted) {
                impactedMethods = newMethods;
            }

            if (updateMethodsChecksums) {
                ZLCHelperMethods.writeZLCFile(methodToTestClasses, methodsCheckSum, null, loader, getArtifactsDir(),
                        null,
                        false,
                        zlcFormat, false);
            }
        } else {
            setChangedMethods();

            if (impacted) {
                computeImpactedMethods();
                computeImpactedTestClasses();
            }

            if (updateMethodsChecksums) {
                ZLCHelperMethods.writeZLCFile(methodToTestClasses, methodsCheckSum, null, loader, getArtifactsDir(),
                        null,
                        false,
                        zlcFormat, false);
            }
        }

        logInfoStatements(impacted);
    }

    /**
     * This method logs information statements about changed methods, new methods,
     * impacted test classes, new classes, old classes and changed classes.
     * If impacted is true, it also logs information about impacted methods.
     *
     * @param impacted a boolean value indicating whether to log information about
     *                 impacted methods
     */
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

    /**
     * This method sets the changed methods by getting the list of sets for changed
     * methods, new methods, impacted test classes, old classes and changed classes
     * accordingly.
     */
    protected void setChangedMethods() throws MojoExecutionException {
        List<Set<String>> data = ZLCHelperMethods.getChangedDataMethods(getArtifactsDir(), cleanBytes, methodsCheckSum,
                METHODS_TEST_DEPS_ZLC_FILE);
        changedMethods = data == null ? new HashSet<String>() : data.get(0);
        newMethods = data == null ? new HashSet<String>() : data.get(1);

        impactedTestClasses = data == null ? new HashSet<String>() : data.get(2);
        for (String newMethod : newMethods) {
            impactedTestClasses.addAll(methodToTestClasses.getOrDefault(newMethod, new HashSet<>()));
        }

        oldClasses = data == null ? new HashSet<String>() : data.get(3);
        changedClasses = data == null ? new HashSet<String>() : data.get(4);
        newClasses = MethodLevelStaticDepsBuilder.getClasses();
        newClasses.removeAll(oldClasses);
        // nonAffectedTestClasses = MethodLevelStaticDepsBuilder.getTests();
        // nonAffectedTestClasses.removeAll(affectedTestClasses);
        nonAffectedMethods = MethodLevelStaticDepsBuilder.computeMethods();
        nonAffectedMethods.removeAll(changedMethods);
        nonAffectedMethods.removeAll(newMethods);
    }

    /**
     * This method computes the impacted test classes by adding all test classes
     * associated with each impacted method to the set of impacted test classes.
     */
    private void computeImpactedTestClasses() {
        for (String impactedMethod : impactedMethods) {
            impactedTestClasses.addAll(methodToTestClasses.getOrDefault(impactedMethod, new HashSet<>()));

        }
    }

    /**
     * This method computes the impacted methods by finding all impacted methods
     * associated with changed methods and new methods and adding them to the set of
     * impacted methods.
     */
    private void computeImpactedMethods() {
        impactedMethods = new HashSet<>();
        impactedMethods.addAll(findImpactedMethods(changedMethods));
        impactedMethods.addAll(findImpactedMethods(newMethods));
    }

    /**
     * This method finds all impacted methods associated with a set of affected
     * methods (new methods and changed methods).
     * It adds all method dependencies of each affected method to the set of
     * impacted methods.
     * This is the method that finds the transitive closure of the affected methods.
     *
     * @param affectedMethods a set of affected methods
     * @return a set of impacted methods found from the affected methods
     */
    private Set<String> findImpactedMethods(Set<String> affectedMethods) {
        Set<String> methods = new HashSet<>(affectedMethods);
        for (String method : affectedMethods) {
            methods.addAll(MethodLevelStaticDepsBuilder.getMethodDeps(method));

        }
        return methods;
    }
}
