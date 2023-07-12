/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.helpers;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.illinois.starts.constants.StartsConstants;
import edu.illinois.starts.data.ZLCData;
import edu.illinois.starts.data.ZLCFileContent;
import edu.illinois.starts.data.ZLCFormat;
import edu.illinois.starts.util.ChecksumUtil;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;

import org.ekstazi.util.Types;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import static edu.illinois.starts.smethods.MethodLevelStaticDepsBuilder.*;

/**
 * Utility methods for dealing with the .zlc format.
 */
public class ZLCHelperMethods implements StartsConstants {
    public static final String zlcFile = "method-deps.zlc";
    public static final String STAR_FILE = "file:*";
    private static final Logger LOGGER = Logger.getGlobal();
    private static Map<String, ZLCData> zlcDataMap;
    private static final String NOEXISTING_ZLCFILE_FIRST_RUN = "@NoExistingZLCFile. First Run?";

    public ZLCHelperMethods() {
        zlcDataMap = new HashMap<>();
    }

    public static void updateZLCFile(Map<String, Set<String>> testDeps, ClassLoader loader,
            String artifactsDir, Set<String> unreached, boolean useThirdParty,
            ZLCFormat format) {
        long start = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "ZLC format: " + format.toString());
        ZLCFileContent zlc = createZLCData(testDeps, loader, useThirdParty, format);
        Writer.writeToFile(zlc, zlcFile, artifactsDir);
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINE, "[PROFILE] updateForNextRun(updateZLCFile): " + Writer.millsToSeconds(end - start));
    }

    public static ZLCFileContent createZLCData(
            Map<String, Set<String>> method2methods,
            ClassLoader loader,
            boolean useJars,
            ZLCFormat format) {

        long start = System.currentTimeMillis();
        List<ZLCData> zlcData = new ArrayList<>();
        
        ArrayList<String> methodList = new ArrayList<>(method2methods.keySet());  // all tests

        for (Map.Entry<String, Set<String>> entry : method2methods.entrySet()) {
            String methodPath = entry.getKey();
            Set<String> deps = entry.getValue();

            String klas = ChecksumUtil.toClassName(methodPath.split("#")[0]);
            String methodName = methodPath.split("#")[1].replace("()", "");

            URL url = loader.getResource(klas);

            String path = url.getPath();
            ClassNode node = new ClassNode(Opcodes.ASM5);
            ClassReader reader = null;
            try {
                reader = new ClassReader(new FileInputStream(path));
            } catch (IOException e) {
                System.out.println("[ERROR] reading class: " + klas);
                continue;
            }

            String methodChecksum = null;
            reader.accept(node, ClassReader.SKIP_DEBUG);
            List<MethodNode> methods = node.methods;
            for (MethodNode method : methods) {
                // methodName is from the generated graph
                // method.name is from method visitor (ASM) -> does not have signatures
                // Append the parameter signature
                String method1 = appendParametersToMethodName(method);
                if (!method1.equals(methodName))
                    continue;
                String methodContent = printMethodContent(method);
                try {
                    methodChecksum = ChecksumUtil.computeMethodChecksum(methodContent);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            
            String extForm = url.toExternalForm();
            if (ChecksumUtil.isWellKnownUrl(extForm) || (!useJars && extForm.startsWith("jar:"))) {
                continue;
            }
            String classURL = url.toString();
            URL newUrl = null;
            try {
                if (!methodName.matches(".*\\(.*\\)$")) {
                    newUrl = new URL(classURL + "#" + methodName + "()");
                } else {
                    newUrl = new URL(classURL + "#" + methodName);
                }
                
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            zlcData.add(new ZLCData(newUrl, methodChecksum, format, deps, null));
        }

        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, "[TIME]CREATING ZLC FILE: " + (end - start) + MILLISECOND);
        return new ZLCFileContent(methodList, zlcData, format);
    }

    public static Pair<Set<String>, Set<String>> getChangedData(String artifactsDir, boolean cleanBytes) {
        long start = System.currentTimeMillis();
        File zlc = new File(artifactsDir, zlcFile);

        if (!zlc.exists()) {
           
            
            LOGGER.log(Level.FINEST, NOEXISTING_ZLCFILE_FIRST_RUN);
            return null;
        }
        Set<String> changedClasses = new HashSet<>();
        Set<String> nonAffected = new HashSet<>();
        Set<String> affected = new HashSet<>();
        Set<String> starTests = new HashSet<>();
        ChecksumUtil checksumUtil = new ChecksumUtil(cleanBytes);
        try {
            List<String> zlcLines = Files.readAllLines(zlc.toPath(), Charset.defaultCharset());
            String firstLine = zlcLines.get(0);//class t affected tests
            String space = WHITE_SPACE;
            

            // check whether the first line is for *
            if (firstLine.startsWith(STAR_FILE)) {
                String[] parts = firstLine.split(space);
                starTests = fromCSV(parts[2]);
                zlcLines.remove(0);
            }

            ZLCFormat format = ZLCFormat.PLAIN_TEXT;  // default to plain text
            if (zlcLines.get(0).equals(ZLCFormat.PLAIN_TEXT.toString())) {
                format = ZLCFormat.PLAIN_TEXT;
                zlcLines.remove(0);
            } else if (zlcLines.get(0).equals(ZLCFormat.INDEXED.toString())) {
                format = ZLCFormat.INDEXED;
                zlcLines.remove(0);
            }

            int testsCount = -1;  // on PLAIN_TEXT, testsCount+1 will starts from 0
            ArrayList<String> testsList = null;
            if (format == ZLCFormat.INDEXED) {
                try {
                    testsCount = Integer.parseInt(zlcLines.get(0));
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
                testsList = new ArrayList<>(zlcLines.subList(1, testsCount + 1));
            }

            for (int i = testsCount + 1; i < zlcLines.size(); i++) {
                String line = zlcLines.get(i);
                String[] parts = line.split(space);
                String stringURL = parts[0];
                String oldCheckSum = parts[1];
                Set<String> tests;
                if (format == ZLCFormat.INDEXED) {
                    Set<Integer> testsIdx = parts.length == 3 ? fromCSVToInt(parts[2]) : new HashSet<>();
                    tests = testsIdx.stream().map(testsList::get).collect(Collectors.toSet());
                } else {
                    tests = parts.length == 3 ? fromCSV(parts[2]) : new HashSet<>();
                }
                nonAffected.addAll(tests);
                URL url = new URL(stringURL);
                String newCheckSum = checksumUtil.computeSingleCheckSum(url);
                if (!newCheckSum.equals(oldCheckSum)) {
                    
                    affected.addAll(tests);
                    changedClasses.add(stringURL);
                }
                if (newCheckSum.equals("-1")) {
                    // a class was deleted or auto-generated, no need to track it in zlc
                    LOGGER.log(Level.FINEST, "Ignoring: " + url);
                    continue;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        if (!changedClasses.isEmpty()) {
            // there was some change so we need to add all tests that reach star, if any
            affected.addAll(starTests);
        }
       
        
        nonAffected.removeAll(affected);

       /*  System.out.println("The affected:");
        System.out.println(affected);
        System.out.println("The unaffected:");
        System.out.println(nonAffected);
        */
        long end = System.currentTimeMillis();
        LOGGER.log(Level.FINEST, TIME_COMPUTING_NON_AFFECTED + (end - start) + MILLISECOND);


        // ShowimpactedC(changedClasses,artifactsDir);
      
        return new Pair<>(nonAffected, changedClasses);
    }

    private static Set<String> fromCSV(String tests) {
        return new HashSet<>(Arrays.asList(tests.split(COMMA)));
    }

    private static Set<Integer> fromCSVToInt(String tests) {
        return Arrays.stream(tests.split(COMMA)).map(Integer::parseInt).collect(Collectors.toSet());
    }

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
        String methodContent = node.access + "\n" + node.signature + "\n" + sw.toString();
        return methodContent;
    }

    private static String appendParametersToMethodName(MethodNode methodNode) {
        String method1 = methodNode.name;
        String parameters = methodNode.desc;
        Pattern pattern = Pattern.compile("\\(.*?\\)");
        Matcher matcher = pattern.matcher(parameters);
        if (matcher.find()) {
            String extracted = matcher.group();
            // Handle the case where there are no parameters
            if (extracted.equals("()"))
                return method1;

            method1 += extracted;
            return method1;
        }
        return method1;
    }
}