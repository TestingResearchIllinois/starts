package edu.illinois.starts.smethods;

import org.ekstazi.asm.ClassReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import edu.illinois.starts.helpers.ZLCHelperMethods;
import edu.illinois.starts.util.ChecksumUtil;
import edu.illinois.starts.util.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class MethodLevelStaticDepsBuilder {

    // for every class, get the methods it implements
    public static Map<String, Set<String>> classToContainedMethodNames = new HashMap<>();

    // for every method, get the methods it invokes (direct invocation not
    // transitive)
    public static Map<String, Set<String>> methodNameToMethodNames = new HashMap<>();

    // Contains method to method dependency graph
    public static Map<String, Set<String>> methodDependencyGraph = new HashMap<>();

    // for every class, find its parents.
    public static Map<String, Set<String>> hierarchyParents = new HashMap<>();

    // for every class, find its children.
    public static Map<String, Set<String>> hierarchyChildren = new HashMap<>();

    public static Map<String, Set<String>> testClassesToMethods = new HashMap<>();

    public static Map<String, Set<String>> testClassesToClasses = new HashMap<>();

    public static Map<String, Set<String>> classesToTestClasses = new HashMap<>();

    // Map from method to test classes
    public static Map<String, Set<String>> methodToTestClasses = new HashMap<>();

    // Map from method to test methods
    public static Map<String, Set<String>> methodToTestMethods = new HashMap<>();

    // Map from class to checksum
    public static Map<String, List<String>> classesChecksums = new HashMap<>();

    // Map from method to checksum
    private static Map<String, String> methodsCheckSum = new HashMap<>();

    // Map for classes dependency graph
    private static Map<String, Set<String>> classesDependencyGraph = new HashMap<>();

    private static final Logger LOGGER = Logger.getGlobal();

    /**
     * This function builds the method dependency graph for all the methods in the
     * project.
     *
     */
    public static void buildMethodsGraph(boolean includeVariables) throws Exception {

        // find all the classes' files
        HashSet<String> classPaths = new HashSet<>(Files.walk(Paths.get("."))
                .filter(Files::isRegularFile)
                .filter(f -> (f.toString().endsWith(".class") && f.toString().contains("target")))
                .map(f -> f.normalize().toAbsolutePath().toString())
                .collect(Collectors.toList()));

        // Finding classToContainedMethodNames, methodNameToMethodNames,
        // hierarchy_parents, hierarchy_children
        findMethodsinvoked(classPaths);

        // Suppose that test classes have Test in their class name
        // and are in src/test
        Set<String> testClasses = new HashSet<>();
        for (String method : methodNameToMethodNames.keySet()) {
            String className = method.split("#|\\$")[0];
            if (className.contains("Test")) {
                testClasses.add(className);
            }
        }

        // Finding Test Classes to methods
        testClassesToMethods = getDepsSingleThread(testClasses);

        /*
         * Adding reflexive closure to methodNameToMethodNames
         * A -> B
         * It will be:
         * A -> A, B
         */
        addReflexiveClosure(methodNameToMethodNames);

        // Inverting methodNameToMethodNames to have the dependency graph for each
        // method
        // methodNameToMethodNames = invertMap(methodNameToMethodNames);
        methodDependencyGraph = invertMap(methodNameToMethodNames);

        if (includeVariables) {
            /*
             * The original dependency graph is like this:
             * (A, B, C) are classes
             * (a) is a variable
             * A -> A, B, C
             * a -> A
             * After this function call the dependency graph will be like this:
             * A -> A, B, C, a
             * a -> A
             */
            addVariableDepsToDependencyGraph();
        } else {
            // Remove any variables from keys or values i.e. pure method-level deps
            filterVariables();
        }

    }

    /**
     * This function builds the classToContainedMethodNames,
     * methodNameToMethodNames.
     * methodNameToMethodNames, hierarchy_parents, hierarchy_children maps
     */
    public static void findMethodsinvoked(Set<String> classPaths) {
        // Finding classToContainedMethodNames, hierarchy_parents, hierarchy_children,
        for (String classPath : classPaths) {
            try {
                ClassReader classReader = new ClassReader(new FileInputStream(new File(classPath)));
                ClassToMethodsCollectorCV classToMethodsVisitor = new ClassToMethodsCollectorCV(
                        classToContainedMethodNames, hierarchyParents, hierarchyChildren);
                classReader.accept(classToMethodsVisitor, ClassReader.SKIP_DEBUG);
            } catch (IOException exception) {
                LOGGER.log(Level.INFO, "[ERROR] cannot parse file: " + classPath);
                continue;
            }
        }

        // Finding methodNameToMethodNames map
        for (String classPath : classPaths) {
            try {
                ClassReader classReader = new ClassReader(new FileInputStream(new File(classPath)));
                MethodCallCollectorCV methodClassVisitor = new MethodCallCollectorCV(methodNameToMethodNames,
                        hierarchyParents, hierarchyChildren, classToContainedMethodNames);
                classReader.accept(methodClassVisitor, ClassReader.SKIP_DEBUG);
            } catch (IOException exception) {
                LOGGER.log(Level.INFO, "[ERROR] cannot parse file: " + classPath);
                continue;
            }
        }

        // deal with test class in a special way, all the @test method in hierarchy
        // should be considered
        for (String superClass : hierarchyChildren.keySet()) {
            if (superClass.contains("Test")) {
                for (String subClass : hierarchyChildren.getOrDefault(superClass, new HashSet<>())) {
                    for (String methodSig : classToContainedMethodNames.getOrDefault(superClass, new HashSet<>())) {
                        String subClassKey = subClass + "#" + methodSig;
                        String superClassKey = superClass + "#" + methodSig;
                        methodNameToMethodNames.computeIfAbsent(subClassKey, k -> new TreeSet<>()).add(superClassKey);
                    }
                }
            }
        }
    }

    /**
     * This function returns methodsCheckSum map.
     *
     * @return methodsCheckSum method to checksum mapping
     */
    public static Map<String, String> getMethodsCheckSum() {
        return methodsCheckSum;
    }

    /**
     * This function Computes and returns the methodToTestClasses map.
     *
     * @return methodToTestClasses method to test classes mapping
     */
    public static Map<String, Set<String>> computeMethodToTestClasses() {
        methodToTestClasses = invertMap(testClassesToMethods);
        return methodToTestClasses;
    }

    /**
     * This function computes methods checksums for the given classes and returns a
     * map containing them.
     *
     * @return methodsCheckSum method to checksum mapping
     */
    public static Map<String, String> getMethodsChecksumsForClasses(Set<String> classes, ClassLoader loader) {
        // Looping over all the classes, and computing the checksum for each method in
        // each class
        Map<String, String> computedMethodsChecksums = new HashMap<>();

        for (String className : classes) {
            // Reading the class file and parsing it
            String klas = ChecksumUtil.toClassOrJavaName(className, false);
            URL url = loader.getResource(klas);

            String path = url.getPath();
            ClassNode node = new ClassNode(Opcodes.ASM5);
            ClassReader reader = null;
            try {
                reader = new ClassReader(new FileInputStream(path));
            } catch (IOException exception) {
                LOGGER.log(Level.INFO, "[ERROR] reading class: " + path);
                continue;
            }

            String methodChecksum = null;
            reader.accept(node, ClassReader.SKIP_DEBUG);
            List<MethodNode> methods = node.methods;
            // Looping over all the methods in the class, and computing the checksum for
            // each method
            for (MethodNode method : methods) {
                String methodContent = ZLCHelperMethods.printMethodContent(method);
                try {
                    methodChecksum = ChecksumUtil.computeStringChecksum(methodContent);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }

                computedMethodsChecksums.put(
                        className + "#" + method.name + method.desc.substring(0, method.desc.indexOf(")") + 1),
                        methodChecksum);
            }
        }
        return computedMethodsChecksums;
    }

    /**
     * This function computes all classes in a project.
     *
     * @return classes set of classes in the project
     */
    public static Set<String> getClasses() {
        Set<String> classes = new HashSet<>();
        for (String className : classToContainedMethodNames.keySet()) {
            classes.add(className);
        }
        return classes;
    }

    /**
     * This function computes checksums for all classes in a project.
     *
     * @return classesChecksums mapping of classes to their checksums
     */
    public static Map<String, List<String>> computeClassesChecksums(ClassLoader loader, boolean cleanBytes) {
        // Loopig over all the classes, and computing the checksum for each class
        for (String className : classToContainedMethodNames.keySet()) {
            // Computing the checksum for the class file
            List<String> classPartsChecksums = new ArrayList<>();
            String klas = ChecksumUtil.toClassOrJavaName(className, false);
            URL url = loader.getResource(klas);
            String path = url.getPath();
            ChecksumUtil checksumUtil = new ChecksumUtil(cleanBytes);
            String classCheckSum = checksumUtil.computeSingleCheckSum(url);
            classPartsChecksums.add(classCheckSum);

            // Computing the checksum for the class headers
            ClassNode node = new ClassNode(Opcodes.ASM5);
            ClassReader reader = null;
            try {
                reader = new ClassReader(new FileInputStream(path));
            } catch (IOException exception) {
                LOGGER.log(Level.INFO, "[ERROR] reading class file: " + path);
                continue;
            }
            reader.accept(node, ClassReader.SKIP_DEBUG);

            String classHeaders = getClassHeaders(node);
            String headersCheckSum = null;
            try {
                headersCheckSum = ChecksumUtil.computeStringChecksum(classHeaders);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
            classPartsChecksums.add(headersCheckSum);
            classesChecksums.put(className, classPartsChecksums);
        }
        return classesChecksums;
    }

    // print class header info (e.g., access flags, inner classes, etc)
    public static String getClassHeaders(ClassNode node) {
        Printer printer = new Textifier(Opcodes.ASM5) {
            @Override
            public Textifier visitField(int access, String name, String desc,
                    String signature, Object value) {
                return new Textifier();
            }

            @Override
            public Textifier visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                return new Textifier();
            }
        };
        StringWriter sw = new StringWriter();
        TraceClassVisitor classPrinter = new TraceClassVisitor(null, printer,
                new PrintWriter(sw));
        node.accept(classPrinter);

        return sw.toString();
    }

    /*
     * This function computes the classes dependency graph.
     * It is a map from class to a set of classes that it depends on through
     * inheritance and uses
     */
    public static Map<String, Set<String>> constructClassesDependencyGraph() {
        for (Map.Entry<String, Set<String>> entry : methodNameToMethodNames.entrySet()) {
            String fromClass = entry.getKey().split("#")[0];
            Set<String> toClasses = new HashSet<>();

            for (String toMethod : entry.getValue()) {
                String toClass = toMethod.split("#")[0];
                toClasses.add(toClass);
            }

            if (classesDependencyGraph.containsKey(fromClass)) {
                classesDependencyGraph.get(fromClass).addAll(toClasses);
            } else {
                classesDependencyGraph.put(fromClass, toClasses);
            }
        }

        for (String className : hierarchyParents.keySet()) {
            Set<String> parents = hierarchyParents.get(className);
            classesDependencyGraph.getOrDefault(className, new HashSet<>()).addAll(parents);
        }
        addReflexiveClosure(classToContainedMethodNames);
        classesDependencyGraph = invertMap(classesDependencyGraph);

        return classesDependencyGraph;
    }

    /*
     * This function computes the testClassesToClasses graph.
     */
    public static Map<String, Set<String>> constuctTestClassesToClassesGraph() {
        for (String testClass : testClassesToMethods.keySet()) {
            Set<String> classes = new HashSet<>();
            for (String method : testClassesToMethods.get(testClass)) {
                String className = method.split("#")[0];
                classes.add(className);
            }
            testClassesToClasses.put(testClass, classes);
        }
        return testClassesToClasses;
    }

    /*
     * This function computes the classesToTestClasses graph.
     */
    public static Map<String, Set<String>> constructClassesToTestClassesGraph() {
        classesToTestClasses = invertMap(testClassesToClasses);
        return classesToTestClasses;
    }

    /**
     * This function computes checksums for all methods in a project.
     */
    public static Map<String, String> computeMethodsChecksum(ClassLoader loader) {
        // Looping over all the classes, and computing the checksum for each method in
        // each class
        for (String className : classToContainedMethodNames.keySet()) {
            String klas = ChecksumUtil.toClassOrJavaName(className, false);
            URL url = loader.getResource(klas);
            String path = url.getPath();
            ClassNode node = new ClassNode(Opcodes.ASM5);
            ClassReader reader = null;
            try {
                reader = new ClassReader(new FileInputStream(path));
            } catch (IOException exception) {
                LOGGER.log(Level.INFO, "[ERROR] reading class file: " + path);
                continue;
            }

            String methodChecksum = null;
            reader.accept(node, ClassReader.SKIP_DEBUG);
            List<MethodNode> methods = node.methods;
            // Looping over all the methods in the class, and computing the checksum for
            // each method
            for (MethodNode method : methods) {
                String methodContent = ZLCHelperMethods.printMethodContent(method);
                try {
                    methodChecksum = ChecksumUtil.computeStringChecksum(methodContent);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }

                methodsCheckSum.put(
                        className + "#" + method.name + method.desc.substring(0, method.desc.indexOf(")") + 1),
                        methodChecksum);
            }
        }
        return methodsCheckSum;
    }

    /**
     * This function computes the methodToTestMethods map.
     * For each method it computes all test methods that cover it.
     */
    public static Map<String, Set<String>> computeMethodToTestMethods() {
        // Looping over all the methods, and computing the test methods that cover each
        // method.
        for (String method : methodDependencyGraph.keySet()) {
            if (!method.contains("Test")) {
                Set<String> deps = getMethodDeps(method);
                Set<String> toRemove = new HashSet<>();

                for (String dep : deps) {
                    if (!dep.contains("Test")) {
                        toRemove.add(dep);
                    }
                }
                deps.removeAll(toRemove);
                methodToTestMethods.put(method, deps);
            }
        }
        return methodToTestMethods;
    }

    /**
     * This function computes all test methods in a project.
     *
     * @return testMethods set of methods in test classes
     */
    public static Set<String> getTestMethods() {
        Set<String> testMethods = new HashSet<>();
        for (String testMethod : methodsCheckSum.keySet()) {
            if (testMethod.contains("Test")) {
                testMethods.add(testMethod);
            }
        }
        return testMethods;
    }

    /**
     * This function add variable dependencies to the dependency graph.
     */
    private static void addVariableDepsToDependencyGraph() {
        /*
         * The original dependency graph is like this:
         * (A, B, C) are classes
         * (a) is a variable
         * A -> A, B, C
         * a -> A
         * After this function call the dependency graph will be like this:
         * A -> A, B, C, a
         * a -> A
         */
        for (String key : methodDependencyGraph.keySet()) {
            if (key.endsWith(")")) {
                continue;
            }

            Set<String> deps = methodDependencyGraph.get(key);
            for (String dep : deps) {
                methodDependencyGraph.get(dep).add(key);
            }
        }
    }

    public static Set<String> getDepsHelper(String testClass) {
        Set<String> visitedMethods = new TreeSet<>();
        // BFS
        ArrayDeque<String> queue = new ArrayDeque<>();

        // initialization
        for (String method : methodDependencyGraph.keySet()) {
            if (method.startsWith(testClass + "#")) {
                queue.add(method);
                visitedMethods.add(method);
            }
        }

        while (!queue.isEmpty()) {
            String currentMethod = queue.pollFirst();
            for (String invokedMethod : methodDependencyGraph.getOrDefault(currentMethod, new HashSet<>())) {
                if (!visitedMethods.contains(invokedMethod)) {
                    queue.add(invokedMethod);
                    visitedMethods.add(invokedMethod);
                }
            }
        }
        return visitedMethods;
    }

    // simple DFS
    public static void getDepsDFS(String methodName, Set<String> visitedMethods) {
        if (methodNameToMethodNames.containsKey(methodName)) {
            for (String method : methodNameToMethodNames.get(methodName)) {
                if (!visitedMethods.contains(method)) {
                    visitedMethods.add(method);
                    getDepsDFS(method, visitedMethods);
                }
            }
        }
    }

    public static Set<String> getClassDeps(String classSignature) {

        Set<String> visitedClasses = new HashSet<>();
        // BFS
        ArrayDeque<String> queue = new ArrayDeque<>();

        // initialization
        queue.add(classSignature);
        visitedClasses.add(classSignature);

        while (!queue.isEmpty()) {
            String currentClass = queue.pollFirst();
            for (String depClass : classesDependencyGraph.getOrDefault(currentClass, new HashSet<>())) {
                if (!visitedClasses.contains(depClass)) {
                    queue.add(depClass);
                    visitedClasses.add(depClass);
                }
            }
        }
        return visitedClasses;
    }

    public static Set<String> getMethodDeps(String methodSignature) {

        Set<String> visitedMethods = new HashSet<>();
        // BFS
        ArrayDeque<String> queue = new ArrayDeque<>();

        // initialization
        queue.add(methodSignature);
        visitedMethods.add(methodSignature);

        while (!queue.isEmpty()) {
            String currentMethod = queue.pollFirst();
            for (String invokedMethod : methodDependencyGraph.getOrDefault(currentMethod, new HashSet<>())) {
                if (!visitedMethods.contains(invokedMethod)) {
                    queue.add(invokedMethod);
                    visitedMethods.add(invokedMethod);
                }
            }
        }
        return visitedMethods;
    }

    public static Set<String> getDeps(String testClass) {
        Set<String> visited = new HashSet<>();
        for (String method : methodNameToMethodNames.keySet()) {
            if (method.startsWith(testClass + "#")) {
                visited.add(method);
                getDepsDFS(method, visited);
            }
        }
        return visited;
    }

    public static Map<String, Set<String>> getDepsSingleThread(Set<String> testClasses) {
        Map<String, Set<String>> testToMethods = new HashMap<>();
        for (String testClass : testClasses) {
            testToMethods.put(testClass, getDeps(testClass));
        }
        return testToMethods;
    }

    public static Map<String, Set<String>> getDepsMultiThread(Set<String> testClasses) {
        Map<String, Set<String>> testToMethods = new ConcurrentSkipListMap<>();
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(16);
            for (final String testClass : testClasses) {
                service.submit(() -> {
                    Set<String> invokedMethods = getDeps(testClass);
                    testToMethods.put(testClass, invokedMethods);
                    // numMethodDepNodes.addAll(invokedMethods);
                });
            }
            service.shutdown();
            service.awaitTermination(5, TimeUnit.MINUTES);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return testToMethods;
    }

    public static Set<String> getMethodsFromHierarchies(String currentMethod, Map<String, Set<String>> hierarchies) {
        Set<String> res = new HashSet<>();
        // consider the superclass/subclass, do not have to consider the constructors
        String currentMethodSig = currentMethod.split("#")[1];
        if (!currentMethodSig.startsWith("<init>") && !currentMethodSig.startsWith("<clinit>")) {
            String currentClass = currentMethod.split("#")[0];
            for (String hyClass : hierarchies.getOrDefault(currentClass, new HashSet<>())) {
                String hyMethod = hyClass + "#" + currentMethodSig;
                res.addAll(getMethodsFromHierarchies(hyMethod, hierarchies));
                res.add(hyMethod);
            }
        }
        return res;
    }

    /**
     * This function inverts the given map.
     */
    public static Map<String, Set<String>> invertMap(Map<String, Set<String>> mapToInvert) {
        // Think of a map as a graph represented as an adjacency list. This funcition
        // inverts the graph by inverting all edges.
        Map<String, Set<String>> map = mapToInvert;
        Map<String, Set<String>> invertedMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            Set<String> values = entry.getValue();
            for (String value : values) {
                if (!invertedMap.containsKey(value)) {
                    invertedMap.put(value, new HashSet<>());
                }
                invertedMap.get(value).add(key);
            }
        }
        return invertedMap;
    }

    /**
     * This function adds reflexive closure to the given map.
     *
     * @param mapToAddReflexiveClosure the passed mapping
     */
    public static void addReflexiveClosure(Map<String, Set<String>> mapToAddReflexiveClosure) {
        for (String method : mapToAddReflexiveClosure.keySet()) {
            mapToAddReflexiveClosure.get(method).add(method);
        }
    }

    /**
     * This function computes and returns the testClasses.
     *
     * @return testClasses
     */
    public static Set<String> computeTestClasses() {
        Set<String> testClasses = new HashSet<>();
        for (String testClass : testClassesToMethods.keySet()) {
            testClasses.add(testClass);
        }
        return testClasses;
    }

    /**
     * This function computes and returns the methods.
     *
     * @return methods
     */
    public static Set<String> computeMethods() {
        Set<String> methodSigs = new HashSet<>();
        for (String keyString : methodsCheckSum.keySet()) {
            methodSigs.add(keyString);
        }
        return methodSigs;
    }

    /**
     * This function removes any variables from dependency graphs.
     *
     * @return testMethods
     */
    public static void filterVariables() {
        // Filter out keys that are variables.
        methodDependencyGraph.keySet().removeIf(method -> !method.matches(".*\\(.*\\)"));

        // Filter values of methodName2MethodNames
        methodDependencyGraph.values()
                .forEach(methodList -> methodList.removeIf(method -> !method.matches(".*\\(.*\\)")));

        // Filter from test2methods
        testClassesToMethods.values()
                .forEach(methodList -> methodList.removeIf(method -> !method.matches(".*\\(.*\\)")));
    }

    /**
     * Experimental method to see if we can have a transitive closure of methods
     * here. Currently not used anywhere.
     *
     * @param changedMethod the method path that changed
     * @return methods
     */
    public static Set<String> findTransitiveClosure(String changedMethod) throws Exception {
        Set<String> impactedMethods = new HashSet<>();
        Stack<String> stack = new Stack<>();
        stack.push(changedMethod);

        while (!stack.isEmpty()) {
            String method = stack.pop();
            if (methodDependencyGraph.containsKey(method)) {
                Set<String> methodDeps = methodDependencyGraph.getOrDefault(method, new HashSet<>());
                for (String invokedMethod : methodDeps) {
                    impactedMethods.add(invokedMethod);
                    stack.push(invokedMethod);
                }
            } else {
                throw new Exception("Method not found in the dependency graph");
            }
        }

        return impactedMethods;
    }
}