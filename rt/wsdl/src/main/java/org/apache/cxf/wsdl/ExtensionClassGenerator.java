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
package org.apache.cxf.wsdl;

import java.lang.reflect.Method;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.ClassGeneratorClassLoader;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.OpcodesProxy;
import org.apache.cxf.common.util.StringUtils;

public class ExtensionClassGenerator extends ClassGeneratorClassLoader implements ExtensionClassCreator {

    public ExtensionClassGenerator(Bus bus) {
        super(bus);
    }
    //CHECKSTYLE:OFF - very complicated ASM code
    public Class<?> createExtensionClass(Class<?> cls, QName qname, ClassLoader loader) {

        String className = StringUtils.periodToSlashes(cls.getName());
        ASMHelper helper = bus.getExtension(ASMHelper.class);
        OpcodesProxy Opcodes = helper.getOpCodes();

        Class<?> extClass = findClass(className + "Extensibility", loader);
        if (extClass != null) {
            return extClass;
        }

        ASMHelper.ClassWriter cw = helper.createClassWriter();
        ASMHelper.FieldVisitor fv;
        ASMHelper.MethodVisitor mv;
        ASMHelper.AnnotationVisitor av0;

        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_SYNTHETIC,
                className + "Extensibility", null,
                className,
                new String[] {"javax/wsdl/extensions/ExtensibilityElement"});

        cw.visitSource(cls.getSimpleName() + "Extensibility.java", null);

        fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                "WSDL_REQUIRED", "Ljavax/xml/namespace/QName;", null, null);
        fv.visitEnd();
        fv = cw.visitField(0, "qn", "Ljavax/xml/namespace/QName;", null, null);
        fv.visitEnd();


        boolean hasAttributes = false;
        try {
            Method m = cls.getDeclaredMethod("getOtherAttributes");
            if (m != null && m.getReturnType() == Map.class){
                hasAttributes = true;
            }
        } catch (Throwable t) {
            //ignore
        }
        if (hasAttributes) {
            mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            ASMHelper.Label l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(64, l0);
            mv.visitTypeInsn(Opcodes.NEW, "javax/xml/namespace/QName");
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn("http://schemas.xmlsoap.org/wsdl/");
            mv.visitLdcInsn("required");
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "javax/xml/namespace/QName", "<init>",
                    "(Ljava/lang/String;Ljava/lang/String;)V", false);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, className + "Extensibility", "WSDL_REQUIRED",
                    "Ljavax/xml/namespace/QName;");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            fv = cw.visitField(Opcodes.ACC_PRIVATE, "required", "Ljava/lang/Boolean;", null, null);
            fv.visitEnd();
        }

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        ASMHelper.Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(33, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", "()V", false);
        ASMHelper.Label l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(31, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitTypeInsn(Opcodes.NEW, "javax/xml/namespace/QName");
        mv.visitInsn(Opcodes.DUP);

        mv.visitLdcInsn(qname.getNamespaceURI());
        mv.visitLdcInsn(qname.getLocalPart());

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "javax/xml/namespace/QName",
                "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", false);
        mv.visitFieldInsn(Opcodes.PUTFIELD, className + "Extensibility",
                "qn", "Ljavax/xml/namespace/QName;");
        ASMHelper.Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(34, l2);
        mv.visitInsn(Opcodes.RETURN);
        ASMHelper.Label l3 = helper.createLabel();
        mv.visitLabel(l3);

        mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l3, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setElementType", "(Ljavax/xml/namespace/QName;)V", null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(37, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, className + "Extensibility", "qn", "Ljavax/xml/namespace/QName;");
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(38, l1);
        mv.visitInsn(Opcodes.RETURN);
        l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l2, 0);
        mv.visitLocalVariable("elementType", "Ljavax/xml/namespace/QName;", null, l0, l2, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getElementType", "()Ljavax/xml/namespace/QName;", null, null);
        av0 = mv.visitAnnotation("Ljakarta/xml/bind/annotation/XmlTransient;", true);
        av0.visitEnd();
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(40, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, className + "Extensibility", "qn", "Ljavax/xml/namespace/QName;");
        mv.visitInsn(Opcodes.ARETURN);
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l1, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        if (hasAttributes) {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getRequired", "()Ljava/lang/Boolean;", null, null);
            mv.visitCode();
            l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(66, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className + "Extensibility", "getOtherAttributes",
                    "()Ljava/util/Map;", false);
            mv.visitFieldInsn(Opcodes.GETSTATIC, className + "Extensibility", "WSDL_REQUIRED",
                    "Ljavax/xml/namespace/QName;");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            mv.visitVarInsn(Opcodes.ASTORE, 1);
            l1 = helper.createLabel();
            mv.visitLabel(l1);
            mv.visitLineNumber(67, l1);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            l2 = helper.createLabel();
            mv.visitJumpInsn(Opcodes.IFNONNULL, l2);
            mv.visitInsn(Opcodes.ACONST_NULL);
            l3 = helper.createLabel();
            mv.visitJumpInsn(Opcodes.GOTO, l3);
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] {"java/lang/String"}, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf",
                    "(Ljava/lang/String;)Ljava/lang/Boolean;", false);
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Boolean"});
            mv.visitInsn(Opcodes.ARETURN);
            ASMHelper.Label l4 = helper.createLabel();
            mv.visitLabel(l4);
            mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l4, 0);
            mv.visitLocalVariable("s", "Ljava/lang/String;", null, l1, l4, 1);
            mv.visitMaxs(0, 0);
            mv.visitEnd();



            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setRequired", "(Ljava/lang/Boolean;)V", null, null);
            mv.visitCode();
            l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(76, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            l1 = helper.createLabel();
            mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
            l2 = helper.createLabel();
            mv.visitLabel(l2);
            mv.visitLineNumber(77, l2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className + "Extensibility", "getOtherAttributes",
                    "()Ljava/util/Map;", false);
            mv.visitFieldInsn(Opcodes.GETSTATIC, className + "Extensibility", "WSDL_REQUIRED",
                    "Ljavax/xml/namespace/QName;");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "remove",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(Opcodes.POP);
            l3 = helper.createLabel();
            mv.visitLabel(l3);
            mv.visitLineNumber(78, l3);
            l4 = helper.createLabel();
            mv.visitJumpInsn(Opcodes.GOTO, l4);
            mv.visitLabel(l1);
            mv.visitLineNumber(79, l1);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className + "Extensibility", "getOtherAttributes",
                    "()Ljava/util/Map;", false);
            mv.visitFieldInsn(Opcodes.GETSTATIC, className + "Extensibility", "WSDL_REQUIRED",
                    "Ljavax/xml/namespace/QName;");
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(Opcodes.POP);
            mv.visitLabel(l4);
            mv.visitLineNumber(81, l4);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(Opcodes.RETURN);
            ASMHelper.Label l5 = helper.createLabel();
            mv.visitLabel(l5);
            mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l5, 0);
            mv.visitLocalVariable("b", "Ljava/lang/Boolean;", null, l0, l5, 1);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getRequired", "()Ljava/lang/Boolean;", null, null);
            mv.visitCode();
            l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(68, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className + "Extensibility", "required", "Ljava/lang/Boolean;");
            mv.visitInsn(Opcodes.ARETURN);
            l1 = helper.createLabel();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l1, 0);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setRequired", "(Ljava/lang/Boolean;)V", null, null);
            mv.visitCode();
            l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(71, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className + "Extensibility", "required", "Ljava/lang/Boolean;");
            l1 = helper.createLabel();
            mv.visitLabel(l1);
            mv.visitLineNumber(72, l1);
            mv.visitInsn(Opcodes.RETURN);
            l2 = helper.createLabel();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l2, 0);
            mv.visitLocalVariable("b", "Ljava/lang/Boolean;", null, l0, l2, 1);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();

        byte[] bytes = cw.toByteArray();
        return loadClass(className + "Extensibility", loader, bytes);
    }
}
