package edu.illinois.starts.smethods;

import static edu.illinois.starts.smethods.Macros.ASM_VERSION;
import static edu.illinois.starts.smethods.Macros.PROJECT_PACKAGE;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodCallCollectorCV extends ClassVisitor {

    Map<String, Set<String>> methodName2InvokedMethodNames;
    Map<String, Set<String>> hierarchyParents;
    Map<String, Set<String>> hierarchyChildren;
    Map<String, Set<String>> class2ContainedMethodNames;
    Set<String> classesInConstantPool;

    // Name of the class being visited.
    private String methodClassName;

    public MethodCallCollectorCV(Map<String, Set<String>> methodName2MethodNames,
            Map<String, Set<String>> hierarchyParents,
            Map<String, Set<String>> hierarchyChildren,
            Map<String, Set<String>> class2ContainedMethodNames,
            Set<String> classesInConstantPool) {
        super(ASM_VERSION);
        this.methodName2InvokedMethodNames = methodName2MethodNames;
        this.hierarchyParents = hierarchyParents;
        this.hierarchyChildren = hierarchyChildren;
        this.class2ContainedMethodNames = class2ContainedMethodNames;
        this.classesInConstantPool = classesInConstantPool;
    }

    public MethodCallCollectorCV(Map<String, Set<String>> methodName2MethodNames,
            Map<String, Set<String>> hierarchyParents,
            Map<String, Set<String>> hierarchyChildren,
            Map<String, Set<String>> class2ContainedMethodNames) {
        super(ASM_VERSION);
        this.methodName2InvokedMethodNames = methodName2MethodNames;
        this.hierarchyParents = hierarchyParents;
        this.hierarchyChildren = hierarchyChildren;
        this.class2ContainedMethodNames = class2ContainedMethodNames;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        methodClassName = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, final String outerName, final String outerDesc, String signature,
            String[] exceptions) {
        // append arguments to key, remove what after ) of desc
        if (outerName.equals(PROJECT_PACKAGE)) {
            return null;
        }
        String outerMethodSig = outerName + outerDesc.substring(0, outerDesc.indexOf(")") + 1);
        String key = methodClassName + "#" + outerMethodSig;
        Set<String> methodInvokedMethods = methodName2InvokedMethodNames.computeIfAbsent(key, k -> new TreeSet<>());
        return new MethodVisitor(ASM_VERSION) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (!owner.startsWith("java/") && !owner.startsWith("org/junit/")
                        && !owner.startsWith(PROJECT_PACKAGE)) {
                    String methodSig = name + desc.substring(0, desc.indexOf(")") + 1);
                    if (class2ContainedMethodNames.getOrDefault(owner, new HashSet<>()).contains(methodSig)) {
                        String invokedKey = owner + "#" + methodSig;
                        methodInvokedMethods.add(invokedKey);
                    } else {
                        // find the first parent that implements the method
                        String firstParent = findFirstParent(owner, methodSig);
                        if (!firstParent.equals("")) {
                            String invokedKey = firstParent + "#" + methodSig;
                            methodInvokedMethods.add(invokedKey);
                        }
                    }
                    if (!methodSig.startsWith("<init>") && !methodSig.startsWith("<clinit>")) {
                        for (String subClass : hierarchyChildren.getOrDefault(owner, new HashSet<>())) {
                            if (class2ContainedMethodNames.getOrDefault(subClass, new HashSet<>())
                                    .contains(methodSig)) {
                                String invokedKey = subClass + "#" + methodSig;
                                methodInvokedMethods.add(invokedKey);
                            }
                        }
                    }
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if (!owner.startsWith("java/") && !owner.startsWith("org/junit/")
                        && !owner.startsWith(PROJECT_PACKAGE)) {
                    String field = owner + "#" + name;
                    // outerDesc.equals("<init>")
                    // non static field would be invoked through constructor
                    if ((opcode == Opcodes.PUTSTATIC && outerName.equals("<clinit>"))
                            || (opcode == Opcodes.PUTFIELD && outerName.equals("<init>"))) {
                        // || hierarchies.getOrDefault(mClassName, new HashSet<>()).contains(owner)
                        if (owner.equals(methodClassName)) {
                            Set methods = methodName2InvokedMethodNames.getOrDefault(field, new HashSet<>());
                            methods.add(key);
                            methodName2InvokedMethodNames.put(field, methods);
                            // method2usage.computeIfAbsent(key, k -> new TreeSet<>()).add(field);
                        }
                    }
                    methodInvokedMethods.add(field);
                    // method2usage.computeIfAbsent(field, k -> new TreeSet<>()).add(key);
                }
                // Opcodes.GETFIELD, Opcodes.PUTFIELD, Opcodes.GETSTATIC, Opcodes.PUTSTATIC
                super.visitFieldInsn(opcode, owner, name, desc);
            }

        };
    }

    public String findFirstParent(String currentClass, String methodSig) {
        for (String parent : hierarchyParents.getOrDefault(currentClass, new HashSet<>())) {
            if (class2ContainedMethodNames.getOrDefault(parent, new HashSet<>()).contains(methodSig)) {
                return parent;
            } else {
                String firstParent = findFirstParent(parent, methodSig);
                if (!firstParent.equals("")) {
                    return firstParent;
                }
            }
        }
        return "";
    }
}