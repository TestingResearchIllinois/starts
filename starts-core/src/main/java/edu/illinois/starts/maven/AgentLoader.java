package edu.illinois.starts.maven;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Optional;

import edu.illinois.starts.constants.StartsConstants;

/**
 * This class is duplicated from Ekstazi, with minor changes.
 */
public final class AgentLoader implements StartsConstants {
    private static final String TOOLS_JAR_NAME = "tools.jar";
    private static final String CLASSES_JAR_NAME = "classes.jar";
    private static final String LIB = "lib";
    private static final String AGENT_INIT = AgentLoader.class.getName() + " Initialized";

    public static boolean loadDynamicAgent() {
        try {
            if (System.getProperty(AGENT_INIT) != null) {
                return true;
            }
            System.setProperty(AGENT_INIT, EMPTY);

            URL agentJarURL = AbstractMojoInterceptor.class.getResource("JavaAgent.class");
            URL agentJarURLConnection = AbstractMojoInterceptor.extractJarURL(agentJarURL);
            return loadAgent(agentJarURLConnection);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean loadAgent(URL aju) throws Exception {
        File toolsJarFile = findToolsJar();
        if (toolsJarFile == null) {  // TODO: maybe remove null check
            return false;
        }
        URL toolsJarFileURL = toolsJarFile.toURI().toURL();

        Class<?> vc = loadVirtualMachine(new URL[]{toolsJarFileURL});
        if (vc == null) {
            return false;
        }

        attachAgent(vc, aju);
        return true;
    }

    private static void attachAgent(Class<?> vc, URL aju) throws Exception {
        String pid = getPID();
        String agentAbsolutePath = new File(aju.toURI().getSchemeSpecificPart()).getAbsolutePath();

        Object vm = getAttachMethod(vc).invoke(null, new Object[]{pid});
        getLoadAgentMethod(vc).invoke(vm, new Object[]{agentAbsolutePath});
        getDetachMethod(vc).invoke(vm);
    }

    private static Method getAttachMethod(Class<?> vc) throws SecurityException, NoSuchMethodException {
        return vc.getMethod("attach", new Class<?>[]{String.class});
    }

    private static Method getLoadAgentMethod(Class<?> vc) throws SecurityException, NoSuchMethodException {
        return vc.getMethod("loadAgent", new Class[]{String.class});
    }

    private static Method getDetachMethod(Class<?> vc) throws SecurityException, NoSuchMethodException {
        return vc.getMethod("detach");
    }

    private static Class<?> loadVirtualMachine(URL[] urls) throws Exception {
        // Code copied from ekstazi:
        // https://github.com/gliga/ekstazi/blob/6567da0534c20eeee802d2dfb8d216cbcbf6883c/org.ekstazi.core/src/main/java/org/ekstazi/agent/AgentLoader.java#L88
        try {
            return ClassLoader.getSystemClassLoader().loadClass("com.sun.tools.attach.VirtualMachine");
        } catch (ClassNotFoundException ex) {
            URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
            return loader.loadClass("com.sun.tools.attach.VirtualMachine");
        }
    }

    private static String getPID() {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        return vmName.substring(0, vmName.indexOf("@"));
    }

    private static File findToolsJar() {
        // Copied from ekstazi:
        // https://github.com/gliga/ekstazi/blob/6567da0534c20eeee802d2dfb8d216cbcbf6883c/org.ekstazi.core/src/main/java/org/ekstazi/agent/AgentLoader.java#L209
        String javaHome = System.getProperty(JAVA_HOME);
        File javaHomeFile = new File(javaHome);
        File tjf = new File(javaHomeFile, LIB + File.separator + TOOLS_JAR_NAME);

        if (!tjf.exists()) {
            tjf = new File(System.getenv("java_home"), LIB + File.separator + TOOLS_JAR_NAME);
        }

        if (!tjf.exists() && javaHomeFile.getAbsolutePath().endsWith(File.separator + "jre")) {
            javaHomeFile = javaHomeFile.getParentFile();
            tjf = new File(javaHomeFile, LIB + File.separator + TOOLS_JAR_NAME);
        }

        if (!tjf.exists() && isMac() && javaHomeFile.getAbsolutePath().endsWith(File.separator + "Home")) {
            javaHomeFile = javaHomeFile.getParentFile();
            tjf = new File(javaHomeFile, "Classes" + File.separator + CLASSES_JAR_NAME);
        }

        return tjf;
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
    }

    public static StringWriter loadAndRunJdeps(List<String> args) {
        StringWriter output = new StringWriter();
        try {
            File toolsJarFile = findToolsJar();
            if (!toolsJarFile.exists()) {
                // Java 9+, load jdeps through java.util.spi.ToolProvider
                Class<?> toolProvider = ClassLoader.getSystemClassLoader().loadClass("java.util.spi.ToolProvider");
                Object jdeps = toolProvider.getMethod("findFirst", String.class).invoke(null, "jdeps");
                jdeps = Optional.class.getMethod("get").invoke(jdeps);
                toolProvider.getMethod("run", PrintWriter.class, PrintWriter.class, String[].class)
                        .invoke(jdeps, new PrintWriter(output), new PrintWriter(output), args.toArray(new String[0]));
            } else {
                // Java 8, load tools.jar
                URLClassLoader loader = new URLClassLoader(new URL[] { toolsJarFile.toURI().toURL() },
                        ClassLoader.getSystemClassLoader());
                Class<?> jdepsMain = loader.loadClass("com.sun.tools.jdeps.Main");
                jdepsMain.getMethod("run", String[].class, PrintWriter.class)
                        .invoke(null, args.toArray(new String[0]), new PrintWriter(output));
            }
        } catch (MalformedURLException malformedURLException) {
            malformedURLException.printStackTrace();
        } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
        } catch (InvocationTargetException invocationTargetException) {
            invocationTargetException.printStackTrace();
        } catch (IllegalAccessException illegalAccessException) {
            illegalAccessException.printStackTrace();
        } catch (NoSuchMethodException noSuchMethodException) {
            noSuchMethodException.printStackTrace();
        }
        return output;
    }
}
