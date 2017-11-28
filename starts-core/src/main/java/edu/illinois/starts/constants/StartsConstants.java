/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.constants;

import java.io.File;

/**
 * Some constants used throughout the STARTS codebase.
 */

public interface StartsConstants {


    String SUREFIRE_PLUGIN_BIN = "org.apache.maven.plugin.surefire.SurefirePlugin";

    String EXCLUDES_FIELD = "excludes";
    String STARTS_EXCLUDE_PROPERTY = "STARTS_EXCLUDES";

    // simple characters:
    String DOT = ".";
    String EMPTY = "";
    String COMMA = ",";
    String FILE_SEPARATOR = File.separator;
    String COLON = ": ";
    String WHITE_SPACE = " ";
    String MILLISECOND = "ms";
    String TRUE = "true";
    String FALSE = "false";

    // Logger Messages:
    String STARS_RUN_STARS = "********** Run **********";
    String NO_TESTS_ARE_SELECTED_TO_RUN = "No tests are selected to run.";
    String TIME_COMPUTING_NON_AFFECTED = "[TIME]COMPUTING NON-AFFECTED: ";
    String PROFILE_RUN_MOJO_TOTAL = "[PROFILE] RUN-MOJO-TOTAL: ";
    String PROFILE_END_OF_RUN_MOJO = "[PROFILE] END-OF-RUN-MOJO: ";
    String PROFILE_TEST_RUNNING_TIME = "[PROFILE] TEST-RUNNING-TIME: ";
    String PROFILE_STARTS_MOJO_UPDATE_TIME = "[PROFILE] STARTS-MOJO-UPDATE-TIME: ";
    String PROFILE_UPDATE_FOR_NEXT_RUN_TOTAL = "[PROFILE] updateForNextRun(total): ";

    String CLASSES = "classes";
    String JAVA_HOME = "java.home";
    String SF_CLASSPATH = "sf-classpath";
    String TEST_CLASSES = "test-classes";
    String JAR_CHECKSUMS = "jar-checksums";
    String CHANGED_CLASSES = "changed-classes";
    String CHECK_IF_ALL_AFFECTED = "checkIfAllAffected";
    String STARTS_AFFECTED_TESTS = "STARTS:AffectedTests: ";

    String JAR_EXTENSION = ".jar";
    String CLASS_EXTENSION = ".class";

}
