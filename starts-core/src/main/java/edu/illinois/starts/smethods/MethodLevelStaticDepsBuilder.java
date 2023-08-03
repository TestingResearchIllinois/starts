package edu.illinois.starts.smethods;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import edu.illinois.starts.util.ChecksumUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import edu.illinois.starts.helpers.ZLCHelperMethods;

public class MethodLevelStaticDepsBuilder {
    // mvn exec:java -Dexec.mainClass=org.smethods.MethodLevelStaticDepsBuilder
    // -Dexec.args="path to test project"

    // for every class, get the methods it implements
    public static Map<String, Set<String>> class2ContainedMethodNames = new HashMap<>();
    // for every method, get the methods it invokes
    public static Map<String, Set<String>> methodName2MethodNames = new HashMap<>();
    // for every class, find its parents.
    public static Map<String, Set<String>> hierarchy_parents = new HashMap<>();
    // for every class, find its children.
    public static Map<String, Set<String>> hierarchy_children = new HashMap<>();

    public static Map<String, Set<String>> test2methods = new HashMap<>();

    public static Map<String, Set<String>> method2tests = new HashMap<>();

    public static Map<String, String> methodsCheckSums = new HashMap<>();

    public static  Map<String, Set<String>> methods2testmethods = new HashMap<>();

    public static  Map<String, String> classesChecksums = new HashMap<>();

    public static void buildMethodsGraph(ClassLoader loader) throws Exception {
        // find all the class files
        HashSet<String> classPaths = new HashSet<>(Files.walk(Paths.get("."))
                .filter(Files::isRegularFile)
                .filter(f -> (f.toString().endsWith(".class") && f.toString().contains("target")))
                .map(f -> f.normalize().toAbsolutePath().toString())
                .collect(Collectors.toList()));

        // Find the methods that each method calls
        findMethodsinvoked(classPaths);

        // Suppose that test classes have Test in their class name
        Set<String> testClasses = new HashSet<>();
        for (String method : methodName2MethodNames.keySet()) {
            String className = method.split("#|\\$")[0];
            if (className.contains("Test")) {
                testClasses.add(className);
            }
        }

        test2methods = getDepsSingleThread(testClasses);

        // Filter out the variables in the graph. We only need methods.
        addReflexiveClosure();
        methodName2MethodNames = invertMap(methodName2MethodNames);
        addVariableDeps();
        method2tests = invertMap(test2methods);


    }





    public static Map<String, String> getMethodsChecksumsForClasses(Set<String> classes, ClassLoader loader){
        for (String className: classes){
            // String klas = ChecksumUtil.toClassName(methodPath.split("#")[0]);
            String klas = ChecksumUtil.toClassName(className);
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
                String methodContent = ZLCHelperMethods.printMethodContent(method);
                try {
                    methodChecksum = ChecksumUtil.computeMethodChecksum(methodContent);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                methodsCheckSums.put(
                        className + "#" + method.name + method.desc.substring(0, method.desc.indexOf(")") + 1),
                        methodChecksum);
            }
        }
        return methodsCheckSums;
    }






    public static Set<String> getClasses(){
        Set<String> classes = new HashSet<>();
        for (String className: class2ContainedMethodNames.keySet()){
            classes.add(className);
        }
        return classes;
    }


    public static void computeClassesChecksums(ClassLoader loader, boolean cleanBytes){
        for (String className: class2ContainedMethodNames.keySet()){
            String klas = ChecksumUtil.toClassName(className);
            URL url = loader.getResource(klas);
            ChecksumUtil checksumUtil = new ChecksumUtil(cleanBytes);
            String checkSum = checksumUtil.computeSingleCheckSum(url);
            classesChecksums.put(className, checkSum);
        }
    }

    public static void computeChecksums(ClassLoader loader) {
        for (String class_name : class2ContainedMethodNames.keySet()) {
            // String klas = ChecksumUtil.toClassName(methodPath.split("#")[0]);
            String klas = ChecksumUtil.toClassName(class_name);
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
                String methodContent = ZLCHelperMethods.printMethodContent(method);
                try {
                    methodChecksum = ChecksumUtil.computeMethodChecksum(methodContent);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                methodsCheckSums.put(
                        class_name + "#" + method.name + method.desc.substring(0, method.desc.indexOf(")") + 1),
                        methodChecksum);
            }
        }
    }



    public static void computeMethod2TestMethods(){
        for (String method : methodName2MethodNames.keySet()){
            if (!method.contains("Test")){
                Set<String> deps = getMethodDeps(method);
                Set<String> to_remove = new HashSet<>();

                for (String dep : deps){
                    if (!dep.contains("Test")){
                        to_remove.add(dep);
                    }
                }
                deps.removeAll(to_remove);
                methods2testmethods.put(method, deps);
            }
        }
    }


    public static Set<String> getTestMethods(){
        Set<String> testMethods = new HashSet<>();
        for (String testMethod : methodsCheckSums.keySet()){
            if (testMethod.contains("Test")){
                testMethods.add(testMethod);
            }
        }
        return testMethods;
    }



    public static void addVariableDeps() {
        for (String key : methodName2MethodNames.keySet()) {
            if (key.endsWith(")")) continue;
            
            Set<String> deps = methodName2MethodNames.get(key);
            for (String dep : deps) {
                methodName2MethodNames.get(dep).add(key);
            }

        }
    }

