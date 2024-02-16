/*
 * Copyright 2014-present Milos Gligoric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.illinois.starts.changelevel;

import org.ekstazi.asm.*;
import org.ekstazi.asm.signature.SignatureReader;

import java.util.HashMap;
import java.util.Map;

/**
 * Removes debug info from a class file. Visits a class file and keeps only the
 * most relevant parts of it (for hashing). For example, this class ignores
 * constant pool (and the order of constants in the pool).
 */
public class CleanCodeUtil extends Printer {

    /**
     * The label names. This map associate String values to Label keys.
     */
    protected Map<Label, String> labelNames;

    public CleanCodeUtil() {
        this(Opcodes.ASM6);
    }

    /**
     * Constructs a new {@link CleanCodeUtil}.
     *
     * @param api
     *            the ASM API version implemented by this visitor. Must be one
     *            of {@link Opcodes#ASM4}, {@link Opcodes#ASM5} or {@link Opcodes#ASM6}.
     */
    protected CleanCodeUtil(final int api) {
        super(api);
    }

    // ------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------

    @Override
    public void visit(final int version, final int access, final String name,
                      final String signature, final String superName,
                      final String[] interfaces) {
        if ((access & Opcodes.ACC_MODULE) != 0) {
            // visitModule will print the module
            return;
        }
        buf.setLength(0);
        buf.append(version);
        buf.append(access);
        buf.append(name);
        buf.append(superName);
        if (interfaces != null && interfaces.length > 0) {
            for (String el : interfaces) {
                buf.append(el);
            }
        }
        text.add(buf.toString());
    }

    @Override
    public void visitSource(final String file, final String debug) {
        buf.setLength(0);
        if (file != null) {
            buf.append(file);
        }
        if (debug != null) {
            buf.append(debug);
        }
        if (buf.length() > 0) {
            text.add(buf.toString());
        }
    }

    @Override
    public Printer visitModule(final String name, final int access,
                               final String version) {
        buf.setLength(0);
        buf.append(access);
        buf.append(name);
        buf.append(version);
        text.add(buf.toString());
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        return c;
    }

    @Override
    public void visitOuterClass(final String owner, final String name,
                                final String desc) {
        buf.setLength(0);
        buf.append(owner);
        buf.append(name);
        buf.append(desc);
        text.add(buf.toString());
    }

    @Override
    public CleanCodeUtil visitClassAnnotation(final String desc,
                                                                   final boolean visible) {
        return visitAnnotation(desc, visible);
    }

