/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.util.*;
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
import java.nio.file.Paths;
import java.nio.file.Files;



@Mojo(name = "hybrid-impacted", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class HybridImpactedMojo extends HybridMojo {

    private Logger logger;
    private Set<String> changedMethods;
    private Set<String> impactedMethods;
    private Set<String> affectedTestClasses;
    private Set<String> nonAffectedTestClasses;
    private Set<String> changedClasses;
    private Map<String, String> methodsCheckSums;
    private Map<String, String> classesChecksums;
    private Map<String, Set<String>> method2testClasses;



    @Parameter(property = "updateMethodsChecksums", defaultValue = TRUE)
    private boolean updateMethodsChecksums;

    public void setUpdateMethodsChecksums(boolean updateChecksums) {
        this.updateMethodsChecksums = updateChecksums;
    }

    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        setIncludesExcludes();
        Classpath sfClassPath = getSureFireClassPath();
        ClassLoader loader = createClassLoader(sfClassPath);

        // Build method level static dependencies
        try {
            MethodLevelStaticDepsBuilder.buildMethodsGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        method2testClasses = MethodLevelStaticDepsBuilder.method2testClasses;
        MethodLevelStaticDepsBuilder.computeClassesChecksums(loader, cleanBytes);
        classesChecksums = MethodLevelStaticDepsBuilder.classesChecksums;

        runMethods(loader);
    }

    protected void runMethods(ClassLoader loader) throws MojoExecutionException {
        // Checking if the file of depedencies exists

        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_TEST_DEPS_ZLC_FILE)) && !Files.exists(Paths.get(getArtifactsDir() + CLASSES_ZLC_FILE))) {
            MethodLevelStaticDepsBuilder.computeMethodsChecksum(loader);
            methodsCheckSums = MethodLevelStaticDepsBuilder.getMethodsCheckSum();
            changedMethods = MethodLevelStaticDepsBuilder.getMethods();
            affectedTestClasses = MethodLevelStaticDepsBuilder.getTests();
            changedClasses = MethodLevelStaticDepsBuilder.getClasses();

            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "ImpactedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses.size());
            ZLCHelperMethods.writeZLCFileH(method2testClasses, methodsCheckSums,classesChecksums, loader, getArtifactsDir(), null, false,
                    zlcFormat);
            dynamicallyUpdateExcludes(new ArrayList<String>());

        } else {
            setChangedAndNonaffectedMethods(loader);
            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods.size());
            logger.log(Level.INFO, "ImpactedMethods: " + impactedMethods);
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses.size());
            ZLCHelperMethods.writeZLCFileH(method2testClasses, methodsCheckSums,classesChecksums, loader, getArtifactsDir(), null, false,
                    zlcFormat);
            List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTestClasses);
            dynamicallyUpdateExcludes(excludePaths);
        }
    }

    protected void setChangedAndNonaffectedMethods(ClassLoader loader) throws MojoExecutionException {
        List<Set<String>> data = ZLCHelperMethods.getChangedDataH(loader ,getArtifactsDir(), cleanBytes,classesChecksums, METHODS_TEST_DEPS_ZLC_FILE, CLASSES_ZLC_FILE);
        methodsCheckSums = MethodLevelStaticDepsBuilder.getMethodsCheckSum();
        changedClasses = data == null ? new HashSet<String>() : data.get(0);
        changedMethods = data == null ? new HashSet<String>() : data.get(1);
        affectedTestClasses = data == null ? new HashSet<String>() : data.get(2);

        impactedMethods = findImpactedMethods(changedMethods);
        for (String impactedMethod : impactedMethods) {
            affectedTestClasses.addAll(method2testClasses.getOrDefault(impactedMethod, new HashSet<String>()));
        }

        nonAffectedTestClasses = MethodLevelStaticDepsBuilder.getTests();
        nonAffectedTestClasses.removeAll(affectedTestClasses);
    }

    private Set<String> findImpactedMethods(Set<String> affectedMethods) {
        Set<String> impactedMethods = new HashSet<>(affectedMethods);
        for (String method : affectedMethods) {
            impactedMethods.addAll(MethodLevelStaticDepsBuilder.getMethodDeps(method));

        }
        return impactedMethods;
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