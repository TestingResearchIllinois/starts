/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.data.ZLCData;
import edu.illinois.starts.data.ZLCFileContent;
import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder;
import edu.illinois.starts.util.ChecksumUtil;
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
    private static final String NOEXISTING_ZLCFILE_FIRST_RUN = "@NoExistingZLCFile. First Run?";

    /**
     * This method is a high-level method that handles the computation and creation
     * of a file that (1) stores the method-level dependency graph.
     * (2) Additionally stores class-level checksums used in hybrid analysis.
     *
     * @param methodsToTests   mapping from methods to test classes
     * @param checksumsMap     mapping from methods to their checksums
     * @param classesChecksums mapping from classes to their checksums (used in
     *                         hybrid analysis only)
     * @param loader           the class loader
     * @param artifactsDir     the directory where the zlc file is to be created
     * @param useThirdParty    flag to indicate whether to use third party libraries
     *                         jar
     * @param format           format of the zlc file. copied from ZLCHelper.java.
     *                         see param format in ZLCHelper.java
     */

    public static void writeZLCFile(Map<String, Set<String>> methodsToTests, Map<String, String> checksumsMap,
            Map<String, String> classesChecksums, ClassLoader loader,
            String artifactsDir, boolean useThirdParty,
            ZLCFormat format, boolean isHybrid) {
        long start = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "ZLC format: " + format.toString());

        // Create ZLC for method-level depentency
        ZLCFileContent zlc = createZLCFileData(methodsToTests, checksumsMap, loader, useThirdParty, format, true);
        Writer.writeToFile(zlc, METHODS_TEST_DEPS_ZLC_FILE, artifactsDir);

        if (isHybrid) {
            // Compute the class-level checksums, used for hybrid analysis only.
            zlc = createZLCFileData(null, classesChecksums, loader, useThirdParty, format, false);
            Writer.writeToFile(zlc, CLASSES_ZLC_FILE, artifactsDir);
        }
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "[PROFILE] updateForNextRun(updateZLCFile): " + Writer.millsToSeconds(end - start));
    }

    /**
     * This method computes the method-level checksums or class-level checksums (for
     * hybrid) based on isMethod flag.
     *
     * @param methodsToTests mapping from methods to test classes
     * @param checksumsMap   mapping from methods (or classes) to their checksums
     * @param isMethod       flag to indicate whether to compute method-level or
     *                       class-level checksums
     */
    public static ZLCFileContent createZLCFileData(
            Map<String, Set<String>> methodToTests,
            Map<String, String> checksumMap,
            ClassLoader loader,
            boolean useJars,
            ZLCFormat format,
            boolean isMethod) {

        long start = System.currentTimeMillis();
        List<ZLCData> zlcData = new ArrayList<>();
        ArrayList<String> itemList = isMethod ? new ArrayList<>(methodToTests.keySet())
                : new ArrayList<>(checksumMap.keySet());

        for (Map.Entry<String, String> entry : checksumMap.entrySet()) {
            String itemPath = entry.getKey();
            String itemChecksum = entry.getValue();
            Set<String> deps = isMethod ? methodToTests.getOrDefault(itemPath, new HashSet<>()) : new HashSet<>();

            String klas = ChecksumUtil.toClassOrJavaName(isMethod ? itemPath.split("#")[0] : itemPath, false);
            URL url = loader.getResource(klas);
            String extForm = url.toExternalForm();
            if (ChecksumUtil.isWellKnownUrl(extForm) || (!useJars && extForm.startsWith("jar:"))) {
                continue;
            }
            String classURL = url.toString();
            URL newUrl = null;
            try {
                newUrl = isMethod ? new URL(classURL + "#" + itemPath.split("#")[1]) : new URL(classURL);
            } catch (MalformedURLException malformedURLException) {
                throw new RuntimeException(malformedURLException);
            }
            zlcData.add(new ZLCData(newUrl, itemChecksum, format, deps, null));
        }

        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]CREATING ZLC FILE: " + (end - start) + MILLISECOND);
        return new ZLCFileContent(itemList, zlcData, format);
    }

    /**
     * This helper method is used in method-level analysis returns a list of sets
     * containing information about changed methods, new methods, affected tests,
     * old classes, and changed classes.
     * This method is called from MethodMojo.java from the setChangedMethods()
     * method.
     *
     * @param artifactsDir        The directory where the serialized file is saved.
     * @param methodsChecksums    A map containing the method names and their
     *                            checksums.
     * @param methodToTestClasses A map from method names to their test classes/
     * @return A list of sets containing all the information described above.
     */

    public static List<Set<String>> getChangedDataMethods(String artifactsDir, Map<String, String> methodsChecksums,
            Map<String, Set<String>> methodToTestClasses) {
        long start = System.currentTimeMillis();

        Map<String, String> oldMethodChecksums = new HashMap<>();
        oldMethodChecksums = deserializeMapping(artifactsDir, METHODS_CHECKSUMS_SERIALIZED_FILE);

        Set<String> changedMethods = new HashSet<>();
        Set<String> affectedTests = new HashSet<>();
        Set<String> oldClasses = new HashSet<>();
        Set<String> changedClasses = new HashSet<>();
        Set<String> newMethods = new HashSet<>(methodsChecksums.keySet());

        for (String method : oldMethodChecksums.keySet()) {
            String oldChecksum = oldMethodChecksums.get(method);
            String newChecksum = methodsChecksums.get(method);
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
            changedClasses.add(convertPath(method.split("#")[0]));
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
     * @param classesChecksums A map containing the class names and their
     *                         checksums.
     * @return A list of sets containing information about changed methods, new
     *         methods, affected tests, old classes, and changed classes. Returns
     *         null if the zlc file does not exist.
     */
    public static List<Set<String>> getChangedDataHybrid(ClassLoader loader, String artifactsDir, boolean cleanBytes,
            Map<String, String> classesChecksums, String methodFilePath, String classesFilePath) {
        long start = System.currentTimeMillis();

        File zlc = new File(artifactsDir, classesFilePath);
        if (!zlc.exists()) {
            LOGGER.log(Level.FINEST, NOEXISTING_ZLCFILE_FIRST_RUN);
            return null;
        }

        Set<String> changedClasses = new HashSet<>();
        Set<String> oldClasses = new HashSet<>();

        try {
            List<String> zlcLines = Files.readAllLines(zlc.toPath(), Charset.defaultCharset());
            zlcLines.remove(0);

            // on PLAIN_TEXT format, testsCount+1 will start from 0
            for (int i = 0; i < zlcLines.size(); i++) {

                String line = zlcLines.get(i);
                String[] parts = line.split(WHITE_SPACE);
                String classURL = parts[0];
                String oldCheckSum = parts[1];

                // convert classURL to class name
                String className = convertPath(classURL);
                String newChecksum = classesChecksums.get(className);
                oldClasses.add(className);
                if (oldCheckSum.equals(newChecksum)) {
                    continue;
                } else {
                    changedClasses.add(className);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        Set<String> newClasses = new HashSet<>(classesChecksums.keySet());
        newClasses.removeAll(oldClasses);

        zlc = new File(artifactsDir, methodFilePath);
        if (!zlc.exists()) {
            LOGGER.log(Level.FINEST, NOEXISTING_ZLCFILE_FIRST_RUN);
            return null;
        }

        // changed classes (comparing old and new checksums of classes)
        // methods checksums for changed classes

        Map<String, String> methodsChecksums = MethodLevelStaticDepsBuilder
                .getMethodsChecksumsForClasses(changedClasses, loader);

        Set<String> changedMethods = new HashSet<>();
        Set<String> newMethods = new HashSet<>(methodsChecksums.keySet());
        Set<String> affectedTestClasses = new HashSet<>();
        Map<String, String> oldMethodsChecksums = new HashMap<>();
        Map<String, Set<String>> oldMethodsDeps = new HashMap<>();
        try {
            List<String> zlcLines = Files.readAllLines(zlc.toPath(), Charset.defaultCharset());
            zlcLines.remove(0);

            // on PLAIN_TEXT format, testsCount+1 will start from 0
            for (int i = 0; i < zlcLines.size(); i++) {

                String line = zlcLines.get(i);
                String[] parts = line.split(WHITE_SPACE);
                // classURL#methodname
                String stringURL = parts[0];
                String classURL = stringURL.split("#")[0];
                String methodName = stringURL.split("#")[1];
                String oldCheckSum = parts[1];
                Set<String> deps;
                deps = parts.length == 3 ? fromCSV(parts[2]) : new HashSet<>(); // Fields should be returned

                // convert classURL to class name
                String className = convertPath(classURL);
                String methodPath = className + "#" + methodName;
                oldMethodsDeps.put(methodPath, deps);
                oldMethodsChecksums.put(methodPath, oldCheckSum);
                newMethods.remove(methodPath);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Finding Changed Methods
        for (String methodPath : methodsChecksums.keySet()) {
            String newChecksum = methodsChecksums.get(methodPath);
            String oldChecksum = oldMethodsChecksums.get(methodPath);

            if (oldChecksum != null && !oldChecksum.equals(newChecksum)) {
                changedMethods.add(methodPath);
                affectedTestClasses.addAll(oldMethodsDeps.getOrDefault(methodPath, new HashSet<>()));
            }
        }

        // Updating methods checksums for non changed but impacted classes
        Set<String> changedClassesMethodsOldChecksums = new HashSet<>();
        for (String changedClass : changedClasses) {
            for (String methodPath : oldMethodsChecksums.keySet()) {
                if (methodPath.startsWith(changedClass + "#")) {
                    changedClassesMethodsOldChecksums.add(methodPath);
                }
            }
        }
        oldMethodsChecksums.keySet().removeAll(changedClassesMethodsOldChecksums);
        methodsChecksums.putAll(oldMethodsChecksums);

        long end = System.currentTimeMillis();
        List<Set<String>> result = new ArrayList<>();
        Collections.addAll(result, changedMethods, newMethods, affectedTestClasses, changedClasses, newClasses,
                oldClasses);
        LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);
        return result;
    }

    /**
     * This method converts a given full path to a class path by removing the prefix
     * up to and including "classes" or "test-classes" and replacing the ".class"
     * extension with an empty string.
     *
     * @param fullPath The full path to be converted.
     * @return The converted class path string.
     */
    public static String convertPath(String fullPath) {
        String[] parts = fullPath.split("/");
        int index = 0;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("classes") || parts[i].equals("test-classes")) {
                index = i + 1;
                break;
            }
        }
        String result = "";
        for (int i = index; i < parts.length; i++) {
            result += parts[i];
            if (i != parts.length - 1) {
                result += "/";
            }
        }
        return result.replace(".class", "");
    }

    /**
     * This method converts a comma-separated string of tests into a set of strings.
     *
     * @param tests A comma-separated string of tests.
     * @return A set of strings representing the tests.
     */
    private static Set<String> fromCSV(String tests) {
        return new HashSet<>(Arrays.asList(tests.split(COMMA)));
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
