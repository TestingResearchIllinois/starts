/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.constants;

import java.io.File;

/**
 * Some constants used throughout the STARTS codebase.
 */

public interface StartsConstants {

    String DOT_STARTS = ".starts";
    String STARTS_NAME = "STARTS";
    String MIN_SUREFIRE_VERSION = "2.13";
    String STARTS_DIRECTORY_PATH = ".starts" + File.separator;

    String SUREFIRE_PLUGIN = "org.apache.maven.plugins:maven-surefire-plugin";
    String SUREFIRE_PLUGIN_VM = "org/apache/maven/plugin/surefire/SurefirePlugin";
    String SUREFIRE_PLUGIN_BIN = "org.apache.maven.plugin.surefire.SurefirePlugin";
    String SUREFIRE_INTERCEPTOR_CLASS_VM = "edu/illinois/starts/maven/SurefireMojoInterceptor";
    String ABSTRACT_SUREFIRE_MOJO_VM = "org/apache/maven/plugin/surefire/AbstractSurefireMojo";
    String ABSTRACT_SUREFIRE_MOJO_BIN = "org.apache.maven.plugin.surefire.AbstractSurefireMojo";

    String MOJO_EXECUTION_EXCEPTION_BIN = "org.apache.maven.plugin.MojoExecutionException";

    String ZLC_FILE = "deps.zlc";

    String EXECUTE_MDESC = "()V";
    String EXECUTE_MNAME = "execute";

    String ARGLINE_FIELD = "argLine";
    String EXCLUDES_FIELD = "excludes";
    String MY_EXCLUDES = "myExcludes";
    String STARTS_EXCLUDE_PROPERTY = "STARTS_EXCLUDES";

    // simple characters:
    String AT = "@";
    String DOT = ".";
    String EMPTY = "";
    String STAR = "*";
    String COMMA = ",";
    String EQUAL = "=";
    String FILE_SEPARATOR = File.separator;
    char FILE_SEPARATOR_CHAR = File.separatorChar;
    String FILE_PATH_SEPARATOR = File.pathSeparator;
    char FILE_PATH_SEPARATOR_CHAR = File.pathSeparatorChar;
    String COLON = ": ";
    String WHITE_SPACE = " ";
    String LEFT_BRACKET = "[";
    String RIGHT_BRACKET = "]";
    String LEFT_PARENTHESIS = "(";
    String RIGHT_PARENTHESIS = ")";
    String LEFT_RIGHT_BRACKETS = "[]";
    String STARS = "**********";

    // Annotations:
    String RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";
    String RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";
    String RUNTIME_VISIBLE_TYPE_ANNOTATIONS = "RuntimeVisibleTypeAnnotations";
    String RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = "RuntimeInvisibleTypeAnnotations";
    String RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations";
    String RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations";

    String MILLISECOND = "ms";
    String TRUE = "true";
    String CODE = "Code";
    String FALSE = "false";
    String SIGNATURE = "Signature";
    String SYNTHETIC = "Synthetic";
    String DEPRECATED = "Deprecated";
    String EXCEPTIONS = "Exceptions";
    String METHOD_PARAMETERS = "MethodParameters";
    String ANNOTATION_DEFAULT = "AnnotationDefault";
    String SOURCE_FILE = "SourceFile";
    String INNER_CLASSES = "InnerClasses";
    String CONSTANT_VALUE = "ConstantValue";
    String ENCLOSING_METHOD = "EnclosingMethod";
    String BOOTSTRAP_METHODS = "BootstrapMethods";
    String SOURCE_DEBUG_EXTENSION = "SourceDebugExtension";

