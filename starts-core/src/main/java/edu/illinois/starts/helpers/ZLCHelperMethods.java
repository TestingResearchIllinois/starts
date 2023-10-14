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
     * This helper method is used in hybrid class-level analysis returns a list of
     * sets
     * containing information about added classes, deleted classes, changed classes
     * with changed headers, changed classes without changed headers, and old
     * classes.
     */
    public static List<Set<String>> getChangedDataHybridClassLevel(Map<String, List<String>> newClassesChecksums,
            String artifactsDir, String classesFilePath) {
        long start = System.currentTimeMillis();

        // Reading the old classes checksums
        Map<String, List<String>> oldClassChecksums = deserializeMapping(artifactsDir, classesFilePath);

        List<Set<String>> classesChanges = diffClasses(oldClassChecksums, newClassesChecksums);
        Set<String> addedClasses = classesChanges.get(0);
        Set<String> deletedClasses = classesChanges.get(1);
        Set<String> changedClassesWithChangedHeaders = classesChanges.get(2);
        Set<String> changedClassesWithoutChangedHeaders = classesChanges.get(3);
        Set<String> oldClasses = new HashSet<>(oldClassChecksums.keySet());
        oldClasses.removeAll(deletedClasses);

        long end = System.currentTimeMillis();
        List<Set<String>> result = new ArrayList<>();
        Collections.addAll(result, addedClasses, deletedClasses, changedClassesWithChangedHeaders,
                changedClassesWithoutChangedHeaders, oldClasses);
        LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);
        return result;
    }

    /*
     * This helper method is used in hybrid method-level analysis returns a list of
     * sets
     * containing information about changed methods and new methods in changed
     * classes without changed headers.
     * It also constructs a methods checksums map for next run.
     */
    public static List<Set<String>> getChangedDataHybridMethodLevel(Set<String> addedClasses, Set<String> deletedClasses,
            Set<String> changedClassesWithChangedHeaders, Set<String> changedClassesWithoutChangedHeaders,
            Map<String, String> methodChecksums,
            ClassLoader loader,
            String artifactsDir, String methodFilePath) {

        Map<String, String> oldMethodsChecksums = deserializeMapping(artifactsDir, methodFilePath);
        methodChecksums.putAll(oldMethodsChecksums);

        Map<String, String> addedClassesMethodsChecksums = MethodLevelStaticDepsBuilder
                .getMethodsChecksumsForClasses(addedClasses, loader);

        Map<String, String> changedClassesWithChangedHeadersMethodsChecksums = MethodLevelStaticDepsBuilder
                .getMethodsChecksumsForClasses(changedClassesWithChangedHeaders, loader);

        Map<String, String> changedClassesWithoutChangedHeadersMethodsChecksums = MethodLevelStaticDepsBuilder
                .getMethodsChecksumsForClasses(changedClassesWithoutChangedHeaders, loader);


        List<Set<String>> changedAndNewMethodsForMethodAnalysis = findChangedAndNewMethods(oldMethodsChecksums,
                changedClassesWithoutChangedHeadersMethodsChecksums);


        // Constructing a methods checksums map for next run
        Set<String> modifiedOldClasses = new HashSet<>();
        modifiedOldClasses.addAll(deletedClasses);
        modifiedOldClasses.addAll(changedClassesWithChangedHeaders);
        modifiedOldClasses.addAll(changedClassesWithoutChangedHeaders);
        // Updating methods checksums for non changed classes
        Set<String> modifiedOldClassesMethodsOldChecksums = new HashSet<>();
        for (String modifiedOldClass : modifiedOldClasses) {
            for (String methodPath : oldMethodsChecksums.keySet()) {
                if (methodPath.startsWith(modifiedOldClass + "#")) {
                    modifiedOldClassesMethodsOldChecksums.add(methodPath);
                }
            }
        }
        methodChecksums.keySet().removeAll(modifiedOldClassesMethodsOldChecksums);
        methodChecksums.putAll(changedClassesWithoutChangedHeadersMethodsChecksums);
        methodChecksums.putAll(changedClassesWithChangedHeadersMethodsChecksums);
        methodChecksums.putAll(addedClassesMethodsChecksums);

        return changedAndNewMethodsForMethodAnalysis;
    }

    /*
     * This method find changed and added methods in a set of classes
     * Arguments:
     * - oldMethodsChecksums: a map from method names to their checksums
     * - newMethodsChecksums: a map from method names to their checksums
     *
     * Returns:
     * - a set of changed methods
     * - a set of new methods
     */
    public static List<Set<String>> findChangedAndNewMethods(Map<String, String> oldMethodsChecksums,
            Map<String, String> newMethodsChecksums) {
        Set<String> changedMethods = new HashSet<>();
        Set<String> newMethods = new HashSet<>();
        List<Set<String>> result = new ArrayList<>();

        for (String method : newMethodsChecksums.keySet()) {
            String newChecksum = newMethodsChecksums.get(method);
            String oldChecksum = oldMethodsChecksums.get(method);

            if (oldChecksum == null) {
                newMethods.add(method);
            } else if (oldChecksum.equals(newChecksum)) {
                continue;
            } else {
                changedMethods.add(method);
            }
        }
        Collections.addAll(result, changedMethods, newMethods);
        return result;
    }

    /*
     * This method reasons about the class level changes
     * Arguments:
     * - oldClassesChecksums: a map from class names to their checksums (file
     * checksum, headers checksum)
     * - newClassesChecksums: a map from class names to their checksums (file
     * checksum, headers checksum)
     *
     * Returns:
     * - a list of sets containing information about newClasses, Deleted Classess,
     * changed classes with changed headers, changed classes without changed
     * headers.
     */
    public static List<Set<String>> diffClasses(Map<String, List<String>> oldClassesChecksums,
            Map<String, List<String>> newClassesChecksums) {
        List<Set<String>> results = new ArrayList<>();

        Set<String> deletedClasses = new HashSet<>(oldClassesChecksums.keySet());
        deletedClasses.removeAll(newClassesChecksums.keySet());

        Set<String> addedClasses = new HashSet<>(newClassesChecksums.keySet());
        addedClasses.removeAll(oldClassesChecksums.keySet());

        Set<String> changedClassesWithChangedHeaders = new HashSet<>();
        Set<String> changedClassesWithoutChangedHeaders = new HashSet<>();

        for (String className : newClassesChecksums.keySet()) {
            List<String> oldClassCheckSums = oldClassesChecksums.getOrDefault(className, null);
            if (oldClassCheckSums == null) {
                continue;
            }
            List<String> newClassChecksums = newClassesChecksums.get(className);

            if (oldClassCheckSums.get(1).equals(newClassChecksums.get(1))) {
                if (!oldClassCheckSums.get(0).equals(newClassChecksums.get(0))) {
                    changedClassesWithoutChangedHeaders.add(className);
                }
            } else {
                changedClassesWithChangedHeaders.add(className);
            }
        }

        Collections.addAll(results, addedClasses, deletedClasses, changedClassesWithChangedHeaders,
                changedClassesWithoutChangedHeaders);
        return results;
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
    public static <T> void serializeMapping(Map<String, T> map, String artifactsDir, String fileName)
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
    private static <T> Map<String, T> deserializeMapping(String artifactsDir, String filename) {
        Map<String, T> map = new HashMap<>();
        try {
            FileInputStream fis = new FileInputStream(artifactsDir + filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (HashMap<String, T>) ois.readObject();
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
