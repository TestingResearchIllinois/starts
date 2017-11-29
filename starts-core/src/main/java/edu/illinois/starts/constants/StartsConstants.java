/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.constants;

import java.io.File;

/**
 * Some constants used throughout the STARTS codebase.
 */
public interface StartsConstants {
    String STARTS_DIRECTORY_PATH = ".starts" + File.separator;
    String MIN_SUREFIRE_VERSION = "2.13";

    String SUREFIRE_PLUGIN_VM = "org/apache/maven/plugin/surefire/SurefirePlugin";
    String SUREFIRE_PLUGIN_BIN = "org.apache.maven.plugin.surefire.SurefirePlugin";

    String ABSTRACT_SUREFIRE_MOJO_VM = "org/apache/maven/plugin/surefire/AbstractSurefireMojo";
    String ABSTRACT_SUREFIRE_MOJO_BIN = "org.apache.maven.plugin.surefire.AbstractSurefireMojo";
    String SUREFIRE_INTERCEPTOR_CLASS_VM = "edu/illinois/starts/maven/SurefireMojoInterceptor";

    String MOJO_EXECUTION_EXCEPTION_BIN = "org.apache.maven.plugin.MojoExecutionException";

    String EXECUTE_MNAME = "execute";
    String EXECUTE_MDESC = "()V";

    String STARTS_NAME = "STARTS";

    String ARGLINE_FIELD = "argLine";
    String EXCLUDES_FIELD = "excludes";

    //runOrder is normally surefire.runOrder, but the prefix does not seem to be needed,
    //and indeed does not seem to work, when run this way
    String RUNORDER_FIELD = "runOrder";
    String THREADCOUNT_FIELD = "threadCount";
    String PARALLEL_FIELD = "parallel";

    String STARTS_EXCLUDE_PROPERTY = "STARTS_EXCLUDES";
}
