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


@Mojo(name = "methods-tm", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class MethodsTMMojo extends MethodsMojo {

    private Logger logger;
    private Set<String> changedMethods;
    private Set<String> affectedTestMethods;
    private Set<String> nonAffectedTestMethods;
    private Map<String, String> methodsCheckSums;
    private Map<String, Set<String>> methods2testmethods;

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
            MethodLevelStaticDepsBuilder.buildMethodsGraph(loader);
            MethodLevelStaticDepsBuilder.computeChecksums(loader);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MethodLevelStaticDepsBuilder.computeMethod2TestMethods();
        methods2testmethods = MethodLevelStaticDepsBuilder.methods2testmethods;
        methodsCheckSums = MethodLevelStaticDepsBuilder.methodsCheckSums;

        runMethods(loader);
    }

    protected void runMethods(ClassLoader loader) throws MojoExecutionException {
        // Checking if the file of depedencies exists

        if (!Files.exists(Paths.get(getArtifactsDir() + METHODS_TEST_DEPS_ZLC_FILE_TM))) {
            changedMethods = MethodLevelStaticDepsBuilder.getMethods();
            affectedTestMethods = MethodLevelStaticDepsBuilder.getTestMethods();
            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "AffectedTestMethods: " + affectedTestMethods.size());            
            ZLCHelperMethods.writeZLCFileTM(methods2testmethods, methodsCheckSums, loader, getArtifactsDir(), null, false,
                    zlcFormat);
            dynamicallyUpdateExcludes(new ArrayList<String>());

        } else {
            setChangedAndNonaffectedMethods();
            logger.log(Level.INFO, "ChangedMethods: " + changedMethods.size());
            logger.log(Level.INFO, "AffectedTestMethods: " + affectedTestMethods.size());
            ZLCHelperMethods.writeZLCFileTM(methods2testmethods, methodsCheckSums, loader, getArtifactsDir(), null, false,
                    zlcFormat);
            List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTestMethods);
            dynamicallyUpdateExcludes(excludePaths);
        }
    }

    protected void setChangedAndNonaffectedMethods() throws MojoExecutionException {
        List<Set<String>> data = ZLCHelperMethods.getChangedData(getArtifactsDir(), cleanBytes, methodsCheckSums,METHODS_TEST_DEPS_ZLC_FILE_TM );
        changedMethods = data == null ? new HashSet<String>() : data.get(0);
        affectedTestMethods = data == null ? new HashSet<String>() : data.get(1);
        nonAffectedTestMethods = MethodLevelStaticDepsBuilder.getTests();
        nonAffectedTestMethods.removeAll(affectedTestMethods);
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