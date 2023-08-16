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
import java.net.URL;
import java.nio.file.Files;

@Mojo(name = "methods", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class MethodsMojo extends DiffMojo {

    private Logger logger;
    private Set<String> changedMethods;
    private Set<String> newClasses;
    private Set<String> oldClasses;
    private Set<String> changedClasses;
    private Set<String> affectedTestClasses;
    private Set<String> nonAffectedTestClasses;
    private Set<String> nonAffectedMethods;
    private Map<String, String> methodsCheckSums;
    private Map<String, Set<String>> method2testClasses;

    @Parameter(property = "updateMethodsChecksums", defaultValue = TRUE)
    private boolean updateMethodsChecksums;

    public void setUpdateMethodsChecksums(boolean updateChecksums) {
        this.updateMethodsChecksums = updateChecksums;
    }


    public Set<String> getChangedMethods() {
        return Collections.unmodifiableSet(changedMethods);
    }

    public Set<String> getNewClasses() {
        return Collections.unmodifiableSet(newClasses);
    }

    public Set<String> getOldClasses() {
        return Collections.unmodifiableSet(oldClasses);
    }

    public Set<String> getChangedClasses() throws MojoExecutionException {
        Classpath sfClassPath = getSureFireClassPath();
        ClassLoader loader = createClassLoader(sfClassPath);
        Set<String> changedC = new HashSet<>();
        for (String c : changedClasses) {
            URL url = loader.getResource(c);
            String extForm = url.toExternalForm();
            changedC.add(extForm);
        }
        return Collections.unmodifiableSet(changedC);
    }


    public Set<String> getNonAffectedMethods() {
        return Collections.unmodifiableSet(nonAffectedMethods);
    }




    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        logger = Logger.getGlobal();
        setIncludesExcludes();
        Classpath sfClassPath = getSureFireClassPath();
        ClassLoader loader = createClassLoader(sfClassPath);

        // Build method level static dependencies
        try {
            MethodLevelStaticDepsBuilder.buildMethodsGraph(loader);
            MethodLevelStaticDepsBuilder.computeChecksums(loader);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        method2testClasses = MethodLevelStaticDepsBuilder.method2tests;
        methodsCheckSums = MethodLevelStaticDepsBuilder.methodsCheckSums;

        runMethods(loader);
    }

    protected void runMethods(ClassLoader loader) throws MojoExecutionException {

        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_TEST_DEPS_ZLC_FILE))) {
            changedMethods = MethodLevelStaticDepsBuilder.getMethods();
            affectedTestClasses = MethodLevelStaticDepsBuilder.getTests();
            oldClasses = new HashSet<>(); 
            changedClasses = new HashSet<>(); 
            newClasses = MethodLevelStaticDepsBuilder.getClasses();
            nonAffectedMethods = new HashSet<>();

            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses.size());
            logger.log(Level.INFO, "NewClasses: " + newClasses.size());
            logger.log(Level.INFO, "OldClasses: " + oldClasses.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            ZLCHelperMethods.writeZLCFile(method2testClasses, methodsCheckSums, loader, getArtifactsDir(), null, false,
                    zlcFormat);
            dynamicallyUpdateExcludes(new ArrayList<String>());

        } else {
            setChangedAndNonaffectedMethods();
            logger.log(Level.INFO, "Changed: " + changedMethods.size());
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTestClasses.size());
            logger.log(Level.INFO, "NewClasses: " + newClasses.size());
            logger.log(Level.INFO, "OldClasses: " + oldClasses.size());
            logger.log(Level.INFO, "ChangedClasses: " + changedClasses.size());
            ZLCHelperMethods.writeZLCFile(method2testClasses, methodsCheckSums, loader, getArtifactsDir(), null, false,
                    zlcFormat);
            List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTestClasses);
            dynamicallyUpdateExcludes(excludePaths);
        }
    }

    protected void setChangedAndNonaffectedMethods() throws MojoExecutionException {
        List<Set<String>> data = ZLCHelperMethods.getChangedData(getArtifactsDir(), cleanBytes, methodsCheckSums,
                METHODS_TEST_DEPS_ZLC_FILE);
        changedMethods = data == null ? new HashSet<String>() : data.get(0);
        affectedTestClasses = data == null ? new HashSet<String>() : data.get(1);
        oldClasses = data == null ? new HashSet<String>() : data.get(2);
        changedClasses = data == null ? new HashSet<String>() : data.get(3);
        newClasses = MethodLevelStaticDepsBuilder.getClasses();
        newClasses.removeAll(oldClasses);
        nonAffectedTestClasses = MethodLevelStaticDepsBuilder.getTests();
        nonAffectedTestClasses.removeAll(affectedTestClasses);
        nonAffectedMethods = MethodLevelStaticDepsBuilder.getMethods();
        nonAffectedMethods.removeAll(changedMethods);
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