package edu.illinois.starts.smethods;

import org.objectweb.asm.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



class MethodChecksum extends MethodVisitor {

    private MessageDigest messageDigest;

    public MethodChecksum(MethodVisitor methodVisitor,MessageDigest messageDigest) {
        super(Opcodes.ASM9, methodVisitor);
        this.messageDigest = messageDigest;
        if (this.messageDigest == null){
            
        }
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        byte[] opcodeBytes = new byte[]{(byte) (opcode & 0xFF)};
        messageDigest.update(opcodeBytes);
    }

    @Override
    public void visitEnd() {
        byte[] checksumBytes = messageDigest.digest();
        String checksum = bytesToHex(checksumBytes);
        System.out.println("Checksum: " + checksum);
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