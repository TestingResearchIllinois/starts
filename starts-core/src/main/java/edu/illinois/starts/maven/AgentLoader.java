package edu.illinois.starts.maven;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;

import com.sun.tools.attach.VirtualMachine;

import edu.illinois.starts.constants.StartsConstants;

/**
 * This class is duplicated from Ekstazi, with minor changes.
 */
public final class AgentLoader implements StartsConstants {
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
            return false;
        }
    }

    public static boolean loadAgent(URL aju) throws Exception {
        attachAgent(aju);
        return true;
    }

    private static void attachAgent(URL aju) throws Exception {
        String pid = getPID();
        String agentAbsolutePath = new File(aju.toURI().getSchemeSpecificPart()).getAbsolutePath();

        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(agentAbsolutePath);
        vm.detach();
    }

    private static String getPID() {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        return vmName.substring(0, vmName.indexOf("@"));
    }
}
