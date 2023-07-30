package edu.illinois.starts.smethods;

import org.objectweb.asm.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

class MethodChecksum extends MethodVisitor {

    private MessageDigest messageDigest;
    private String methodKey;
    private Map<String, String> methodCheckSum;

    public MethodChecksum(MethodVisitor methodVisitor, String methodKey, Map<String, String> methodCheckSum) {
        super(Opcodes.ASM9, methodVisitor);
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        this.methodKey = methodKey;
        this.methodCheckSum = methodCheckSum;
        byte[] methodBytes = methodKey.getBytes();
        messageDigest.update(methodBytes);
    }

    @Override
    public void visitTypeInsn(int opcode, String desc) {
        super.visitTypeInsn(opcode, desc);
        String temp = opcode + desc;
        byte[] bytes = temp.getBytes();
        messageDigest.update(bytes);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        String temp = opcode + "" + operand;
        byte[] bytes = temp.getBytes();
        messageDigest.update(bytes);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        String temp = opcode + "" + var;
        byte[] bytes = temp.getBytes();
        messageDigest.update(bytes);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        String temp = opcode + "";
        byte[] bytes = temp.getBytes();
        messageDigest.update(bytes);
    }

    @Override
    public void visitFieldInsn(int opc, String owner, String name, String desc) {
        super.visitFieldInsn(opc, owner, name, desc);
        String temp = opc + owner + name + desc;
        byte[] bytes = temp.getBytes();
        messageDigest.update(bytes);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        String temp = opcode + owner + name + descriptor + isInterface;
        byte[] bytes = temp.getBytes();
        messageDigest.update(bytes);
    }

    @Override
    public void visitEnd() {
        byte[] checksumBytes = messageDigest.digest();
        String checksum = bytesToHex(checksumBytes);
        methodCheckSum.put(methodKey, checksum);
        super.visitEnd();
    }

    // Helper method to convert byte array to hexadecimal string
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}