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

package org.apache.cxf.common.util;

import java.lang.reflect.Method;

import org.apache.cxf.common.util.ReflectionInvokationHandler.Optional;
import org.apache.cxf.common.util.ReflectionInvokationHandler.UnwrapParam;
import org.apache.cxf.common.util.ReflectionInvokationHandler.WrapReturn;


public interface ASMHelper {
    String getClassCode(Class<?> cl);
    String getClassCode(java.lang.reflect.Type type);
    ClassWriter createClassWriter();
    ASMType getType(String type);
    Label createLabel();
    OpcodesProxy getOpCodes();
    Class<?> getASMClass() throws ClassNotFoundException;
    String getMethodSignature(Method m);
    String getNonPrimitive(Class<?> tp);
    String getPrimitive(Class<?> tp);

    interface ASMType {
        int getOpcode(int ireturn);
    }

    interface ClassWriter {
        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitAnnotation(String cls, boolean t);

        @WrapReturn(FieldVisitor.class)
        FieldVisitor visitField(int accPrivate, String fieldName, String classCode,
                                String fieldDescriptor, Object object);

        void visitEnd();
        byte[] toByteArray();

        @WrapReturn(MethodVisitor.class)
        MethodVisitor visitMethod(int accPublic, String string, String string2,
                                  String s3,
                                  String[] s4);
        void visit(int v15, int i, String newClassName, String object, String string, String[] object2);
        void visitSource(String arg0, String arg1);
    }

    interface Label {
    }

    interface FieldVisitor {
        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitAnnotation(String cls, boolean b);
        void visitEnd();
    }
    interface MethodVisitor {
        void visitEnd();
        void visitLabel(@UnwrapParam(typeMethodName = "realType") Label l1);
        void visitMaxs(int i, int j);
        void visitLineNumber(int i, @UnwrapParam(typeMethodName = "realType") Label l0);
        void visitInsn(int return1);
        void visitVarInsn(int aload, int i);
        void visitCode();
        void visitLdcInsn(String sig);
        void visitLocalVariable(String string,
                                String string2,
                                String string3,
                                @UnwrapParam(typeMethodName = "realType") Label lBegin,
                                @UnwrapParam(typeMethodName = "realType") Label lEnd,
                                int i);
        void visitTypeInsn(int checkcast, String string);
        void visitMethodInsn(int invokevirtual, String periodToSlashes,
                             String name, String methodSignature, @Optional boolean itf);
        void visitIntInsn(int sipush, int x);
        void visitIincInsn(int i, int j);
        void visitFieldInsn(int getfield, String periodToSlashes,
                            String string, String string2);
        void visitJumpInsn(int ifnonnull, @UnwrapParam(typeMethodName = "realType") Label nonNullLabel);
        void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack);

        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitAnnotation(String cls, boolean b);
    }
    interface AnnotationVisitor {
        void visit(String arg0, @UnwrapParam(typeMethodName = "realType") ASMType arg1);
        void visit(String arg0, Object arg1);
        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitAnnotation(String arg0, String arg1);
        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitArray(String arg0);
        void visitEnd();
        void visitEnum(String arg0, String arg1, String arg2);
    }


}
