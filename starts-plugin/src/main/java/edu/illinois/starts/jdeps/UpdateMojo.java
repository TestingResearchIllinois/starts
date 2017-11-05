/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Update test dependencies on disk.
 */
@Mojo(name = "update", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
//Assume the user always run (e) before running this goal
//@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class UpdateMojo extends DiffMojo {
    public void execute() throws MojoExecutionException {
        Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));

        Set<String> changed = new HashSet<>();
        Set<String> nonAffected = new HashSet<>();
        Pair<Set<String>, Set<String>> data = computeChangeData();
        String extraText = "";
        if (data != null) {
            nonAffected = data.getKey();
            changed = data.getValue();
        } else {
            extraText = " (no RTS artifacts; likely the first run)";
        }
        printResult(changed, "ChangedClasses" + extraText);
        //updateDiffChecksums is always true for this module
        updateForNextRun(nonAffected);
    }
}
