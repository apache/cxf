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

package org.apache.cxf.jaxb;

import java.lang.reflect.Constructor;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.ClassGeneratorClassLoader;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.OpcodesProxy;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;


public class FactoryClassGenerator extends ClassGeneratorClassLoader implements FactoryClassCreator {
    private final ASMHelper helper;
    FactoryClassGenerator(Bus bus) {
        super(bus);
        helper = bus.getExtension(ASMHelper.class);
    }
    @SuppressWarnings("unused")
    public Class<?> createFactory(Class<?> cls) {
        String newClassName = cls.getName() + "Factory";
        Class<?> factoryClass = findClass(newClassName, cls);
        if (factoryClass != null) {
            return factoryClass;
        }
        Constructor<?> contructor = ReflectionUtil.getDeclaredConstructors(cls)[0];
        OpcodesProxy opcodes = helper.getOpCodes();
        ASMHelper.ClassWriter cw = helper.createClassWriter();
        ASMHelper.MethodVisitor mv;

        cw.visit(opcodes.V1_6, opcodes.ACC_PUBLIC + opcodes.ACC_SUPER,
                StringUtils.periodToSlashes(newClassName), null, "java/lang/Object", null);

        cw.visitSource(cls.getSimpleName() + "Factory" + ".java", null);

        mv = cw.visitMethod(opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(opcodes.ALOAD, 0);
        mv.visitMethodInsn(opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(opcodes.ACC_PUBLIC, "create" + cls.getSimpleName(),
                "()L" + StringUtils.periodToSlashes(cls.getName()) + ";", null, null);
        mv.visitCode();
        String name = cls.getName().replace('.', '/');
        mv.visitTypeInsn(opcodes.NEW, name);
        mv.visitInsn(opcodes.DUP);
        StringBuilder paraString = new StringBuilder(32).append('(');

        for (Class<?> paraClass : contructor.getParameterTypes()) {
            mv.visitInsn(opcodes.ACONST_NULL);
            paraString.append("Ljava/lang/Object;");
        }
        paraString.append(")V");

        mv.visitMethodInsn(opcodes.INVOKESPECIAL, name, "<init>", paraString.toString(), false);

        mv.visitInsn(opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return loadClass(newClassName, cls, cw.toByteArray());
    }
}
