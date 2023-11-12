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

@Mojo(name = "hybrid", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class HybridMojo extends DiffMojo {

    private Logger logger;
    private Set<String> changedMethods;
    private Set<String> newMethods;
    private Set<String> impactedMethods;
    private Set<String> newClasses;
    private Set<String> oldClasses;
    private Set<String> affectedTestClasses;
    private Set<String> nonAffectedMethods;
    private Set<String> nonAffectedClasses;
    private Map<String, String> methodsCheckSum;
    private Map<String, List<String>> classesChecksum;
    private Map<String, Set<String>> methodToTestClasses;
    private ClassLoader loader;
    private Map<String, Set<String>> classDependencyGraph;
    private Map<String, Set<String>> classToTestClassGraph;
    private Set<String> deletedClasses;
    private Set<String> changedClassesWithChangedHeaders;
    private Set<String> changedClassesWithoutChangedHeaders;
    private Set<String> impactedClasses;

    @Parameter(property = "computeImpactedMethods", defaultValue = TRUE)
    private boolean computeImpactedMethods;

    @Parameter(property = "updateMethodsChecksums", defaultValue = TRUE)
    private boolean updateMethodsChecksums;

    @Parameter(property = "includeVariables", defaultValue = FALSE)
    private boolean includeVariables;

    @Parameter(property = "debug", defaultValue = TRUE)
    private boolean debug;

    /**
     * Set this to "true" to compute affected test classes as well.
     */
    @Parameter(property = "computeAffectedTests", defaultValue = FALSE)
    private boolean computeAffectedTests;

    /*
     * Set this to "true" to include non-affected classes as well.
     */
    @Parameter(property = "includeNonAffectedClasses", required = false, defaultValue = "true")
    private boolean includeNonAffectedClasses;

    public void setComputeImpactedMethods(boolean computeImpactedMethods) {
        this.computeImpactedMethods = computeImpactedMethods;
    }

    public void setUpdateMethodsChecksums(boolean updateChecksums) {
        this.updateMethodsChecksums = updateChecksums;
    }

    public void setComputeAffectedTests(boolean computeAffectedTests) {
        this.computeAffectedTests = computeAffectedTests;
    }

    public void setIncludeNonAffectedClasses(boolean includeNonAffectedClasses) {
        this.includeNonAffectedClasses = includeNonAffectedClasses;
    }

    public Set<String> getAffectedMethods() {
        Set<String> affectedMethods = new HashSet<>();
        affectedMethods.addAll(changedMethods);
        affectedMethods.addAll(newMethods);
        return Collections.unmodifiableSet(affectedMethods);
    }

    public Set<String> getAffectedClasses() {
        Set<String> affectedClasses = new HashSet<>();
        affectedClasses.addAll(changedClassesWithChangedHeaders);
        affectedClasses.addAll(newClasses);
        return Collections.unmodifiableSet(affectedClasses);
    }

    public Set<String> getImpactedMethods() {
        return Collections.unmodifiableSet(impactedMethods);
    }

    public Set<String> getImpactedClasses() {
        return Collections.unmodifiableSet(impactedClasses);
    }

    public Set<String> getNewClasses() {
        return Collections.unmodifiableSet(newClasses);
    }

    public Set<String> getOldClasses() {
        return Collections.unmodifiableSet(oldClasses);
    }

    public Set<String> getChangedClassesWithChangedHeaders() throws MojoExecutionException {
        Set<String> changedC = new HashSet<>();
        for (String c : changedClassesWithChangedHeaders) {
            URL url = loader.getResource(ChecksumUtil.toClassOrJavaName(c, false));
            String extForm = url.toExternalForm();
            changedC.add(extForm);
        }
        return Collections.unmodifiableSet(changedC);
    }

    public Set<String> getChangedClassesWithoutChangedHeaders() throws MojoExecutionException {
        Set<String> changedC = new HashSet<>();
        for (String c : changedClassesWithoutChangedHeaders) {
            URL url = loader.getResource(ChecksumUtil.toClassOrJavaName(c, false));
            String extForm = url.toExternalForm();
            changedC.add(extForm);
        }
        return Collections.unmodifiableSet(changedC);
    }

    public Set<String> getChangedClasses() throws MojoExecutionException {
        Set<String> changedC = new HashSet<>();
        for (String c : changedClassesWithoutChangedHeaders) {
            URL url = loader.getResource(ChecksumUtil.toClassOrJavaName(c, false));
            String extForm = url.toExternalForm();
            changedC.add(extForm);
        }
        for (String c : changedClassesWithChangedHeaders) {
            URL url = loader.getResource(ChecksumUtil.toClassOrJavaName(c, false));
            String extForm = url.toExternalForm();
            changedC.add(extForm);
        }

        return Collections.unmodifiableSet(changedC);
    }

    public Set<String> getNonAffectedMethods() {
        return Collections.unmodifiableSet(nonAffectedMethods);
    }

    public Set<String> getNonAffectedClasses() {
        return Collections.unmodifiableSet(nonAffectedClasses);
    }

    /**
     * This method first builds the method-level static dependencies by calling
     * MethodLevelStaticDepsBuilder.buildMethodsGraph().
     * Then, it computes and retrieves the classes' checksums and the mapping
     * between methods and test classes by calling
     * MethodLevelStaticDepsBuilder.computeMethodsChecksum(ClassLoader) and
     * MethodLevelStaticDepsBuilder.computeMethod2testClasses() respectively.
     * Finally, it computes the changed (and impacted) methods through changed
     * classes by calling runMethods(boolean).
     */
    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        Classpath sfClassPath = getSureFireClassPath();
        loader = createClassLoader(sfClassPath);

        // Build method level static dependencies
        try {
            MethodLevelStaticDepsBuilder.buildMethodsGraph(includeVariables);

            classesChecksum = MethodLevelStaticDepsBuilder.computeClassesChecksums(loader, cleanBytes);
            if (computeAffectedTests) {
                methodToTestClasses = MethodLevelStaticDepsBuilder.computeMethodToTestClasses();
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        runHybrid(computeImpactedMethods);
    }

    /**
     * This method handles the main logic of the mojo for hybrid analysis.
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
    protected void runHybrid(boolean impacted) throws MojoExecutionException {
        // Checking if the file of dependencies exists (first run)
        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_CHECKSUMS_SERIALIZED_FILE))
                && !Files.exists(Paths.get(getArtifactsDir() + CLASSES_CHECKSUM_SERIALIZED_FILE))) {
            // In the first run we compute all method checksums and save them.
            // In later runs we just compute new method checksums for changed classes
            MethodLevelStaticDepsBuilder.computeMethodsChecksum(loader);
            methodsCheckSum = MethodLevelStaticDepsBuilder.getMethodsCheckSum();
            changedMethods = new HashSet<>();
            newMethods = MethodLevelStaticDepsBuilder.computeMethods();
            newClasses = MethodLevelStaticDepsBuilder.getClasses();
            oldClasses = new HashSet<>();
            deletedClasses = new HashSet<>();
            changedClassesWithChangedHeaders = new HashSet<>();
            changedClassesWithoutChangedHeaders = new HashSet<>();
            nonAffectedMethods = new HashSet<>();
            nonAffectedClasses = new HashSet<>();

            if (computeAffectedTests) {
                affectedTestClasses = MethodLevelStaticDepsBuilder.computeTestClasses();
            }

            if (impacted) {
                impactedMethods = newMethods;
                impactedClasses = newClasses;
            }

            if (updateMethodsChecksums) {
                try {
                    // Save class-to-checksums mapping
                    ZLCHelperMethods.serializeMapping(classesChecksum, getArtifactsDir(),
                            CLASSES_CHECKSUM_SERIALIZED_FILE);
                    // The method-to-checksum mapping has been updated in
                    // ZLCHelperMethods.getChangedDataHybrid()
                    ZLCHelperMethods.serializeMapping(methodsCheckSum, getArtifactsDir(),
                            METHODS_CHECKSUMS_SERIALIZED_FILE);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        } else {

            classDependencyGraph = MethodLevelStaticDepsBuilder.constructClassesDependencyGraph();
            MethodLevelStaticDepsBuilder.constuctTestClassesToClassesGraph();
            if (computeAffectedTests) {
                classToTestClassGraph = MethodLevelStaticDepsBuilder.constructClassesToTestClassesGraph();
                methodToTestClasses = MethodLevelStaticDepsBuilder.computeMethodToTestClasses();
                affectedTestClasses = new HashSet<>();
            }

            setChangedAndNonaffectedMethods();

            if (impacted) {
                computeImpactedMethods();
                computeImpactedClasses();
            }

            if (updateMethodsChecksums) {
                try {
                    // Save class-to-checksums mapping
                    ZLCHelperMethods.serializeMapping(classesChecksum, getArtifactsDir(),
                            CLASSES_CHECKSUM_SERIALIZED_FILE);
                    // Save method-to-checksum mapping
                    ZLCHelperMethods.serializeMapping(methodsCheckSum, getArtifactsDir(),
                            METHODS_CHECKSUMS_SERIALIZED_FILE);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }

        }

        logInfo(impacted);
    }

    /**
     * This method logs information statements about changed methods, new methods,
     * impacted test classes, new classes, old classes and changed classes.
     * If impacted is true, it also logs information about impacted methods.
     *
     * @param impacted a boolean value indicating whether to log information about
     *                 impacted methods
     */
    private void logInfo(boolean impacted) {
        logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
        logger.log(Level.INFO, "NewMethods: " + newMethods.size());

        if (impacted) {
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods.size());
        }

        logger.log(Level.INFO, "NewClasses: " + newClasses.size());
        logger.log(Level.INFO, "OldClasses: " + oldClasses.size());
        logger.log(Level.INFO, "DeletedClasses: " + deletedClasses.size());
        logger.log(Level.INFO, "ChangedClassesWithChangedHeaders: " + changedClassesWithChangedHeaders.size());
        logger.log(Level.INFO, "ChangedClassesWithoutChangedHeaders: " + changedClassesWithoutChangedHeaders.size());
        if (computeAffectedTests) {
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses.size());
        }

        if (impacted) {
            logger.log(Level.INFO, "ImpactedClasses: " + impactedClasses.size());
        }

        if (includeNonAffectedClasses) {
            logger.log(Level.INFO, "NonAffectedClasses: " + nonAffectedClasses.size());
        }

        // DEBUG PRINTS
        if (debug) {
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods);
            logger.log(Level.INFO, "ImpactedClasses: " + impactedClasses);
            logger.log(Level.INFO, "ClassDependencyGraph: " + classDependencyGraph);
            logger.log(Level.INFO, "ChangedClassesWithChangedHeaders: " + changedClassesWithChangedHeaders);
            logger.log(Level.INFO, "ChangedClassesWithoutChangedHeaders: " + changedClassesWithoutChangedHeaders);
            if (computeAffectedTests) {
                logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses);
                logger.log(Level.INFO, "ClassToTestClassGraph: " + classToTestClassGraph);
            }
            if (includeNonAffectedClasses) {
                logger.log(Level.INFO, "NonAffectedClasses: " + nonAffectedClasses);
            }
        }
    }

    /**
     * Sets the changed and non-affected methods by retrieving changed data using
     * the ZLCHelperMethods class and updating the relevant fields.
     * This method also updates the impacted test classes by adding test classes
     * associated with new methods.
     */
    protected void setChangedAndNonaffectedMethods() throws MojoExecutionException {
        List<Set<String>> classesData = ZLCHelperMethods.getChangedDataHybridClassLevel(classesChecksum,
                getArtifactsDir(), CLASSES_CHECKSUM_SERIALIZED_FILE);

        newClasses = classesData == null ? new HashSet<String>() : classesData.get(0);
        deletedClasses = classesData == null ? new HashSet<String>() : classesData.get(1);
        changedClassesWithChangedHeaders = classesData == null ? new HashSet<String>() : classesData.get(2);
        changedClassesWithoutChangedHeaders = classesData == null ? new HashSet<String>() : classesData.get(3);
        oldClasses = classesData == null ? new HashSet<String>() : classesData.get(4);

        List<Set<String>> methodsData = ZLCHelperMethods.getChangedDataHybridMethodLevel(newClasses, deletedClasses,
                changedClassesWithChangedHeaders,
                changedClassesWithoutChangedHeaders, MethodLevelStaticDepsBuilder.getMethodsCheckSum(), loader,
                getArtifactsDir(), METHODS_CHECKSUMS_SERIALIZED_FILE);

        methodsCheckSum = MethodLevelStaticDepsBuilder.getMethodsCheckSum();

        changedMethods = methodsData == null ? new HashSet<String>() : methodsData.get(0);
        newMethods = methodsData == null ? new HashSet<String>() : methodsData.get(1);

        if (computeAffectedTests) {
            for (String newMethod : newMethods) {
                affectedTestClasses.addAll(methodToTestClasses.getOrDefault(newMethod, new HashSet<>()));
            }

            for (String changedMethod : changedMethods) {
                affectedTestClasses.addAll(methodToTestClasses.getOrDefault(changedMethod, new HashSet<>()));
            }

            for (String addedClass : newClasses) {
                affectedTestClasses.addAll(classToTestClassGraph.getOrDefault(addedClass, new HashSet<>()));
            }

            for (String changedClassesWithChangedHeader : changedClassesWithChangedHeaders) {
                affectedTestClasses
                        .addAll(classToTestClassGraph.getOrDefault(changedClassesWithChangedHeader, new HashSet<>()));
            }
        }

        if (includeNonAffectedClasses) {
            nonAffectedClasses = MethodLevelStaticDepsBuilder.getClasses();
            nonAffectedClasses.removeAll(newClasses);
            nonAffectedClasses.removeAll(deletedClasses);
            nonAffectedClasses.removeAll(changedClassesWithChangedHeaders);
            nonAffectedClasses.removeAll(changedClassesWithoutChangedHeaders);
        }
    }

    /**
     * Computes the impacted methods by finding impacted methods for changed and new
     * methods (called affected methods), and updating the impacted test classes by
     * adding test classes found from impacted methods.
     */
    private void computeImpactedMethods() {
        impactedMethods = new HashSet<>();
        impactedMethods.addAll(findImpactedMethods(changedMethods));
        impactedMethods.addAll(findImpactedMethods(newMethods));
        if (computeAffectedTests) {
            for (String impactedMethod : impactedMethods) {
                affectedTestClasses.addAll(methodToTestClasses.getOrDefault(impactedMethod, new HashSet<String>()));
            }
        }
    }

    /**
     * Computes the impacted classes by finding impacted classes for new and
     * changedClassesWithChangedHeaders
     * methods (called affected methods), and updating the impacted test classes by
     * adding test classes found from impacted methods.
     */
    private void computeImpactedClasses() {
        impactedClasses = new HashSet<>();
        impactedClasses.addAll(findImpactedClasses(newClasses));
        impactedClasses.addAll(findImpactedClasses(changedClassesWithChangedHeaders));
        if (computeAffectedTests) {
            for (String impactedClass : impactedClasses) {
                affectedTestClasses.addAll(classToTestClassGraph.getOrDefault(impactedClass, new HashSet<String>()));
            }
        }
    }

    private Set<String> findImpactedClasses(Set<String> affectedClasses) {
        Set<String> classes = new HashSet<>(affectedClasses);
        for (String clazz : affectedClasses) {
            classes.addAll(MethodLevelStaticDepsBuilder.getClassDeps(clazz));
        }
        return classes;
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