    public static void findMethodsinvoked(Set<String> classPaths) {
        // finds class2ContainedMethodNames, hierarchy_parents, hierarchy_children,
        // methodCheckSum
        for (String classPath : classPaths) {
            try {
                ClassReader classReader = new ClassReader(new FileInputStream(new File(classPath)));
                ClassToMethodsCollectorCV classToMethodsVisitor = new ClassToMethodsCollectorCV(
                        class2ContainedMethodNames, hierarchy_parents, hierarchy_children);
                classReader.accept(classToMethodsVisitor, ClassReader.SKIP_DEBUG);
            } catch (IOException e) {
                System.out.println("Cannot parse file: " + classPath);
                continue;
            }
        }

        // System.out.println("class2ContainedMethodNames: " +
        // class2ContainedMethodNames);
        // System.out.println("hierarchy_parents: " + hierarchy_parents);
        // System.out.println("hierarchy_children: " + hierarchy_children);
        // System.out.println("methodCheckSum: " + methodCheckSum);

        for (String classPath : classPaths) {
            try {
                ClassReader classReader = new ClassReader(new FileInputStream(new File(classPath)));
                MethodCallCollectorCV methodClassVisitor = new MethodCallCollectorCV(methodName2MethodNames,
                        hierarchy_parents, hierarchy_children, class2ContainedMethodNames);
                classReader.accept(methodClassVisitor, ClassReader.SKIP_DEBUG);
            } catch (IOException e) {
                System.out.println("Cannot parse file: " + classPath);
                continue;
            }
        }

        // deal with test class in a special way, all the @test method in hierarchy
        // should be considered
        for (String superClass : hierarchy_children.keySet()) {
            if (superClass.contains("Test")) {
                for (String subClass : hierarchy_children.getOrDefault(superClass, new HashSet<>())) {
                    for (String methodSig : class2ContainedMethodNames.getOrDefault(superClass, new HashSet<>())) {
                        String subClassKey = subClass + "#" + methodSig;
                        String superClassKey = superClass + "#" + methodSig;
                        methodName2MethodNames.computeIfAbsent(subClassKey, k -> new TreeSet<>()).add(superClassKey);
                    }
                }
            }
        }
    }

    public static Set<String> getDepsHelper(String testClass) {
        Set<String> visitedMethods = new TreeSet<>();
        // BFS
        ArrayDeque<String> queue = new ArrayDeque<>();

        // initialization
        for (String method : methodName2MethodNames.keySet()) {
            if (method.startsWith(testClass + "#")) {
                queue.add(method);
                visitedMethods.add(method);
            }
        }

        while (!queue.isEmpty()) {
            String currentMethod = queue.pollFirst();
            for (String invokedMethod : methodName2MethodNames.getOrDefault(currentMethod, new HashSet<>())) {
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
        if (methodName2MethodNames.containsKey(methodName)) {
            for (String method : methodName2MethodNames.get(methodName)) {
                if (!visitedMethods.contains(method)) {
                    visitedMethods.add(method);
                    getDepsDFS(method, visitedMethods);
                }
            }
        }
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
            for (String invokedMethod : methodName2MethodNames.getOrDefault(currentMethod, new HashSet<>())) {
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
        for (String method : methodName2MethodNames.keySet()) {
            if (method.startsWith(testClass + "#")) {
                visited.add(method);
                getDepsDFS(method, visited);
            }
        }
        return visited;
    }

    public static Map<String, Set<String>> getDepsSingleThread(Set<String> testClasses) {
        Map<String, Set<String>> test2methods = new HashMap<>();
        for (String testClass : testClasses) {
            test2methods.put(testClass, getDeps(testClass));
        }
        return test2methods;
    }

    public static Map<String, Set<String>> getDepsMultiThread(Set<String> testClasses) {
        Map<String, Set<String>> test2methods = new ConcurrentSkipListMap<>();
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(16);
            for (final String testClass : testClasses) {
                service.submit(() -> {
                    Set<String> invokedMethods = getDeps(testClass);
                    test2methods.put(testClass, invokedMethods);
                    // numMethodDepNodes.addAll(invokedMethods);
                });
            }
            service.shutdown();
            service.awaitTermination(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return test2methods;
    }

    public static Set<String> getMethodsFromHierarchies(String currentMethod, Map<String, Set<String>> hierarchies) {
        Set<String> res = new HashSet<>();
        // consider the superclass/subclass, do not have to consider the constructors
        String currentMethodSig = currentMethod.split("#")[1];
        if (!currentMethodSig.startsWith("<init>") && !currentMethodSig.startsWith("<clinit>")) {
            String currentClass = currentMethod.split("#")[0];
            for (String hClass : hierarchies.getOrDefault(currentClass, new HashSet<>())) {
                String hMethod = hClass + "#" + currentMethodSig;
                res.addAll(getMethodsFromHierarchies(hMethod, hierarchies));
                res.add(hMethod);
            }
        }
        return res;
    }

    public static void filterVariables() {
        // Filter out keys that are variables.
        methodName2MethodNames.keySet().removeIf(method -> !method.matches(".*\\(.*\\)"));

        // Filter values of methodName2MethodNames
        methodName2MethodNames.values()
                .forEach(methodList -> methodList.removeIf(method -> !method.matches(".*\\(.*\\)")));

        // Filter from test2methods
        test2methods.values().forEach(methodList -> methodList.removeIf(method -> !method.matches(".*\\(.*\\)")));
    }

    public static Map<String, Set<String>> invertMap(Map<String, Set<String>> mapToInvert) {
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

    public static void addReflexiveClosure() {
        for (String method : methodName2MethodNames.keySet()) {
            methodName2MethodNames.get(method).add(method);
        }
    }

    public static Set<String> getTests() {
        Set<String> tests = new HashSet<>();
        for (String test : test2methods.keySet()) {
            tests.add(test);
        }
        return tests;
    }

    public static Set<String> getMethods() {
        Set<String> methodSigs = new HashSet<>();
        for (String keyString : methodsCheckSums.keySet()) {
            methodSigs.add(keyString);
        }
        return methodSigs;
    }
}