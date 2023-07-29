// package edu.illinois.starts.smethods;


// import org.objectweb.asm.*;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;


// public class BytecodeChecksumClassVisitor extends ClassVisitor {

//     public BytecodeChecksumClassVisitor() {
//         super(Opcodes.ASM9);
//     }

//     @Override
//     public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
//         MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
//         return new MethodChecksum(methodVisitor, MessageDigest.getInstance("MD5"));
//     }

//     public static void run() {
//         String filePath = "/home/mustafa/Calculator.class"; // Replace with the path to your Java class file
//         Path path = Paths.get(filePath);

//         try {
//             byte[] bytes = Files.readAllBytes(path);

//             ClassReader classReader = new ClassReader(bytes);
//             BytecodeChecksumClassVisitor bytecodeChecksumClassVisitor = new BytecodeChecksumClassVisitor();
//             classReader.accept(bytecodeChecksumClassVisitor, 0);
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }
// }