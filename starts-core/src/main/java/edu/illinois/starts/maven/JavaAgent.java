/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.maven;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import edu.illinois.starts.constants.StartsConstants;

public class JavaAgent implements StartsConstants {
    /**
     * JVM hook to statically load the javaagent at startup.
     * After the Java Virtual Machine (JVM) has initialized, the premain method
     * will be called. Then the real application main method will be called.
     *
     * @param args Arguments to pass to the jvm
     * @param inst The instrumentation instance
     * @throws Exception If the class cannot the modified
     */
    public static void premain(String args, Instrumentation inst) throws Exception {
    }

    /**
     * This method is invoked if we start the agent after the VM already started.
     * We use this method to hijack the surefire instance being run, so that we
     * can set its argLine correctly all the time.
     */
    public static void agentmain(String options, Instrumentation instrumentation) {
        instrumentation.addTransformer(new MavenCFT(), true);
        instrumentMaven(instrumentation);
    }

    private static void instrumentMaven(Instrumentation instrumentation) {
        try {
            for (Class<?> clz : instrumentation.getAllLoadedClasses()) {
                String name = clz.getName();
                if (name.equals(ABSTRACT_SUREFIRE_MOJO_BIN) || name.equals(SUREFIRE_PLUGIN_BIN)) {
                    instrumentation.retransformClasses(clz);
                }
            }
        } catch (UnmodifiableClassException uce) {
            uce.printStackTrace();
        }
    }
}
