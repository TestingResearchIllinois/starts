package edu.illinois.starts.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;

/** This class is from Ekstazi. **/

public final class SurefireMojoInterceptor extends AbstractMojoInterceptor implements StartsConstants {

    public static void execute(Object mojo) throws Exception {
        if (!isSurefirePlugin(mojo)) {
            return;
        }
        if (isAlreadyInvoked(mojo)) {
            return;
        }
        checkSurefireVersion(mojo);
        try {
            updateExcludes(mojo);
        } catch (Exception ex) {
            throwMojoExecutionException(mojo, "Unsupported surefire version", ex);
        }
    }

    private static boolean isSurefirePlugin(Object mojo) throws Exception {
        return mojo.getClass().getName().equals(SUREFIRE_PLUGIN_BIN);
    }

    private static boolean isAlreadyInvoked(Object mojo) throws Exception {
        String key = STARTS_NAME + System.identityHashCode(mojo);
        String value = System.getProperty(key);
        System.setProperty(key, "STARTS-invoked");
        return value != null;
    }

    private static void checkSurefireVersion(Object mojo) throws Exception {
        try {
            getField(ARGLINE_FIELD, mojo);
            getField(EXCLUDES_FIELD, mojo);
        } catch (NoSuchMethodException ex) {
            throwMojoExecutionException(mojo, "Unsupported surefire version. "
                     + "Try setting excludesFile in the surefire configuration.", ex);
        }
    }

    private static void updateExcludes(Object mojo) throws Exception {
        LOGGER.log(Level.FINE, "updating Excludes");
        List<String> currentExcludes = getListField(EXCLUDES_FIELD, mojo);
        List<String> newExcludes = new ArrayList<>(Arrays.asList(System.getProperty(STARTS_EXCLUDE_PROPERTY)
                .replace("[", "").replace("]", "").split(",")));
        if (currentExcludes != null) {
            newExcludes.addAll(currentExcludes);
        } else {
            newExcludes.add("**/*$*");
        }
        setField(EXCLUDES_FIELD, mojo, newExcludes);
    }
}
