/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.binding.corba.utils;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.ClassGeneratorClassLoader;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.OpcodesProxy;

public class CorbaFixedAnyImplGenerator extends ClassGeneratorClassLoader implements CorbaFixedAnyImplClassCreator {

    public CorbaFixedAnyImplGenerator(Bus bus) {
        super(bus);
    }
    public Class<?> createFixedAnyClass() {
        ASMHelper helper = bus.getExtension(ASMHelper.class);
        OpcodesProxy opCodes = helper.getOpCodes();
        ASMHelper.ClassWriter cw = helper.createClassWriter();
        ASMHelper.FieldVisitor fv;

        cw.visit(opCodes.V1_6, opCodes.ACC_PUBLIC + opCodes.ACC_SUPER,
                "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                null, "com/sun/corba/se/impl/corba/AnyImpl", null);

        cw.visitSource("FixedAnyImpl.java", null);

        fv = cw.visitField(0, "obj", "Lorg/omg/CORBA/portable/Streamable;", null, null);
        fv.visitEnd();
        addFixedAnyConstructor(helper, cw);
        addInsertOverride(helper, cw);
        addExtractOverride(helper, cw);
        addWriteOverride(helper, cw);
        addReadOverride(helper, cw);

        cw.visitEnd();

        byte[] b = cw.toByteArray();
        Class<?> c = loadClass("org.apache.cxf.binding.corba.utils.FixedAnyImpl",
                CorbaFixedAnyImplGenerator.class, b);
        return c;
    }

    private void addReadOverride(ASMHelper helper, ASMHelper.ClassWriter cw) {
        OpcodesProxy opCodes = helper.getOpCodes();
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC, "read_value",
                "(Lorg/omg/CORBA/portable/InputStream;Lorg/omg/CORBA/TypeCode;)V",
                null, null);
        mv.visitCode();
        ASMHelper.Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(54, l0);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitFieldInsn(opCodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                "obj", "Lorg/omg/CORBA/portable/Streamable;");
        ASMHelper.Label l1 = helper.createLabel();
        mv.visitJumpInsn(opCodes.IFNULL, l1);
        ASMHelper.Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(55, l2);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitFieldInsn(opCodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                "obj", "Lorg/omg/CORBA/portable/Streamable;");
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitMethodInsn(opCodes.INVOKEINTERFACE, "org/omg/CORBA/portable/Streamable",
                "_read", "(Lorg/omg/CORBA/portable/InputStream;)V", true);
        ASMHelper.Label l3 = helper.createLabel();
        mv.visitJumpInsn(opCodes.GOTO, l3);
        mv.visitLabel(l1);
        mv.visitLineNumber(57, l1);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitVarInsn(opCodes.ALOAD, 2);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL, "com/sun/corba/se/impl/corba/AnyImpl",
                "read_value",
                "(Lorg/omg/CORBA/portable/InputStream;Lorg/omg/CORBA/TypeCode;)V", false);
        mv.visitLabel(l3);
        mv.visitLineNumber(59, l3);
        mv.visitInsn(opCodes.RETURN);
        ASMHelper.Label l4 = helper.createLabel();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                null, l0, l4, 0);
        mv.visitLocalVariable("is", "Lorg/omg/CORBA/portable/InputStream;", null, l0, l4, 1);
        mv.visitLocalVariable("t", "Lorg/omg/CORBA/TypeCode;", null, l0, l4, 2);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void addWriteOverride(ASMHelper helper, ASMHelper.ClassWriter cw) {
        OpcodesProxy opCodes = helper.getOpCodes();
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC, "write_value",
                "(Lorg/omg/CORBA/portable/OutputStream;)V", null, null);
        mv.visitCode();
        ASMHelper.Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(61, l0);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitFieldInsn(opCodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                "obj", "Lorg/omg/CORBA/portable/Streamable;");
        ASMHelper.Label l1 = helper.createLabel();
        mv.visitJumpInsn(opCodes.IFNULL, l1);
        ASMHelper.Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(62, l2);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitFieldInsn(opCodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                "obj", "Lorg/omg/CORBA/portable/Streamable;");

        ASMHelper.Label l3 = helper.createLabel();
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitMethodInsn(opCodes.INVOKEINTERFACE, "org/omg/CORBA/portable/Streamable",
                "_write", "(Lorg/omg/CORBA/portable/OutputStream;)V", true);
        mv.visitJumpInsn(opCodes.GOTO, l3);
        mv.visitLabel(l1);
        mv.visitLineNumber(64, l1);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL, "com/sun/corba/se/impl/corba/AnyImpl",
                "write_value", "(Lorg/omg/CORBA/portable/OutputStream;)V", false);
        mv.visitLabel(l3);
        mv.visitLineNumber(66, l3);
        mv.visitInsn(opCodes.RETURN);
        ASMHelper.Label l4 = helper.createLabel();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                null, l0, l4, 0);
        mv.visitLocalVariable("os", "Lorg/omg/CORBA/portable/OutputStream;", null, l0, l4, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }

    private void addExtractOverride(ASMHelper helper, ASMHelper.ClassWriter cw) {
        OpcodesProxy opCodes = helper.getOpCodes();
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC, "extract_Streamable",
                "()Lorg/omg/CORBA/portable/Streamable;", null, null);
        mv.visitCode();
        ASMHelper.Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(47, l0);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitFieldInsn(opCodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                "obj", "Lorg/omg/CORBA/portable/Streamable;");
        ASMHelper.Label l1 = helper.createLabel();
        mv.visitJumpInsn(opCodes.IFNULL, l1);
        ASMHelper.Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(48, l2);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitFieldInsn(opCodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                "obj", "Lorg/omg/CORBA/portable/Streamable;");
        mv.visitInsn(opCodes.ARETURN);
        mv.visitLabel(l1);
        mv.visitLineNumber(50, l1);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL, "com/sun/corba/se/impl/corba/AnyImpl",
                "extract_Streamable", "()Lorg/omg/CORBA/portable/Streamable;", false);
        mv.visitInsn(opCodes.ARETURN);
        ASMHelper.Label l3 = helper.createLabel();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;", null, l0, l3, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }

    private void addInsertOverride(ASMHelper helper, ASMHelper.ClassWriter cw) {
        OpcodesProxy opCodes = helper.getOpCodes();
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC,
                "insert_Streamable",
                "(Lorg/omg/CORBA/portable/Streamable;)V", null, null);
        mv.visitCode();
        ASMHelper.Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(43, l0);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL,
                "com/sun/corba/se/impl/corba/AnyImpl",
                "insert_Streamable",
                "(Lorg/omg/CORBA/portable/Streamable;)V", false);
        ASMHelper.Label l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(44, l1);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitFieldInsn(opCodes.PUTFIELD,
                "org/apache/cxf/binding/corba/utils/FixedAnyImpl", "obj",
                "Lorg/omg/CORBA/portable/Streamable;");
        ASMHelper.Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(45, l2);
        mv.visitInsn(opCodes.RETURN);
        ASMHelper.Label l3 = helper.createLabel();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                null, l0, l3, 0);
        mv.visitLocalVariable("s", "Lorg/omg/CORBA/portable/Streamable;", null, l0, l3, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void addFixedAnyConstructor(ASMHelper helper, ASMHelper.ClassWriter cw) {
        OpcodesProxy opCodes = helper.getOpCodes();
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC, "<init>", "(Lorg/omg/CORBA/ORB;)V", null, null);
        mv.visitCode();
        ASMHelper.Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(36, l0);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitTypeInsn(opCodes.CHECKCAST, "com/sun/corba/se/spi/orb/ORB");
        mv.visitMethodInsn(opCodes.INVOKESPECIAL,
                "com/sun/corba/se/impl/corba/AnyImpl",
                "<init>", "(Lcom/sun/corba/se/spi/orb/ORB;)V", false);
        ASMHelper.Label l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(37, l1);
        mv.visitInsn(opCodes.RETURN);
        ASMHelper.Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this",
                "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                null, l0, l2, 0);
        mv.visitLocalVariable("orb", "Lorg/omg/CORBA/ORB;", null, l0, l2, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();


        mv = cw.visitMethod(opCodes.ACC_PUBLIC, "<init>",
                "(Lorg/omg/CORBA/ORB;Lorg/omg/CORBA/Any;)V",
                null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(39, l0);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitTypeInsn(opCodes.CHECKCAST, "com/sun/corba/se/spi/orb/ORB");
        mv.visitVarInsn(opCodes.ALOAD, 2);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL,
                "com/sun/corba/se/impl/corba/AnyImpl",
                "<init>",
                "(Lcom/sun/corba/se/spi/orb/ORB;Lorg/omg/CORBA/Any;)V", false);
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(40, l1);
        mv.visitInsn(opCodes.RETURN);
        l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                null, l0, l2, 0);
        mv.visitLocalVariable("orb", "Lorg/omg/CORBA/ORB;", null, l0, l2, 1);
        mv.visitLocalVariable("any", "Lorg/omg/CORBA/Any;", null, l0, l2, 2);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }
}