    @Override
    public Printer visitClassTypeAnnotation(int typeRef, TypePath typePath,
                                            String desc, boolean visible) {
        return visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public void visitClassAttribute(final Attribute attr) {
        visitAttribute(attr);
    }

    @Override
    public void visitInnerClass(final String name, final String outerName,
                                final String innerName, final int access) {
        buf.setLength(0);
        buf.append(access);
        buf.append(name);
        buf.append(outerName);
        buf.append(innerName);
        text.add(buf.toString());
    }

    @Override
    public CleanCodeUtil visitField(final int access, final String name,
                                    final String desc, final String signature, final Object value) {
        buf.setLength(0);
        buf.append(access);
        buf.append(name);
        buf.append(desc);
        //TODO signature may always be null if there is no generic type
        buf.append(signature);
        //TODO value of field change should always be file level?
        if (value != null) {
            buf.append(value);
        }
        text.add(buf.toString());

        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        return c;
    }

    @Override
    public CleanCodeUtil visitMethod(final int access, final String name,
                                                          final String desc, final String signature, final String[] exceptions) {
        buf.setLength(0);
        buf.append(access);
        buf.append(name);
        buf.append(desc);
        buf.append(signature);
        if (exceptions != null) {
            for (String el : exceptions) {
                buf.append(el);
            }
        }
        text.add(buf.toString());
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        return c;
    }

    @Override
    public void visitClassEnd() {
    }

    // ------------------------------------------------------------------------
    // Module
    // ------------------------------------------------------------------------

    @Override
    public void visitMainClass(String mainClass) {
        buf.setLength(0);
        buf.append(mainClass);
        text.add(buf.toString());
    }

    @Override
    public void visitPackage(String packaze) {
        buf.setLength(0);
        buf.append(packaze);
        text.add(buf.toString());
    }

    @Override
    public void visitRequire(String require, int access, String version) {
        buf.setLength(0);
        buf.append(access);
        buf.append(require);
        if (version != null) {
            buf.append(version);
        }
        text.add(buf.toString());
    }

    @Override
    public void visitExport(String export, int access, String... modules) {
        buf.setLength(0);
        buf.append(export);
        buf.append(access);
        if (modules != null) {
            for (String module: modules){
                buf.append(module);
            }
        }
        text.add(buf.toString());
    }

    @Override
    public void visitOpen(String export, int access, String... modules) {
        buf.setLength(0);
        buf.append(export);
        buf.append(access);
        if (modules != null) {
            for(String module : modules){
                buf.append(module);
            }
        }
        text.add(buf.toString());
    }

    @Override
    public void visitUse(String use) {
        buf.setLength(0);
        buf.append(use);
        text.add(buf.toString());
    }

    @Override
    public void visitProvide(String provide, String... providers) {
        buf.setLength(0);
        buf.append(provide);
        for(String provider:providers){
            buf.append(provider);
        }
        text.add(buf.toString());
    }

    @Override
    public void visitModuleEnd() {
        // empty
    }

    // ------------------------------------------------------------------------
    // Annotations
    // ------------------------------------------------------------------------

    @Override
    public void visit(final String name, final Object value) {
        buf.setLength(0);
        if (name != null) {
            buf.append(name);
        }
        buf.append(value);
        if (value instanceof String) {
            visitString((String) value);
        } else if (value instanceof Type) {
            visitType((Type) value);
        } else if (value instanceof Byte) {
            visitByte(((Byte) value).byteValue());
        } else if (value instanceof Boolean) {
            visitBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Short) {
            visitShort(((Short) value).shortValue());
        } else if (value instanceof Character) {
            visitChar(((Character) value).charValue());
        } else if (value instanceof Integer) {
            visitInt(((Integer) value).intValue());
        } else if (value instanceof Float) {
            visitFloat(((Float) value).floatValue());
        } else if (value instanceof Long) {
            visitLong(((Long) value).longValue());
        } else if (value instanceof Double) {
            visitDouble(((Double) value).doubleValue());
        } else if (value.getClass().isArray()) {
            if (value instanceof byte[]) {
                byte[] v = (byte[]) value;
                for (int i = 0; i < v.length; i++) {
                    visitByte(v[i]);
                }
            } else if (value instanceof boolean[]) {
                boolean[] v = (boolean[]) value;
                for (int i = 0; i < v.length; i++) {
                    visitBoolean(v[i]);
                }
            } else if (value instanceof short[]) {
                short[] v = (short[]) value;
                for (int i = 0; i < v.length; i++) {
                    visitShort(v[i]);
                }
            } else if (value instanceof char[]) {
                char[] v = (char[]) value;
                for (int i = 0; i < v.length; i++) {
                    visitChar(v[i]);
                }
            } else if (value instanceof int[]) {
                int[] v = (int[]) value;
                for (int i = 0; i < v.length; i++) {
                    visitInt(v[i]);
                }
            } else if (value instanceof long[]) {
                long[] v = (long[]) value;
                for (int i = 0; i < v.length; i++) {
                    visitLong(v[i]);
                }
            } else if (value instanceof float[]) {
                float[] v = (float[]) value;
                for (int i = 0; i < v.length; i++) {
                    visitFloat(v[i]);
                }
            } else if (value instanceof double[]) {
                double[] v = (double[]) value;
                for (int i = 0; i < v.length; i++) {
                    visitDouble(v[i]);
                }
            }
        }

        text.add(buf.toString());
    }

    private void visitInt(final int value) {
        buf.append(value);
    }

    private void visitLong(final long value) {
        buf.append(value);
    }

    private void visitFloat(final float value) {
        buf.append(value);
    }

    private void visitDouble(final double value) {
        buf.append(value);
    }

    private void visitChar(final char value) {
        buf.append((int) value);
    }

    private void visitShort(final short value) {
        buf.append(value);
    }

    private void visitByte(final byte value) {
        buf.append(value);
    }

    private void visitBoolean(final boolean value) {
        buf.append(value);
    }

    private void visitString(final String value) {
        appendString(buf, value);
    }

    private void visitType(final Type value) {
        buf.append(value.getClassName());
    }

    @Override
    public void visitEnum(final String name, final String desc,
                          final String value) {
        buf.setLength(0);
        if (name != null) {
            buf.append(name);
        }
        buf.append(desc);
        buf.append(value);
        text.add(buf.toString());
    }

    @Override
    public CleanCodeUtil visitAnnotation(final String name, final String desc) {
        buf.setLength(0);
        if (name != null) {
            buf.append(name);
        }
        buf.append(desc);
        text.add(buf.toString());
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        return c;
    }

    @Override
    public CleanCodeUtil visitArray(final String name) {
        buf.setLength(0);
        if (name != null) {
            buf.append(name);
        }
        text.add(buf.toString());
        CleanCodeUtil t = createCleanCodeUtil();
        text.add(t.getText());
        return t;
    }

    @Override
    public void visitAnnotationEnd() {
    }

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    @Override
    public CleanCodeUtil visitFieldAnnotation(final String desc,
                                                                   final boolean visible) {
        return visitAnnotation(desc, visible);
    }

    @Override
    public Printer visitFieldTypeAnnotation(int typeRef, TypePath typePath,
                                            String desc, boolean visible) {
        return visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public void visitFieldAttribute(final Attribute attr) {
        visitAttribute(attr);
    }

    @Override
    public void visitFieldEnd() {
    }

    // ------------------------------------------------------------------------
    // Methods
    // ------------------------------------------------------------------------

    @Override
    public void visitParameter(final String name, final int access) {
        buf.setLength(0);
        buf.append(access);
        if (name != null){
            buf.append(name);
        }
        text.add(buf.toString());
    }

    @Override
    public CleanCodeUtil visitAnnotationDefault() {
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        return c;
    }

    @Override
    public CleanCodeUtil visitMethodAnnotation(final String desc,
                                                                    final boolean visible) {
//        buf.setLength(0);
//        buf.append(desc);
//        text.add(buf.toString());
//        CleanCodeUtil c = createCleanCodeUtil();
//        text.add(c.getText());
//        text.add(visible);
//        return c;
        return visitAnnotation(desc, visible);
    }

    @Override
    public Printer visitMethodTypeAnnotation(int typeRef, TypePath typePath,
                                             String desc, boolean visible) {
        return visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public CleanCodeUtil visitParameterAnnotation(final int parameter,
                                                                       final String desc, final boolean visible) {
        buf.setLength(0);
        buf.append(desc);
        text.add(buf.toString());
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        text.add(visible);
        text.add(parameter);
        return c;
    }

    @Override
    public void visitMethodAttribute(final Attribute attr) {
        buf.setLength(0);
        buf.append(attr.type);

        if (attr instanceof Textifiable) {
            ((Textifiable) attr).textify(buf, labelNames);
        }

        text.add(buf.toString());
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
                           final Object[] local, final int nStack, final Object[] stack) {
        buf.setLength(0);
        switch (type) {
            case Opcodes.F_NEW:
            case Opcodes.F_FULL:
                appendFrameTypes(nLocal, local);
                appendFrameTypes(nStack, stack);
                break;
            case Opcodes.F_APPEND:
                appendFrameTypes(nLocal, local);
                break;
            case Opcodes.F_CHOP:
                buf.append(nLocal);
                break;
            case Opcodes.F_SAME:
                buf.append("SAME");
                break;
            case Opcodes.F_SAME1:
                buf.append("SAME1 ");
                appendFrameTypes(1, stack);
                break;
        }
        text.add(buf.toString());
    }

    @Override
    public void visitInsn(final int opcode) {
        buf.setLength(0);
        buf.append(OPCODES[opcode]);
        text.add(buf.toString());
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        buf.setLength(0);
        buf.append(OPCODES[opcode])
                .append(opcode == Opcodes.NEWARRAY ? TYPES[operand] : Integer
                        .toString(operand));
        text.add(buf.toString());
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        buf.setLength(0);
        buf.append(OPCODES[opcode]).append(var);
        text.add(buf.toString());
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        buf.setLength(0);
        buf.append(OPCODES[opcode]);
        buf.append(type);
        text.add(buf.toString());
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
                               final String name, final String desc) {
        buf.setLength(0);
        buf.append(OPCODES[opcode]);
        buf.append(owner);
        buf.append(name);
        buf.append(desc);;
        text.add(buf.toString());
    }

    @Deprecated
    @Override
    public void visitMethodInsn(final int opcode, final String owner,
                                final String name, final String desc) {
        if (api >= Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, desc,
                opcode == Opcodes.INVOKEINTERFACE);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
                                final String name, final String desc, final boolean itf) {
        if (api < Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, desc, itf);
    }

    private void doVisitMethodInsn(final int opcode, final String owner,
                                   final String name, final String desc, final boolean itf) {
        buf.setLength(0);
        buf.append(OPCODES[opcode]);
        buf.append(owner);
        buf.append(name);
        buf.append(desc);
        text.add(buf.toString());
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                                       Object... bsmArgs) {
        buf.setLength(0);
        buf.append(name);
        buf.append(desc);
        appendHandle(bsm);

        for (int i = 0; i < bsmArgs.length; i++) {
            Object cst = bsmArgs[i];
            if (cst instanceof String) {
                Printer.appendString(buf, (String) cst);
            } else if (cst instanceof Type) {
                Type type = (Type) cst;
                if(type.getSort() == Type.METHOD){
                    buf.append(type.getDescriptor());
                } else {
                    buf.append(type.getDescriptor());
                }
            } else if (cst instanceof Handle) {
                appendHandle((Handle) cst);
            } else {
                buf.append(cst);
            }
        }
        buf.setLength(buf.length() - 3);

        text.add(buf.toString());
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        buf.setLength(0);
        buf.append(OPCODES[opcode]);
        appendLabel(label);
        text.add(buf.toString());
    }

    @Override
    public void visitLabel(final Label label) {
        buf.setLength(0);
        appendLabel(label);
        text.add(buf.toString());
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        buf.setLength(0);
        if (cst instanceof String) {
            Printer.appendString(buf, (String) cst);
        } else if (cst instanceof Type) {
            buf.append(((Type) cst).getDescriptor());
        } else {
            buf.append(cst);
        }
        buf.append('\n');
        text.add(buf.toString());
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        buf.setLength(0);
        buf.append(var)
                .append(increment);
        text.add(buf.toString());
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
                                     final Label dflt, final Label... labels) {
        buf.setLength(0);
        for (int i = 0; i < labels.length; ++i) {
            buf.append(min + i);
            appendLabel(labels[i]);
        }
        appendLabel(dflt);
        text.add(buf.toString());
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
                                      final Label[] labels) {
        buf.setLength(0);
        for (int i = 0; i < labels.length; ++i) {
            buf.append(keys[i]);
            appendLabel(labels[i]);
        }
        appendLabel(dflt);
        text.add(buf.toString());
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        buf.setLength(0);
        buf.append(desc);
        buf.append(dims);
        text.add(buf.toString());
    }

    @Override
    public Printer visitInsnAnnotation(int typeRef, TypePath typePath,
                                       String desc, boolean visible) {
        return visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end,
                                   final Label handler, final String type) {
        buf.setLength(0);
        appendLabel(start);
        appendLabel(end);
        appendLabel(handler);
        buf.append(type);
        text.add(buf.toString());
    }

    @Override
    public Printer visitTryCatchAnnotation(int typeRef, TypePath typePath,
                                           String desc, boolean visible) {
        buf.setLength(0);
        buf.append(desc);
        text.add(buf.toString());
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        buf.setLength(0);
        appendTypeReference(typeRef);
        buf.append(typePath);
        buf.append(visible);
        text.add(buf.toString());
        return c;
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
                                   final String signature, final Label start, final Label end,
                                   final int index) {
        buf.setLength(0);
        buf.append(name);
        buf.append(desc);
        appendLabel(start);
        appendLabel(end);
        buf.append(index);

        if (signature != null) {
            buf.append(signature);

            TraceSignatureVisitor sv = new TraceSignatureVisitor(0);
            SignatureReader r = new SignatureReader(signature);
            r.acceptType(sv);
            buf.append(sv.getDeclaration());
        }
        text.add(buf.toString());
    }

    @Override
    public Printer visitLocalVariableAnnotation(int typeRef, TypePath typePath,
                                                Label[] start, Label[] end, int[] index, String desc,
                                                boolean visible) {
        buf.setLength(0);
        buf.append(desc);
        text.add(buf.toString());
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        buf.setLength(0);
        appendTypeReference(typeRef);
        buf.append(typePath);
        for (int i = 0; i < start.length; ++i) {
            appendLabel(start[i]);
            appendLabel(end[i]);
            buf.append(index[i]);
        }
        buf.append(visible);
        text.add(buf.toString());
        return c;
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
        buf.setLength(0);
        buf.append(line);
        appendLabel(start);
        text.add(buf.toString());
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        buf.setLength(0);
        buf.append(maxStack);
        text.add(buf.toString());

        buf.setLength(0);
        buf.append(maxLocals);
        text.add(buf.toString());
    }

    @Override
    public void visitMethodEnd() {
    }

    // ------------------------------------------------------------------------
    // Common methods
    // ------------------------------------------------------------------------

    /**
     * Prints a disassembled view of the given annotation.
     *
     * @param desc
     *            the class descriptor of the annotation class.
     * @param visible
     *            <tt>true</tt> if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values.
     */
    public CleanCodeUtil visitAnnotation(final String desc, final boolean visible) {
        buf.setLength(0);
        buf.append(desc);
        text.add(buf.toString());
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        text.add(visible);
        return c;
    }

    /**
     * Prints a disassembled view of the given type annotation.
     *
     * @param typeRef
     *            a reference to the annotated type. See {@link TypeReference}.
     * @param typePath
     *            the path to the annotated type argument, wildcard bound, array
     *            element type, or static inner type within 'typeRef'. May be
     *            <tt>null</tt> if the annotation targets 'typeRef' as a whole.
     * @param desc
     *            the class descriptor of the annotation class.
     * @param visible
     *            <tt>true</tt> if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values.
     */
    public CleanCodeUtil visitTypeAnnotation(final int typeRef,
                                                                  final TypePath typePath, final String desc, final boolean visible) {
        buf.setLength(0);
        buf.append(desc);
        text.add(buf.toString());
        CleanCodeUtil c = createCleanCodeUtil();
        text.add(c.getText());
        buf.setLength(0);
        appendTypeReference(typeRef);
        buf.append(typePath);
        buf.append(visible);
        text.add(buf.toString());
        return c;
    }

    /**
     * Prints a disassembled view of the given attribute.
     *
     * @param attr
     *            an attribute.
     */
    public void visitAttribute(final Attribute attr) {
        buf.setLength(0);
        buf.append(attr.type);

        if (attr instanceof Textifiable) {
            ((Textifiable) attr).textify(buf, null);
        } else {
            buf.append(" : unknown\n");
        }

        text.add(buf.toString());
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * Creates a new TraceVisitor instance.
     *
     * @return a new TraceVisitor.
     */
    protected CleanCodeUtil createCleanCodeUtil() {
        return new CleanCodeUtil();
    }


    /**
     * Appends the name of the given label to {@link #buf buf}. Creates a new
     * label name if the given label does not yet have one.
     *
     * @param l
     *            a label.
     */
    protected void appendLabel(final Label l) {
        if (labelNames == null) {
            labelNames = new HashMap<Label, String>();
        }
        String name = labelNames.get(l);
        if (name == null) {
            name = String.valueOf(labelNames.size());
            labelNames.put(l, name);
        }
        buf.append(name);
    }

    /**
     * Appends the information about the given handle to {@link #buf buf}.
     *
     * @param h
     *            a handle, non null.
     */
    protected void appendHandle(final Handle h) {
        int tag = h.getTag();
        buf.append(Integer.toHexString(tag));
        boolean isMethodHandle = false;
        switch (tag) {
            case Opcodes.H_GETFIELD:
            case Opcodes.H_GETSTATIC:
            case Opcodes.H_PUTFIELD:
            case Opcodes.H_PUTSTATIC:
                break;
            case Opcodes.H_INVOKEINTERFACE:
            case Opcodes.H_NEWINVOKESPECIAL:
            case Opcodes.H_INVOKESPECIAL:
            case Opcodes.H_INVOKESTATIC:
            case Opcodes.H_INVOKEVIRTUAL:
                isMethodHandle = true;
                break;
        }
        buf.append(h.getOwner());
        buf.append(h.getName());
        if (!isMethodHandle) {
            buf.append('(');
        }
        buf.append(h.getDesc());
        if (!isMethodHandle) {
            buf.append(')');
        }
        if (h.isInterface()) {
            buf.append(" itf");
        }
    }

    private void appendTypeReference(final int typeRef) {
        TypeReference ref = new TypeReference(typeRef);
        switch (ref.getSort()) {
            case TypeReference.CLASS_TYPE_PARAMETER:
                buf.append("CLASS_TYPE_PARAMETER ").append(
                        ref.getTypeParameterIndex());
                break;
            case TypeReference.METHOD_TYPE_PARAMETER:
                buf.append("METHOD_TYPE_PARAMETER ").append(
                        ref.getTypeParameterIndex());
                break;
            case TypeReference.CLASS_EXTENDS:
                buf.append("CLASS_EXTENDS ").append(ref.getSuperTypeIndex());
                break;
            case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
                buf.append("CLASS_TYPE_PARAMETER_BOUND ")
                        .append(ref.getTypeParameterIndex()).append(", ")
                        .append(ref.getTypeParameterBoundIndex());
                break;
            case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
                buf.append("METHOD_TYPE_PARAMETER_BOUND ")
                        .append(ref.getTypeParameterIndex()).append(", ")
                        .append(ref.getTypeParameterBoundIndex());
                break;
            case TypeReference.FIELD:
                buf.append("FIELD");
                break;
            case TypeReference.METHOD_RETURN:
                buf.append("METHOD_RETURN");
                break;
            case TypeReference.METHOD_RECEIVER:
                buf.append("METHOD_RECEIVER");
                break;
            case TypeReference.METHOD_FORMAL_PARAMETER:
                buf.append("METHOD_FORMAL_PARAMETER ").append(
                        ref.getFormalParameterIndex());
                break;
            case TypeReference.THROWS:
                buf.append("THROWS ").append(ref.getExceptionIndex());
                break;
            case TypeReference.LOCAL_VARIABLE:
                buf.append("LOCAL_VARIABLE");
                break;
            case TypeReference.RESOURCE_VARIABLE:
                buf.append("RESOURCE_VARIABLE");
                break;
            case TypeReference.EXCEPTION_PARAMETER:
                buf.append("EXCEPTION_PARAMETER ").append(
                        ref.getTryCatchBlockIndex());
                break;
            case TypeReference.INSTANCEOF:
                buf.append("INSTANCEOF");
                break;
            case TypeReference.NEW:
                buf.append("NEW");
                break;
            case TypeReference.CONSTRUCTOR_REFERENCE:
                buf.append("CONSTRUCTOR_REFERENCE");
                break;
            case TypeReference.METHOD_REFERENCE:
                buf.append("METHOD_REFERENCE");
                break;
            case TypeReference.CAST:
                buf.append("CAST ").append(ref.getTypeArgumentIndex());
                break;
            case TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
                buf.append("CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT ").append(
                        ref.getTypeArgumentIndex());
                break;
            case TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT:
                buf.append("METHOD_INVOCATION_TYPE_ARGUMENT ").append(
                        ref.getTypeArgumentIndex());
                break;
            case TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
                buf.append("CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT ").append(
                        ref.getTypeArgumentIndex());
                break;
            case TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT:
                buf.append("METHOD_REFERENCE_TYPE_ARGUMENT ").append(
                        ref.getTypeArgumentIndex());
                break;
        }
    }

    private void appendFrameTypes(final int n, final Object[] o) {
        for (int i = 0; i < n; ++i) {
            if (i > 0) {
                buf.append(' ');
            }
            if (o[i] instanceof String) {
                String desc = (String) o[i];
                buf.append(desc);
            } else if (o[i] instanceof Integer) {
                buf.append(o[i]);
            } else {
                appendLabel((Label) o[i]);
            }
        }
    }

    /**
     * Prints a disassembled view of the given class to the standard output.
     * <p>
     * Usage: Textifier [-debug] &lt;binary class name or class file name &gt;
     *
     * @param args
     *            the command line arguments.
     *
     * @throws Exception
     *             if the class cannot be found, or if an IO exception occurs.
     */
    public static void main(final String[] args) throws Exception {
//        String filePath = "/Users/liuyu/Desktop/ekstazi-tool/org.ekstazi.core/src/main/java/org/ekstazi/changelevel/A.class";
//        byte[] array = Files.readAllBytes(Paths.get(filePath));
//        MyClassVisitor visitor = initMyClassVisitor(array);
//        TreeMap<String, String> map = visitor.getMethodMap();
//        String key = "parseArray(Ljava/lang/reflect/Type;Ljava/util/Collection;Ljava/lang/Object;)V";
//        if(map.containsKey(key)){
//            System.out.println(map.get(key));
//        }
//        }
//        for (String key : map.keySet().stream().sorted().collect(Collectors.toList())){
//            System.out.println(key);
//            System.out.println();
//        }

//        String f = visitor.getField();
    }

//    public static MyClassVisitor initMyClassVisitor(byte[] bytes){
//        ClassReader reader = new ClassReader(bytes);
//        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
//        MyClassVisitor visitor =new MyClassVisitor(Opcodes.ASM6, writer);
//        reader.accept(visitor, ClassReader.SKIP_DEBUG);
//        return visitor;
//    }
}
