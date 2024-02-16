package edu.illinois.starts.changelevel;

import org.ekstazi.asm.ClassReader;
import org.ekstazi.util.FileUtil;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class StartsChangeTypes implements Serializable, Comparable<StartsChangeTypes>{
    private static final long serialVersionUID = 1234567L;
    public transient static HashMap<String, Set<String>> hierarchyGraph;
    public transient TreeMap<String, String> instanceFieldMap;
    public transient TreeMap<String, String> staticFieldMap;
    public static transient long saveChangeTypes = 0;
    public static transient long numChangeTypes = 0;
    public static transient long sizeChangeTypes = 0;

    public TreeMap<String, String> constructorsMap;
    public Set<String> fieldList;
    public TreeMap<String, String> instanceMethodMap;
    public TreeMap<String, String> staticMethodMap;
    public String curClass = "";
    public String superClass = "";
    public String urlExternalForm = "";


    public StartsChangeTypes(){
        constructorsMap = new TreeMap<>();
        instanceMethodMap = new TreeMap<>();
        staticMethodMap = new TreeMap<>();
        instanceFieldMap = new TreeMap<>();
        staticFieldMap = new TreeMap<>();
        fieldList = new HashSet<>();
        curClass = "";
        superClass = "";
        urlExternalForm = "";
    }

    public static StartsChangeTypes fromFile(String fileName) throws IOException,ClassNotFoundException{
        StartsChangeTypes c = null;
        FileInputStream fileIn = null;
        if (!new File(fileName).exists()) {
            return null;
        }
        try {
            fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            c= (StartsChangeTypes) in.readObject();
            in.close();
            fileIn.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return c;
        }
        return c;
    }

    public static void toFile(String fileName, StartsChangeTypes c){
        numChangeTypes += 1;
        long toFileStart = System.currentTimeMillis();
        try {
            File file = new File(fileName);
            sizeChangeTypes += file.length();
            if (!file.exists()){
                File dir = new File(file.getParent());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            }else {
                file.delete();
            }
            FileOutputStream fileOut =
                    new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(c);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
        long toFileEnd = System.currentTimeMillis();
        saveChangeTypes += toFileEnd - toFileStart;
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final StartsChangeTypes other = (StartsChangeTypes) obj;

        boolean modified;

        // field changes
//        if (!sortedString(fieldList.toString()).equals(sortedString(other.fieldList.toString()))){
//            return false;
//        }
        if (fieldChange(fieldList, other.fieldList)){
            return false;
        }

        TreeMap<String, String> newConstructor = other.constructorsMap;
        TreeMap<String, String> oldConstructor = this.constructorsMap;

        if (newConstructor.size() != oldConstructor.size()){
            return false;
        }

        // constructor changes
        for (String s : newConstructor.keySet()){
            if (!oldConstructor.keySet().contains(s) || !newConstructor.get(s).equals(oldConstructor.get(s))){
                return false;
            }
        }

        // if there is method change
        boolean hasHierarchy = false;
        String newCurClass = other.curClass;
        String oldCurClass = this.curClass;
        if (StartsChangeTypes.hierarchyGraph.containsKey(newCurClass) || StartsChangeTypes.hierarchyGraph.containsKey(oldCurClass)){
            hasHierarchy =  true;
        }
        modified = methodChange(this.instanceMethodMap, other.instanceMethodMap, hasHierarchy) || methodChange(this.staticMethodMap, other.staticMethodMap, hasHierarchy);
        return !modified;
    }

    public static Set<String> listFiles(String dir) {
        Set<String> res = new HashSet<>();
        try {
            List<Path> pathList =  Files.find(Paths.get(dir), 999, (p, bfa) -> !bfa.isDirectory())
                    .collect(Collectors.toList());
            for(Path filePath : pathList){
                if(!filePath.getFileName().toString().endsWith("class")){
                    continue;
                }
                String curClassPath = filePath.getParent().toString()+"/"+filePath.getFileName().toString();
                res.add(curClassPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static void initHierarchyGraph(Set<String> classPaths){
        // subclass <-> superclasses
        HashMap<String, Set<String>> graph = new HashMap<>();
        try {
            for (String classPath : classPaths) {
                byte[] bytes = FileUtil.readFile(new File(classPath));
                ClassReader reader = new ClassReader(bytes);
                String curClassName = reader.getClassName();
                String superClassName = reader.getSuperName();
                if (superClassName == null || superClassName.equals("java/lang/Object")){
                    continue;
                }else{
                    Set<String> h = graph.getOrDefault(curClassName, new HashSet<>());
                    h.add(superClassName);
                    graph.put(curClassName, h);

                    h = graph.getOrDefault(superClassName, new HashSet<>());
                    h.add(curClassName);
                    graph.put(superClassName, h);
                }
            }
        }catch(Exception e){

        }
        if (hierarchyGraph == null){
            hierarchyGraph = new HashMap<>();
        }
        hierarchyGraph.putAll(graph);
    //    System.out.println("[log]hierarchyGraph: "+hierarchyGraph.keySet());
    }

    private boolean fieldChange(Set<String> newFields, Set<String> oldFields){
        Set<String> preFieldList = new HashSet<>(oldFields);
        Set<String> curFieldList = new HashSet<>(newFields);

        for (String preField : oldFields){
            curFieldList.remove(preField);
        }
        for (String curField : newFields){
            preFieldList.remove(curField);
        }

        if (preFieldList.size() == 0 || curFieldList.size() == 0){
            return false;
        }else{
            return true;
        }
    }

    private boolean methodChange(TreeMap<String, String> oldMethodsPara, TreeMap<String, String> newMethodsPara, boolean hasHierarchy){
        TreeMap<String, String> oldMethods = new TreeMap<>(oldMethodsPara);
        TreeMap<String, String> newMethods = new TreeMap<>(newMethodsPara);
       
        Set<String> methodSig = new HashSet<>(oldMethods.keySet());
        methodSig.addAll(newMethods.keySet());
        for (String sig : methodSig){
            if (oldMethods.containsKey(sig) && newMethods.containsKey(sig)){
                if (oldMethods.get(sig).equals(newMethods.get(sig))) {
                    oldMethods.remove(sig);
                    newMethods.remove(sig);
                }else{
                    return true;
                }
            } else if (oldMethods.containsKey(sig) && newMethods.containsValue(oldMethods.get(sig))){
                newMethods.values().remove(oldMethods.get(sig));
                oldMethods.remove(sig);
            } else if (newMethods.containsKey(sig) && oldMethods.containsValue(newMethods.get(sig))){
                oldMethods.values().remove(newMethods.get(sig));
                newMethods.remove(sig);
            }
        }

        if (oldMethods.size() == 0 && newMethods.size() == 0){
            return false;
        }

        // one methodmap is empty then the left must be added or deleted.
        if (!hasHierarchy && (oldMethods.size() == 0 || newMethods.size() == 0)){
            // the class is test class and the test class adds/revises test methods
            // if (testClasses.contains(this.curClass) && newMethods.size()>0){
            if (this.curClass.contains("Test") && newMethods.size()>0){
                return true;
            }
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(StartsChangeTypes o) {
        return this.urlExternalForm.compareTo(o.urlExternalForm);
    }

    @Override
    public String toString() {
        return "StartsChangeTypes{" +
                ", curClass='" + curClass + '\'' +
                ", constructorsMap=" + constructorsMap +
                ", instanceMethodsMap=" + instanceMethodMap +
                ", staticMethodMap=" + staticMethodMap +
                '}';
    }
}
