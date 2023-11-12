/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.io.IOException;
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
    private Set<String> affectedTestClasses;
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

    /**
     * Set this to "true" to include variables in the method-level static
     * dependencies.
     */
    @Parameter(property = "includeVariables", defaultValue = FALSE)
    private boolean includeVariables;

    /**
     * Set this to "true" to print debug statements.
     */
    @Parameter(property = "debug", defaultValue = FALSE)
    private boolean debug;

    /**
     * Set this to "true" to compute affected test classes as well.
     */
    @Parameter(property = "computeAffectedTests", defaultValue = FALSE)
    private boolean computeAffectedTests;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setUpdateMethodsChecksums(boolean updateChecksums) {
        this.updateMethodsChecksums = updateChecksums;
    }

    public void setComputeImpactedMethods(boolean computeImpactedMethods) {
        this.computeImpactedMethods = computeImpactedMethods;
    }

    public void setIncludeVariables(boolean includeVariables) {
        this.includeVariables = includeVariables;
    }

    public void setComputeAffectedTests(boolean computeAffectedTests) {
        this.computeAffectedTests = computeAffectedTests;
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

            URL url = loader.getResource(ChecksumUtil.toClassOrJavaName(c, false));
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
            MethodLevelStaticDepsBuilder.buildMethodsGraph(includeVariables);
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

        // Checking if the file of depedencies exists (first run or not)
        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_CHECKSUMS_SERIALIZED_FILE))) {
            changedMethods = new HashSet<>();
            newMethods = MethodLevelStaticDepsBuilder.computeMethods();
            oldClasses = new HashSet<>();
            changedClasses = new HashSet<>();
            newClasses = MethodLevelStaticDepsBuilder.getClasses();
            nonAffectedMethods = new HashSet<>();

            if (computeAffectedTests) {
                affectedTestClasses = MethodLevelStaticDepsBuilder.computeTestClasses();
            }

            if (impacted) {
                impactedMethods = newMethods;
            }

            // Always save the checksums in the first run
            try {
                ZLCHelperMethods.serializeMapping(methodsCheckSum, getArtifactsDir(),
                        METHODS_CHECKSUMS_SERIALIZED_FILE);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

        } else {
            // First run has saved the old revision's checksums. Time to find changes.
            computeChangedMethods();

            if (impacted) {
                computeImpactedMethods();

            }
            if (computeAffectedTests) {
                computeAffectedTestClasses();
            }

            if (updateMethodsChecksums) {
                try {
                    ZLCHelperMethods.serializeMapping(methodsCheckSum, getArtifactsDir(),
                            METHODS_CHECKSUMS_SERIALIZED_FILE);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
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

        if (impacted) {
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods.size());
        }

        logger.log(Level.INFO, "NewClasses: " + newClasses.size());
        logger.log(Level.INFO, "OldClasses: " + oldClasses.size());
        logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());

        if (computeAffectedTests) {
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses.size());
        }

        // DEBUG PRINTS
        if (debug) {
            logger.log(Level.INFO, "ChangedMethods: " + changedMethods);
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods);
            if (computeAffectedTests) {
                logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses);
            }
        }
    }

    /**
     * This method sets the changed methods by getting the list of sets for changed
     * methods, new methods, impacted test classes, old classes and changed classes
     * accordingly.
     */
    protected void computeChangedMethods() throws MojoExecutionException {

        List<Set<String>> dataList = ZLCHelperMethods.getChangedDataMethods(methodsCheckSum,
                methodToTestClasses, getArtifactsDir(), METHODS_CHECKSUMS_SERIALIZED_FILE);

        changedMethods = dataList == null ? new HashSet<String>() : dataList.get(0);
        newMethods = dataList == null ? new HashSet<String>() : dataList.get(1);

        oldClasses = dataList == null ? new HashSet<String>() : dataList.get(3);
        changedClasses = dataList == null ? new HashSet<String>() : dataList.get(4);
        newClasses = MethodLevelStaticDepsBuilder.getClasses();
        newClasses.removeAll(oldClasses);

        if (computeAffectedTests) {
            affectedTestClasses = dataList == null ? new HashSet<String>() : dataList.get(2);
            for (String newMethod : newMethods) {
                affectedTestClasses.addAll(methodToTestClasses.getOrDefault(newMethod, new HashSet<>()));
            }
        }

        // nonAffectedMethods = MethodLevelStaticDepsBuilder.computeMethods();
        // nonAffectedMethods.removeAll(changedMethods);
        // nonAffectedMethods.removeAll(newMethods);
    }

    /**
     * This method computes the impacted test classes by adding all test classes
     * associated with each impacted method to the set of impacted test classes.
     */
    private void computeAffectedTestClasses() {
        for (String impactedMethod : impactedMethods) {
            affectedTestClasses.addAll(methodToTestClasses.getOrDefault(impactedMethod, new HashSet<>()));

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
