package edu.illinois.starts.maven;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is duplicated from Ekstazi, with minor changes.
 */
public final class AgentLoader implements StartsConstants {
    private static final String TOOLS_JAR_NAME = TOOLS_DOT_JAR;
    private static final String CLASSES_JAR_NAME = CLASSES_DOT_JAR;
    private static final String AGENT_INIT = AgentLoader.class.getName() + INITIALIZED;

    public static boolean loadDynamicAgent() {
        try {
            if (System.getProperty(AGENT_INIT) != null) {
                return true;
            }
            System.setProperty(AGENT_INIT, BLANK);

            URL agentJarURL = AbstractMojoInterceptor.extractJarURL(JavaAgent.class);
            return loadAgent(agentJarURL);
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean loadAgent(URL aju) throws Exception {
        URL toolsJarFile = findToolsJar();
        if (toolsJarFile == null) {
            return false;
        }

        Class<?> vc = loadVirtualMachine(new URL[]{toolsJarFile});
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
        return vc.getMethod(ATTACH, new Class<?>[]{String.class});
    }

    private static Method getLoadAgentMethod(Class<?> vc) throws SecurityException, NoSuchMethodException {
        return vc.getMethod(LOAD_AGENT, new Class[]{String.class});
    }

    private static Method getDetachMethod(Class<?> vc) throws SecurityException, NoSuchMethodException {
        return vc.getMethod(DETACH);
    }

    private static Class<?> loadVirtualMachine(URL[] urls) throws Exception {
        URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
        return loader.loadClass(VIRTUALMACHINE_ATTACH_API_CLASS);
    }

    private static String getPID() {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        return vmName.substring(0, vmName.indexOf(AT));
    }

    private static URL findToolsJar() throws MalformedURLException {
        String javaHome = System.getProperty(JAVA_DOT_HOME);
        File javaHomeFile = new File(javaHome);
        File tjf = new File(javaHomeFile, LIB + File.separator + TOOLS_JAR_NAME);

        if (!tjf.exists()) {
            tjf = new File(System.getenv(JAVA_HOME), LIB + File.separator + TOOLS_JAR_NAME);
        }

        if (!tjf.exists() && javaHomeFile.getAbsolutePath().endsWith(File.separator + JRE)) {
            javaHomeFile = javaHomeFile.getParentFile();
            tjf = new File(javaHomeFile, LIB + File.separator + TOOLS_JAR_NAME);
        }

        if (!tjf.exists() && isMac() && javaHomeFile.getAbsolutePath().endsWith(File.separator + HOME)) {
            javaHomeFile = javaHomeFile.getParentFile();
            tjf = new File(javaHomeFile, "Classes" + File.separator + CLASSES_JAR_NAME);
        }

        return tjf.toURI().toURL();
    }

    private static boolean isMac() {
        return System.getProperty(OS_DOT_NAME).toLowerCase().indexOf(MAC) >= 0;
    }
}
