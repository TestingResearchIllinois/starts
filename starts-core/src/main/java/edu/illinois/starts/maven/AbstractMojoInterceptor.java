/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.util.Logger;

public abstract class AbstractMojoInterceptor implements StartsConstants {

    protected static final Logger LOGGER = Logger.getGlobal();

    public static URL extractJarURL(URL url) throws IOException {
        LOGGER.log(Level.INFO, "jeff entered extractJarURL2");
        if (url == null) {
            LOGGER.log(Level.INFO, "url is null");
        }
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        LOGGER.log(Level.INFO, "jeff successfully opened jarURLconnection");
        return connection.getJarFileURL();
    }

    public static URL extractJarURL(Class<?> clz) throws IOException {
        LOGGER.log(Level.INFO, "jeff entered extractJarURL");
        return extractJarURL(getResource(clz));
    }

    public static URL getResource(Class<?> clz) {
        LOGGER.log(Level.INFO, "jeff entered getResource with fs char: " + File.separatorChar);
        URL resource = clz.getResource("\\" + clz.getName().replace('.', File.separatorChar) + ".class");
        LOGGER.log(Level.INFO, "jeff got resource: ");
        return resource;
    }

    protected static void throwMojoExecutionException(Object mojo, String message, Exception cause) throws Exception {
        Class<?> clz = mojo.getClass().getClassLoader().loadClass(MOJO_EXECUTION_EXCEPTION_BIN);
        Constructor<?> con = clz.getConstructor(String.class, Exception.class);
        Exception ex = (Exception) con.newInstance(message, cause);
        throw ex;
    }

    protected static void setField(String fieldName, Object mojo, Object value) throws Exception {
        Field field;
        try {
            field = mojo.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException ex) {
            field = mojo.getClass().getSuperclass().getDeclaredField(fieldName);
        }
        field.setAccessible(true);
        field.set(mojo, value);
    }

    protected static Object getField(String fieldName, Object mojo) throws Exception {
        Field field;
        try {
            field = mojo.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException ex) {
            field = mojo.getClass().getSuperclass().getDeclaredField(fieldName);
        }
        field.setAccessible(true);
        return field.get(mojo);
    }

    protected static List<String> getListField(String fieldName, Object mojo) throws Exception {
        return (List<String>) getField(fieldName, mojo);
    }
}