    // Logger Messages:
    String BROKEN_EDGE = "@@BrokenEdge: ";
    String AGENT_LOADED = "AGENT LOADED!!!";
    String STARS_RUN_STARS = "********** Run **********";
    String LOADED_NULL_URL_FOR_DEP = "@@LoadedNullURLForDep: ";
    String LOADING_FROM_NORMAL_CACHE = "@@LoadingFromNormalCache: ";
    String NO_TESTS_ARE_SELECTED_TO_RUN = "No tests are selected to run.";
    String LOADED_CACHED_EDGES_FROM_JARS = "@@LoadedCachedEdgesFromJars: ";
    /*
     * String NOEXISTING_ZLCFILE_FIRST_RUN = "@NoExistingZLCFile. First Run?";
     */
    String NO_RTS_ARTIFACTS_LIKELY_THE_FIRST_RUN = " (no RTS artifacts; likely the first run)";
    String THERE_ARE_NO_CLASS_FILES_IN_THIS_MODULE = "There are no .class files in this module.";
    String TIME_WRITING_FILES = "[TIME]WRITING FILES: ";
    String TIME_CREATING_ZLC_FILE = "[TIME]CREATING ZLC FILE: ";
    String TIME_UPDATING_CHECKSUMS = "[TIME]UPDATING CHECKSUMS: ";
    String TIME_RESAVING_CHECKSUMS = "[TIME]RE-SAVING CHECKSUMS: ";
    String TIME_COMPUTING_AFFECTED = "[TIME]COMPUTING AFFECTED: ";
    String TIME_COMPUTING_NON_AFFECTED = "[TIME]COMPUTING NON-AFFECTED: ";
    String TIME_COMPUTING_NON_AFFECTED_2 = "[TIME]COMPUTING NON-AFFECTED(2): ";
    String TIME_COMPUTING_EXISTING_CLASSES = "[TIME]COMPUTING EXISTING CLASSES: ";
    String PROFILE_RUN_MOJO_TOTAL = "[PROFILE] RUN-MOJO-TOTAL: ";
    String PROFILE_END_OF_RUN_MOJO = "[PROFILE] END-OF-RUN-MOJO: ";
    String PROFILE_TEST_RUNNING_TIME = "[PROFILE] TEST-RUNNING-TIME: ";
    String PROFILE_COMPUTING_CHANGES = "[PROFILE] COMPUTING CHANGES: ";
    String PROFILE_STARTS_MOJO_TOTAL = "[PROFILE] STARTS-MOJO-TOTAL: ";
    String PROFILE_STARTS_MOJO_UPDATE_TIME = "[PROFILE] STARTS-MOJO-UPDATE-TIME: ";
    String PROFILE_CREATE_LOADABLE_TOTAL = "[PROFILE] createLoadable(TOTAL): ";
    String PROFILE_CREATE_LOADABLE_RUNJDEPS = "[PROFILE] createLoadable(runJDeps): ";
    String PROFILE_CREATE_LOADABLE_BUILDGRAPH = "[PROFILE] createLoadable(buildGraph): ";
    String PROFILE_CREATE_LOADABLE_FINDUNREACHED = "[PROFILE] createLoadable(findUnreached): ";
    String PROFILE_CREATE_LOADABLE_TRANSITIVECLOSURE = "[PROFILE] createLoadable(transitiveClosure): ";
    String PROFILE_PREPARE_FOR_NEXT_RUN_LOAD_MORE_EDGES = "[PROFILE] prepareForNextRun(loadMoreEdges): ";
    String PROFILE_PREPARE_FOR_NEXT_RUN_CREATE_LOADABLE = "[PROFILE] prepareForNextRun(createLoadable): ";
    String PROFILE_PREPARE_FOR_NEXT_RUN_COMPUTE_AFFECTED_TESTS = "[PROFILE] prepareForNextRun(computeAffectedTests): ";
    String PROFILE_PREPARE_FOR_NEXT_RUN_LOAD_M2_EDGES_FROM_CACHE = "[PROFILE] prepareForNextRun(loadM2EdgesFromCache): ";
    String PROFILE_UPDATE_AND_PREPARE_FOR_NEXT_RUN_TOTAL = "[PROFILE] updateForNextRun(prepareForNextRun(TOTAL)): ";
    String PROFILE_UPDATE_FOR_NEXT_RUN_TOTAL = "[PROFILE] updateForNextRun(total): ";
    String PROFILE_UPDATE_FOR_NEXT_RUN_PATHTOSTRING = "[PROFILE] updateForNextRun(pathToString): ";
    String PROFILE_UPDATE_FOR_NEXT_RUN_UPDATEZLCFILE = "[PROFILE] updateForNextRun(updateZLCFile): ";
    String PROFILE_UPDATE_FOR_NEXT_RUN_CREATE_CLASS_LOADER = "[PROFILE] updateForNextRun(createClassLoader): ";
    String PROFILE_UPDATE_FOR_NEXT_RUN_SET_INCLUDES_EXCLUDES = "[PROFILE] updateForNextRun(setIncludesExcludes): ";
    String PROFILE_UPDATE_FOR_NEXT_RUN_GET_SUREFIRE_CLASSPATH = "[PROFILE] updateForNextRun(getSureFireClassPath): ";

