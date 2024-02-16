package edu.illinois.starts.smethods;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.illinois.starts.constants.StartsConstants;

public class ClassToMethodsCollectorCV extends ClassVisitor implements StartsConstants {

    // Mapping from class name to the set of methods contained in the class.
    Map<String, Set<String>> classToContainedMethodNames;

    // for every class, find its parents.
    Map<String, Set<String>> hierarchyParents;

    // for every class, find its children.
    Map<String, Set<String>> hierarchyChildren;

    // Name of the class being visited.
    private String methodClassName;

    public ClassToMethodsCollectorCV(Map<String, Set<String>> class2ContainedMethodNames,
                                     Map<String, Set<String>> hierarchyParents,
                                     Map<String, Set<String>> hierarchyChildren) {
        super(ASM_VERSION);
        this.classToContainedMethodNames = class2ContainedMethodNames;
        this.hierarchyParents = hierarchyParents;
        this.hierarchyChildren = hierarchyChildren;
    }

    /**
     * Visits the version and get information of a class.
     *
     * @param version    The version of the class file format being visited.
     * @param access     The access flags of the class being visited (see
     *                   {@link org.objectweb.asm.Opcodes}).
     * @param name       The internal name of the class
     * @param signature  The signature of this class. May be null if the class is
     *                   not a generic one, and does not extend or implement generic
     *                   classes or interfaces.
     * @param superName  The internal name of the super class.
     * @param interfaces The internal names of the implemented interfaces.
     */
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

    /**
     * Visits a method of the visited class. This method is called when the visit of
     * the method's code is just about to start.
     *
     * @param access     The method's access flags (see
     *                   {@link org.objectweb.asm.Opcodes}).
     * @param outerName  The method's name.
     * @param outerDesc  The method's descriptor
     * @param signature  The method's signature.
     * @param exceptions The internal names of the method's exception classes
     * @return An object to visit the byte code of the method's code, or null
     */
    @Override
    public MethodVisitor visitMethod(int access, final String outerName, final String outerDesc, String signature,
                                     String[] exceptions) {
        // append arguments to key, remove what after ) of desc
        if (outerName.equals(PROJECT_PACKAGE)) {
            return null;
        }
        String method = outerName + outerDesc.substring(0, outerDesc.indexOf(")") + 1);
        Set<String> methods = classToContainedMethodNames.getOrDefault(methodClassName, new TreeSet<>());
        methods.add(method);
        classToContainedMethodNames.put(methodClassName, methods);
        MethodVisitor methodVisitor = super.visitMethod(access, outerName, outerDesc, signature, exceptions);
        return methodVisitor;
    }

}
