package edu.illinois.starts.maven;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import edu.illinois.starts.asm.ClassReader;
import edu.illinois.starts.asm.ClassVisitor;
import edu.illinois.starts.asm.ClassWriter;
import edu.illinois.starts.asm.MethodVisitor;
import edu.illinois.starts.asm.Opcodes;
import edu.illinois.starts.constants.StartsConstants;

/** This class is from Ekstazi. **/

public final class MavenCFT implements ClassFileTransformer, StartsConstants {

    private static class MavenMethodVisitor extends MethodVisitor {
        private final String mavenMethodName;
        private final String mavenMethodDesc;

        private final String mavenInterceptorName;

        public MavenMethodVisitor(String interceptorName, String methodName, String methodDesc, MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
            this.mavenInterceptorName = interceptorName;
            this.mavenMethodName = methodName;
            this.mavenMethodDesc = methodDesc;
        }

        public void visitCode() {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, mavenInterceptorName, mavenMethodName,
                    mavenMethodDesc.replace("(", "(Ljava/lang/Object;"), false);
            mv.visitCode();
        }
    }

    private static class MavenClassVisitor extends ClassVisitor {
        private final String mavenClassName;
        private final String mavenInterceptorName;

        public MavenClassVisitor(String className, String interceptorName, ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
            this.mavenClassName = className;
            this.mavenInterceptorName = interceptorName;
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (name.equals(EXECUTE_MNAME) && desc.equals(EXECUTE_MDESC)) {
                mv = new MavenMethodVisitor(mavenInterceptorName, name, desc, mv);
            }
            return mv;
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className.equals(ABSTRACT_SUREFIRE_MOJO_VM) || className.equals(SUREFIRE_PLUGIN_VM)) {
            return addInterceptor(className, classfileBuffer, SUREFIRE_INTERCEPTOR_CLASS_VM);
        } else {
            return null;
        }
    }

    private byte[] addInterceptor(String className, byte[] classfileBuffer, String interceptorName) {
        ClassReader classReader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new MavenClassVisitor(className, interceptorName, classWriter);
        classReader.accept(visitor, 0);
        return classWriter.toByteArray();
    }
}

