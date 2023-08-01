/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.util.*;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelperMethods;
import edu.illinois.starts.maven.AgentLoader;
import edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.booter.Classpath;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


// import static edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder.buildMethodsGraph;
// import static edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder.methodName2MethodNames;

@Mojo(name = "methods", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class MethodsMojo extends DiffMojo {
    private static final String TARGET = "target";

    private Logger logger;
    protected List<Pair> jarCheckSums = null;
    private Set<String> impactedMethods;
    private Set<String> changedMethods;
    private Set<String> affectedTests;
    private Set<String> nonAffectedTests;
    Map<String, String> methodsCheckSums;
    Map<String, Set<String>> method2tests;

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        method2tests = MethodLevelStaticDepsBuilder.method2tests;
        methodsCheckSums = MethodLevelStaticDepsBuilder.methodsCheckSums;
        
        runMethods(loader);
        
        // // If it is first run with update methods checksums as true, then all methods are impacted.
        // // impacted = (updateMethodsChecksums && data == null) ? getAllMethods() : findImpactedMethods(affected);
        



        // // Optionally update methods-deps.zlc
        // if (updateMethodsChecksums) {
        //     this.updateForNextRun(null);
        // }

        // Writer.writeToFile(changed, "changed-methods", getArtifactsDir());
        // Writer.writeToFile(impacted, "impacted-methods", getArtifactsDir());
    }


    protected void runMethods(ClassLoader loader) throws MojoExecutionException {
        // Checking if the file of depedencies exists
        

        if (!Files.exists(Paths.get(getArtifactsDir()+ METHODS_TEST_DEPS_ZLC_FILE))) {
            changedMethods = MethodLevelStaticDepsBuilder.getMethods();
            impactedMethods = MethodLevelStaticDepsBuilder.getMethods();
            affectedTests = MethodLevelStaticDepsBuilder.getTests();


            logger.log(Level.INFO, "Changed: " + changedMethods.size());
            logger.log(Level.INFO, "Impacted: " + impactedMethods.size()); 
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTests.size()); 
            ZLCHelperMethods.writeZLCFile(method2tests, methodsCheckSums, loader, getArtifactsDir(), null, false, zlcFormat);
            dynamicallyUpdateExcludes(new ArrayList<String>());
            
        } else {
            setChangedAndNonaffectedMethods();
            logger.log(Level.INFO, "Changed: " + changedMethods.size());
            logger.log(Level.INFO, "Impacted: " + impactedMethods.size()); 
            logger.log(Level.INFO, "AffectedTestClasses: " + affectedTests.size()); 
            ZLCHelperMethods.writeZLCFile(method2tests, methodsCheckSums, loader, getArtifactsDir(), null, false, zlcFormat);
            List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTests);
            dynamicallyUpdateExcludes(excludePaths);
        }

        
        
        // if (!isSameClassPath(sfPathElements) || !hasSameJarChecksum(sfPathElements)) {
        //     nonAffectedTestMethods = new HashSet<>();
        //     changedMethods = MethodLevelStaticDepsBuilder.getMethods();
        //     impactedMethods = MethodLevelStaticDepsBuilder.getMethods();
        //     affectedTests = MethodLevelStaticDepsBuilder.getTests();
        //     dynamicallyUpdateExcludes(new ArrayList<String>());

        //     System.out.println("We are Here" );
        //     logger.log(Level.INFO, "CHANGED: " + changedMethods.toString());
        //     logger.log(Level.INFO, "IMPACTED: " + impactedMethods.toString()); 
            
        //     Writer.writeClassPath(cpString, artifactsDir);
        //     Writer.writeJarChecksums(sfPathElements, artifactsDir, jarCheckSums);
        // } else {
        //     setChangedAndNonaffectedMethods();
        //     // List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTestsMethods);
        //     // dynamicallyUpdateExcludes(excludePaths);
        // }
        // long startUpdateTime = System.currentTimeMillis();
        // if (updateMethodsChecksums) {
        //     updateForNextRunMethod(nonAffectedTestMethods);
        // }


        // long endUpdateTime = System.currentTimeMillis();
        // logger.log(Level.FINE, PROFILE_STARTS_MOJO_UPDATE_TIME
        //         + Writer.millsToSeconds(endUpdateTime - startUpdateTime));
    }


    protected void setChangedAndNonaffectedMethods() throws MojoExecutionException {
        List<Set<String>> data = ZLCHelperMethods.getChangedData(getArtifactsDir(), cleanBytes, methodsCheckSums);
        changedMethods = data == null ? new HashSet<String>() : data.get(0);
        impactedMethods = data == null ? new HashSet<String>() : data.get(1);
        affectedTests = data == null ? new HashSet<String>() : data.get(2);
        nonAffectedTests = MethodLevelStaticDepsBuilder.getTests();
        nonAffectedTests.removeAll(affectedTests);
    }


    protected void updateForNextRunMethod(Set<String> nonAffected) throws MojoExecutionException {
        Classpath sfClassPath = getSureFireClassPath();
        ClassLoader loader = createClassLoader(sfClassPath);
        // ZLCHelperMethods.updateZLCFile(loader, getArtifactsDir(),
        //         nonAffectedTestMethods, useThirdParty, zlcFormat);
    }

    private Set<String> findImpactedMethods(Set<String> affectedMethods) {
        Set<String> impactedMethods = new HashSet<>(affectedMethods);
        Map<String, Set<String>> graph = MethodLevelStaticDepsBuilder.methodName2MethodNames;
        for (String method : affectedMethods) {
            if (graph.containsKey(method)) {
                impactedMethods.addAll(graph.get(method));
            }
        }
        return impactedMethods;
    }

    private Set<String> getAllMethods() {
        Set<String> allMethods = new HashSet<>();
        for (Set<String> methods : MethodLevelStaticDepsBuilder.methodName2MethodNames.values()) {
            allMethods.addAll(methods);
        }
        return allMethods;
    }

    private void dynamicallyUpdateExcludes(List<String> excludePaths) throws MojoExecutionException {
        if (AgentLoader.loadDynamicAgent()) {
            logger.log(Level.FINEST, "AGENT LOADED!!!");
            System.setProperty(STARTS_EXCLUDE_PROPERTY, Arrays.toString(excludePaths.toArray(new String[0])));
        } else {
            throw new MojoExecutionException("I COULD NOT ATTACH THE AGENT");
        }
    }


    private boolean isSameClassPath(List<String> sfPathString) throws MojoExecutionException {
        if (sfPathString.isEmpty()) {
            return true;
        }
        String oldSfPathFileName = Paths.get(getArtifactsDir(), SF_CLASSPATH).toString();
        if (!new File(oldSfPathFileName).exists()) {
            return false;
        }
        try {
            List<String> oldClassPathLines = Files.readAllLines(Paths.get(oldSfPathFileName));
            if (oldClassPathLines.size() != 1) {
                throw new MojoExecutionException(SF_CLASSPATH + " is corrupt! Expected only 1 line.");
            }
            List<String> oldClassPathelements = getCleanClassPath(oldClassPathLines.get(0));
            // comparing lists and not sets in case order changes
            if (sfPathString.equals(oldClassPathelements)) {
                return true;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }

    private boolean hasSameJarChecksum(List<String> cleanSfClassPath) throws MojoExecutionException {
        if (cleanSfClassPath.isEmpty()) {
            return true;
        }
        String oldChecksumPathFileName = Paths.get(getArtifactsDir(), JAR_CHECKSUMS).toString();
        if (!new File(oldChecksumPathFileName).exists()) {
            return false;
        }
        boolean noException = true;
        try {
            List<String> lines = Files.readAllLines(Paths.get(oldChecksumPathFileName));
            Map<String, String> checksumMap = new HashMap<>();
            for (String line : lines) {
                String[] elems = line.split(COMMA);
                checksumMap.put(elems[0], elems[1]);
            }
            jarCheckSums = new ArrayList<>();
            for (String path : cleanSfClassPath) {
                Pair<String, String> pair = Writer.getJarToChecksumMapping(path);
                jarCheckSums.add(pair);
                String oldCS = checksumMap.get(pair.getKey());
                noException &= pair.getValue().equals(oldCS);
            }
        } catch (IOException ioe) {
            noException = false;
            // reset to null because we don't know what/when exception happened
            jarCheckSums = null;
            ioe.printStackTrace();
        }
        return noException;
    }

    private List<String> getCleanClassPath(String cp) {
        List<String> cpPaths = new ArrayList<>();
        String[] paths = cp.split(File.pathSeparator);
        String classes = File.separator + TARGET +  File.separator + CLASSES;
        String testClasses = File.separator + TARGET + File.separator + TEST_CLASSES;
        for (int i = 0; i < paths.length; i++) {
            // TODO: should we also exclude SNAPSHOTS from same project?
            if (paths[i].contains(classes) || paths[i].contains(testClasses)) {
                continue;
            }
            cpPaths.add(paths[i]);
        }
        return cpPaths;
    }
}