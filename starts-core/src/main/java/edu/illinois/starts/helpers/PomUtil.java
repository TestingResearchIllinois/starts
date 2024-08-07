/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.IOException;
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
        if (sfPlugin == null) {
            throw new MojoExecutionException("Surefire plugin not available");
        }
        return sfPlugin;
    }

    public static String getSfVersion(MavenProject project) throws MojoExecutionException {
        return getSfPlugin(project).getVersion();
    }

    public static boolean invalidSfVersion(MavenProject project) throws MojoExecutionException {
        String version = getSfVersion(project);
        return MIN_SUREFIRE_VERSION.compareTo(version) > 0;
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
        // Did not find plugin under <build><plugins>,
        // check under <build><pluginManagement><plugins>
        if (plugin == null) {
            plugins = project.getPluginManagement().getPlugins();
            for (Plugin p : plugins) {
                if (p.getKey().equalsIgnoreCase(name)) {
                    plugin = p;
                    break;
                }
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
}
