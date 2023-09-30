/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder;
import edu.illinois.starts.util.Logger;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * Utility methods for dealing with the .zlc format.
 */
public class ZLCHelperMethods implements StartsConstants {
    private static final Logger LOGGER = Logger.getGlobal();

    /**
     * This helper method is used in method-level analysis returns a list of sets
     * containing information about changed methods, new methods, affected tests,
     * old classes, and changed classes.
     * This method is called from MethodMojo.java from the setChangedMethods()
     * method.
     *
     * @param artifactsDir        The directory where the serialized file is saved.
     * @param newMethodsChecksums A map containing the method names and their
     *                            checksums.
     * @param methodToTestClasses A map from method names to their test classes/
     * @return A list of sets containing all the information described above.
     */

    public static List<Set<String>> getChangedDataMethods(Map<String, String> newMethodsChecksums,
            Map<String, Set<String>> methodToTestClasses, String artifactsDir, String filepath) {
        long start = System.currentTimeMillis();

        Map<String, String> oldMethodChecksums = deserializeMapping(artifactsDir, filepath);

        Set<String> changedMethods = new HashSet<>();
        Set<String> affectedTests = new HashSet<>();
        Set<String> oldClasses = new HashSet<>();
        Set<String> changedClasses = new HashSet<>();
        Set<String> newMethods = new HashSet<>(newMethodsChecksums.keySet());

        for (String method : oldMethodChecksums.keySet()) {
            String oldChecksum = oldMethodChecksums.get(method);
            String newChecksum = newMethodsChecksums.get(method);
            Set<String> deps = methodToTestClasses.getOrDefault(method, new HashSet<>());
            String className = method.split("#")[0];

            oldClasses.add(className);
            newMethods.remove(method);
            if (newChecksum == null) {
                continue;
            } else if (oldChecksum.equals(newChecksum)) {
                continue;
            } else {
                changedMethods.add(method);
                affectedTests.addAll(deps);
                changedClasses.add(className);
            }
        }

        long end = System.currentTimeMillis();
        List<Set<String>> result = new ArrayList<>();
        for (String method : newMethods) {
            changedClasses.add(method.split("#")[0]);
        }
        Collections.addAll(result, changedMethods, newMethods, affectedTests, oldClasses, changedClasses);
        LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);
        return result;
    }

    /**
     * This helper method is used in hybrid analysis and returns a list of sets
     * containing information about changed methods, new methods, affected tests,
     * old classes, and changed classes.
     * This method is called from HybridMojo.java from the setChangedMethods()
     * method.
     *
     * @param newClassesChecksums A map containing the class names and their
     *                            checksums.
     * @return A list of sets containing information about changed methods, new
     *         methods, affected tests, old classes, and changed classes. Returns
     *         null if the zlc file does not exist.
     */
    public static List<Set<String>> getChangedDataHybrid(Map<String, String> newClassesChecksums,
            Map<String, Set<String>> methodToTestClasses, ClassLoader loader,
            String artifactsDir, String classesFilePath, String methodFilePath, boolean updateChecksums) {
        long start = System.currentTimeMillis();

        Map<String, String> oldClassChecksums = deserializeMapping(artifactsDir, classesFilePath);

        Set<String> changedClasses = new HashSet<>();
        Set<String> oldClasses = new HashSet<>();

        for (String className : oldClassChecksums.keySet()) {
            String oldCheckSum = oldClassChecksums.get(className);
            String newChecksum = newClassesChecksums.get(className);
            oldClasses.add(className);
            if (oldCheckSum.equals(newChecksum)) {
                continue;
            } else {
                changedClasses.add(className);
            }
        }

        Set<String> newClasses = new HashSet<>(newClassesChecksums.keySet());
        newClasses.removeAll(oldClasses);

        Map<String, String> oldMethodsChecksums = deserializeMapping(artifactsDir, methodFilePath);

        // Compute
        Map<String, String> newMethodsChecksums = MethodLevelStaticDepsBuilder
                .getMethodsChecksumsForClasses(changedClasses, loader);

        Set<String> changedMethods = new HashSet<>();
        Set<String> newMethods = new HashSet<>(newMethodsChecksums.keySet());
        newMethods.removeAll(oldMethodsChecksums.keySet());
        Set<String> affectedTestClasses = new HashSet<>();

        // Finding Changed Methods. Since newMethodsChecksums contains only methods
        // inside changed classes, we're good to go.
        for (String method : newMethodsChecksums.keySet()) {
            String newChecksum = newMethodsChecksums.get(method);
            String oldChecksum = oldMethodsChecksums.get(method);

            Set<String> deps = methodToTestClasses.getOrDefault(method, new HashSet<>());
            String className = method.split("#")[0];

            if (oldChecksum == null || newChecksum == null) {
                // Deleted or new method
                continue;
            } else if (oldChecksum.equals(newChecksum)) {
                continue;
            } else {
                // Update checksums in the mapping on the fly
                if (updateChecksums) {
                    oldMethodsChecksums.put(method, newChecksum);
                }
                changedMethods.add(method);
                affectedTestClasses.addAll(deps);
                changedClasses.add(className);
            }
        }

        if (updateChecksums) {
            try {
                serializeMapping(oldMethodsChecksums, artifactsDir, methodFilePath);
            } catch (IOException exception) {
                // TODO Auto-generated catch block
                exception.printStackTrace();
            }
        }

        long end = System.currentTimeMillis();
        List<Set<String>> result = new ArrayList<>();
        Collections.addAll(result, changedMethods, newMethods, affectedTestClasses, changedClasses, newClasses,
                oldClasses);
        LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);
        return result;
    }

    /**
     * This method prints the content of a given MethodNode object using a Textifier
     * printer and a TraceMethodVisitor. The method includes the access code,
     * signature, name, and description of the MethodNode object in the returned
     * string.
     *
     * @param node The MethodNode object whose content is to be printed.
     * @return A string representing the content of the MethodNode object.
     */
    public static String printMethodContent(MethodNode node) {
        Printer printer = new Textifier(Opcodes.ASM5) {
            @Override
            public void visitLineNumber(int line, Label start) {
            }
        };
        TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);
        node.accept(methodPrinter);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        // include the access code in case of access code changes
        String methodContent = node.access + "\n" + node.signature + "\n" + node.name + "\n" + node.desc + "\n"
                + sw.toString();
        return methodContent;
    }

    /**
     * This method serializes the method-to-testclasses mapping and saves it to the
     * specified directory.
     *
     * @param methodToTestClasses The mapping of methods to test classes.
     * @param artifactsDir        The directory where the serialized file will be
     *                            saved.
     * @throws IOException If an I/O error occurs.
     */
    public static void serializeMapping(Map<String, String> map, String artifactsDir, String fileName)
            throws IOException {
        try {
            FileOutputStream fos = new FileOutputStream(artifactsDir + fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(map);
            oos.close();
            fos.close();
            // System.out.println("Serialized HashMap data is saved in map.ser");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * This method deserializes the method-to-testclasses mapping from the specified
     * directory.
     *
     * @param artifactsDir The directory where the serialized file is saved.
     * @return The deserialized mapping of methods to test classes.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be
     *                                found.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> deserializeMapping(String artifactsDir, String filename) {
        Map<String, String> map = new HashMap<>();
        try {
            FileInputStream fis = new FileInputStream(artifactsDir + filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (HashMap<String, String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException classException) {
            classException.printStackTrace();
        }
        return map;
    }
}