    // Types' Names:
    String INT_NAME = "int";
    String VOID_NAME = "void";
    String CHAR_NAME = "char";
    String BYTE_NAME = "byte";
    String LONG_NAME = "long";
    String SHORT_NAME = "short";
    String FLOAT_NAME = "float";
    String DOUBLE_NAME = "double";
    String BOOLEAN_NAME = "boolean";

    /* String LIB = "lib"; */
    String JRE = "jre";
    String MAC = "mac";
    String ZLC = "ZLC";
    String MD5 = "MD5";
    String HOME = "Home";
    String GRAPH = "graph";
    String MORE = "More: ";
    String S_FILE = "sFile";
    String CONFIG = "CONFIG";
    String ATTACH = "attach";
    String DETACH = "detach";
    /* String TARGET = "target"; */
    String MY_ROLE = "MyRole";
    String G_CACHE = "gCache";
    String FILE_NAME = "file";
    String CLASSES = "classes";
    String PROJECT = "project";
    String INCLUDE = "include";
    String EXCLUDE = "exclude";
    String CHANGED = "CHANGED: ";
    String VALUE_NAME = "value ";
    String PROFILE = "[PROFILE] ";
    String STACK_MAP = "StackMap";
    String OS_DOT_NAME = "os.name";
    String JAVA_HOME = "java_home";
    String IGNORING = "Ignoring: ";
    String IMPACTED = "IMPACTED: ";
    String ALL_TESTS = "all-tests";
    String JDEPS_OUT = "jdeps-out";
    String FILTER_LIB = "filterLib";
    String DEP_FORMAT = "depFormat";
    String LOAD_AGENT = "loadAgent";
    String RETEST_ALL = "retestAll";
    String EXCLUDES = "@@Excludes: ";
    String INCLUDES = "@@Includes: ";
    String AND_ABOVE = " and above.";
    String DIFF_MARKER = "::Diff:: ";
    String JDEPS_CMD = "JDEPS CMD: ";
    String CLEAN_BYTES = "cleanBytes";
    String PRINT_GRAPH = "printGraph";
    String JDEPS_ARGS = "JDEPS ARGS:";
    String ALL_COUNT = "ALL(count): ";
    String ROOT_DOT_DIR = "root.dir=";
    String NEW_CLASSES = "new-classes";
    String JAVA_DOT_HOME = "java.home";
    String INITIALIZED = " Initialized";
    String NEWLY_ADDED = "NEWLY-ADDED: ";
    String SF_CLASSPATH = "sf-classpath";
    String TEST_CLASSES = "test-classes";
    String Use_version = ". Use version ";
    String STARTS_NODES = "STARTS:Nodes: ";
    String STARTS_EDGES = "STARTS:Edges: ";
    String JAR_CHECKSUMS = "jar-checksums";
    String AFFECTED_TESTS = "AffectedTests";
    String STARTS_LOGGING = "startsLogging";
    String STARTS_INVOKED = "STARTS-invoked";
    String SELECTED_TESTS = "selected-tests";
    String CHANGEDCLASSES = "ChangedClasses";
    String LINE_SEPARATOR = "line.separator";
    String STACK_MAP_TABLE = "StackMapTable";
    String SNAPSHOT_DOT_JAR = "-SNAPSHOT.jar";
    String CHANGED_CLASSES = "changed-classes";
    String SF_CLASSPATH_COLON = "SF-CLASSPATH: ";
    String TRACK_NEW_CLASSES = "trackNewClasses";
    String Line_Number_Table = "LineNumberTable";
    String IMPACTED_CLASSES = "impacted-classes";
    String UNREACHED_COUNT = "UNREACHED(count): ";
    String NOT_FIRST_RUN_CLZ = "not-first-run.clz";
    String GET_TEST_CLASSES = "(getTestClasses): ";
    String UPDATING_EXCLUDES = "updating Excludes";
    String LOADED_RESOURCES = "LOADED RESOURCES: ";
    String FOUND_NO_CLASSES = " found no classes.";
    String UPDATE_FOR_NEXT_RUN = "updateForNextRun";
    String NON_AFFECTED_TESTS = "non-affected-tests";
    String STARTS_TOTAL_TESTS = "STARTS:TotalTests: ";
    String UPDATE_RUN_CHECKSUMS = "updateRunChecksums";
    String LOCAL_VARIABLE_TABLE = "LocalVariableTable";
    String SCAN_FOR_TEST_CLASSES = "scanForTestClasses";
    String CHECK_IF_ALL_AFFECTED = "checkIfAllAffected";
    String UPDATE_DIFF_CHECKSUMS = "updateDiffChecksums";
    String STARTS_AFFECTED_TESTS = "STARTS:AffectedTests: ";
    String UPDATE_SELECT_CHECKSUMS = "updateSelectChecksums";
    String LOCAL_VARIABLE_TYPE_TABLE = "LocalVariableTypeTable";
    String UPDATE_IMPACTED_CHECKSUMS = "updateImpactedChecksums";
    String NUMBER_OF_AFFECTED_TESTS_EXPECTED = "Number of affected tests expected: ";

