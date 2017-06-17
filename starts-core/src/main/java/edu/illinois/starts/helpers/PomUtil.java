/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import edu.illinois.starts.constants.StartsConstants;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Utility methods for manipulating pom.xml files.
 */
public class PomUtil implements StartsConstants {
    public static String extractParamValue(Plugin plugin, String elem) throws MojoExecutionException {
        String value = null;
        Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
        if (dom != null) {
            Xpp3Dom child = dom.getChild(elem);
            value = child == null ? null : child.getValue();
        }
        return value;
    }

    public static List<String> extractIncludeExcludes(Plugin plugin, String elem) throws MojoExecutionException {
        List<String> values = new ArrayList<>();
        String value;
        Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
        if (dom != null) {
            Xpp3Dom outer = dom.getChild(elem + "s");
            if (outer != null) {
                Xpp3Dom[] children = outer.getChildren(elem);
                for (int i = 0; i < children.length; i++) {
                    value = children[i].getValue() == null ? null : children[i].getValue();
                    if (value != null) {
                        values.add(value);
                    }
                }
            }
        }
        return values;
    }

    public static Plugin getSfPlugin(MavenProject project) throws MojoExecutionException {
        Plugin sfPlugin = lookupPlugin("org.apache.maven.plugins:maven-surefire-plugin", project);
        checkSFVersion(sfPlugin);
        return sfPlugin;
    }

    public static void checkSFVersion(Plugin sfPlugin) throws MojoExecutionException {
        if (sfPlugin == null) {
            throw new MojoExecutionException("Surefire plugin not available");
        }

        String version = sfPlugin.getVersion();
        if (StartsConstants.MIN_SUREFIRE_VERSION.compareTo(version) > 0) {
            throw new MojoExecutionException("Unsupported Surefire version: " + version
                    + ". Use version " + StartsConstants.MIN_SUREFIRE_VERSION + " and above.");
        }
    }

    public static Plugin lookupPlugin(String name, MavenProject project) {
        Plugin plugin = null;
        List<Plugin> plugins = project.getBuildPlugins();
        for (Plugin p : plugins) {
            if (p.getKey().equalsIgnoreCase(name)) {
                plugin = p;
                break;
            }
        }
        return plugin;
    }

    public static List<String> getFromPom(String type, MavenProject project) throws MojoExecutionException {
        String file = type + "sFile";
        List<String> values = new ArrayList<>();
        List<String> typeValues = extractIncludeExcludes(getSfPlugin(project), type);
        values.addAll(typeValues);
        String fileName = extractParamValue(getSfPlugin(project), file);
        if (fileName != null && !fileName.equals("myExcludes")) {
            try {
                values.addAll(Files.readAllLines(Paths.get(fileName), Charset.defaultCharset()));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return values;
    }

    /**
     * Copied from Ekstazi.
     */
    public static void appendExcludesListToExcludesFile(Plugin plugin, List<String> originalExcludes, List<String> excludes,
                                                        File baseDir) throws MojoExecutionException {
        String excludesParam = PomUtil.extractParamValue(plugin, "excludesFile");
        File excludesFile = new File(baseDir.getAbsolutePath() + File.separator + excludesParam);
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(new FileOutputStream(excludesFile, true), true);
            writer.println(STARTS_EXCLUDE_MARKER);
            for (String e : excludes) {
                writer.println(e);
            }
            //TODO: we need to confirm that this is really expected behavior from surefire
            //If surefire already declares <excludes>, then we should *not* use the default excludes regex.
            if (originalExcludes.isEmpty()) {
                writer.println("**/*$*");
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Could not access excludesFile", ioe);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static File getExcludesFile(Plugin plugin, File baseDir) throws MojoExecutionException {
        String excludesFileName = PomUtil.extractParamValue(plugin, "excludesFile");
        return new File(baseDir.getAbsolutePath() + File.separator + excludesFileName);
    }

    /**
     * The following three methods are replicated from Ekstazi because the ones
     * in Ekstazi has "Ekstazi" hard-coded or are private.
     * TODO: Use reflection for the private ones and remove from here.
     */
    public static void restoreExcludesFile(Plugin plugin, File baseDir) throws MojoExecutionException {
        File excludesFile = getExcludesFile(plugin, baseDir);
        restoreExcludeFile(excludesFile, STARTS_EXCLUDE_MARKER);
        removeExcludesFileIfEmpty(excludesFile);
    }

    public static void restoreExcludeFile(File excludesFile, String marker) throws MojoExecutionException {
        if (excludesFile.exists()) {
            try {
                String[] oldLines = org.ekstazi.util.FileUtil.readLines(excludesFile);
                ArrayList newLines = new ArrayList();
                String[] temp = oldLines;
                int len = oldLines.length;

                for (int i = 0; i < len; ++i) {
                    String line = temp[i];
                    if (line.equals(marker)) {
                        break;
                    }

                    newLines.add(line);
                }
                org.ekstazi.util.FileUtil.writeLines(excludesFile, newLines);
            } catch (IOException ioe) {
                throw new MojoExecutionException("Could not restore \'excludesFile\'", ioe);
            }
        }
    }

    public static void removeExcludesFileIfEmpty(File file) throws MojoExecutionException {
        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                if (br.readLine() == null) {
                    file.delete();
                }
            } catch (IOException ioe) {
                throw new MojoExecutionException("Could not remove \'excludesFile\'", ioe);
            }
        }
    }
}
