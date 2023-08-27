package edu.illinois.starts.smethods;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.illinois.starts.constants.StartsConstants;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassToMethodsCollectorCV extends ClassVisitor implements StartsConstants {

    Map<String, Set<String>> class2ContainedMethodNames;
    Map<String, Set<String>> hierarchyParents;
    Map<String, Set<String>> hierarchyChildren;

    // Name of the class being visited.
    private String methodClassName;

    public ClassToMethodsCollectorCV(Map<String, Set<String>> class2ContainedMethodNames,
            Map<String, Set<String>> hierarchyParents,
            Map<String, Set<String>> hierarchyChildren) {
        super(ASM_VERSION);
        this.class2ContainedMethodNames = class2ContainedMethodNames;
        this.hierarchyParents = hierarchyParents;
        this.hierarchyChildren = hierarchyChildren;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        methodClassName = name;
        Set<String> parents = hierarchyParents.getOrDefault(name, new HashSet<>());
        if (superName != null && !superName.startsWith("java/") && !superName.startsWith("org/junit/")
                && !superName.startsWith(PROJECT_PACKAGE)) {
            parents.add(superName);
            Set<String> subClasses = hierarchyChildren.getOrDefault(superName, new HashSet<>());
            subClasses.add(name);
            hierarchyChildren.put(superName, subClasses);
        }
        for (String i : interfaces) {
            parents.add(i);
            Set<String> subClasses = hierarchyChildren.getOrDefault(i, new HashSet<>());
            subClasses.add(name);
            hierarchyChildren.put(i, subClasses);
        }
        hierarchyParents.put(name, parents);
    }

    @Override
    public MethodVisitor visitMethod(int access, final String outerName, final String outerDesc, String signature,
            String[] exceptions) {
        // append arguments to key, remove what after ) of desc
        if (outerName.equals(PROJECT_PACKAGE)) {
            return null;
        }
        String method = outerName + outerDesc.substring(0, outerDesc.indexOf(")") + 1);
        Set<String> methods = class2ContainedMethodNames.getOrDefault(methodClassName, new TreeSet<>());
        methods.add(method);
        class2ContainedMethodNames.put(methodClassName, methods);
        MethodVisitor methodVisitor = super.visitMethod(access, outerName, outerDesc, signature, exceptions);
        return methodVisitor;
    }

}