    String GZ_EXTENSION = ".gz";
    String CLZ_EXTENSION = ".clz";
    String JAR_EXTENSION = ".jar";
    String JAVA_EXTENSION = ".java";
    String CLASS_EXTENSION = ".class";
    /* String GRAPH_EXTENSION = ".graph"; */
    String TOOLS_DOT_JAR = "tools.jar";
    String JDK_DOT_GRAPH = "jdk.graph";
    String CLASSES_DOT_JAR = "classes.jar";
    String CLASS_CLASS = "java/lang/Class";
    String STRING_CLASS = "java/lang/String";
    String OBJECT_CLASS = "java/lang/Object";
    String THROWABLE_CLASS = "java/lang/Throwable";
    String METHODTYPE_CLASS = "java/lang/invoke/MethodType";
    String METHODHANDLE_CLASS = "java/lang/invoke/MethodHandle";
    String VIRTUALMACHINE_ATTACH_API_CLASS = "com.sun.tools.attach.VirtualMachine";

    // Exceptions And Warnings' Messages:
    String CLASS_NOT_FOUND_EXCEPTION = "Class not found";
    String NO_EDGE_TARGET_EXCEPTION = "@@@NoEdgeTarget: ";
    String CLASS_FILE_TOO_LARGE_EXCEPTION = "Class file too large!";
    String METHOD_CODE_TOO_LARGE_EXCEPTION = "Method code too large!";
    String EDGE_SHOULD_HAVE_LENGTH_TWO_EXCEPTION = "Edge should have length 2";
    String COULD_NOT_ATTACH_THE_AGENT_EXCEPTION = "I COULD NOT ATTACH THE AGENT";
    /*
     * String UNSUPPORTED_SUREFIRE_VERSION_EXCEPTION =
     * "Unsupported surefire version. ";
     */
    String SUREFIRE_PLUGIN_NOT_AVAILABLE_EXCEPTION = "Surefire plugin not available";
    String COULD_NOT_CREATE_ARTIFACTS_DIR_EXCEPTION = "I could not create artifacts dir: ";
    String COULD_NOT_CREATE_JDEPS_CACHE_EXCEPTION = "I could not create the jdeps cache: ";
    String UNSUPPORTED_SUREFIRE_VERSION_COLON_EXCEPTION = "Unsupported Surefire version: ";
    String LABEL_OFFSET_NOT_RESOLVED_EXCEPTION = "Label offset position has not been resolved yet";
    String JSR_RET_NOT_SUPPORTED_EXCEPTION = "JSR/RET are not supported with computeFrames option";
    String JDEPS_CANNOT_RUN_WITH_EMPTY_CLASSPATH_EXCEPTION = "JDEPS cannot run with an empty classpath.";
    String COULD_NOT_FIND_CREATE_JDEPS_GRAPH_EXCEPTION = "I could not find or create jdeps graphs in any cache: ";
    String AFFECTED_TESTS_SHOULD_NOT_BE_NULL_WITH_CLZ_FORMAT_EXCEPTION = "Affected tests should "
            + "not be null with CLZ format!";
    String TRY_SETTING_EXCLUDESFILE_SUREFIRE_CONFIGURATION_EXCEPTION = "Try setting excludesFile "
            + "in the surefire configuration.";
    String JDEPS_CACHE_EMPTY_RUNNING_IN_RECURSIVE_MODE_WARNING = "Should jdeps cache really be empty? "
            + "Running in recursive mode.";

}
