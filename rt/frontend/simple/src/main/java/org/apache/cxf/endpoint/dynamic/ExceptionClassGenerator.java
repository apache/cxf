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
package org.apache.cxf.endpoint.dynamic;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.ClassGeneratorClassLoader;
import org.apache.cxf.common.util.ASMHelper;

public class ExceptionClassGenerator extends ClassGeneratorClassLoader implements ExceptionClassCreator {
    private Bus bus;
    private ASMHelper helper;

    public ExceptionClassGenerator(Bus bus) {
        this.bus = bus;
        this.helper = bus.getExtension(ASMHelper.class);
    }
    @Override
    public Class<?> createExceptionClass(Class<?> bean) {
        String newClassName = bean.getName() + "_Exception";
        newClassName = newClassName.replaceAll("\\$", ".");
        newClassName = helper.periodToSlashes(newClassName);

        Class<?> cls = findClass(newClassName.replace('/', '.'), bean);
        if (cls == null) {
            ASMHelper.ClassWriter cw = helper.createClassWriter();
            ASMHelper.OpcodesProxy Opcodes = helper.getOpCodes();

            cw.visit(Opcodes.V1_5,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                    newClassName,
                    null,
                    "java/lang/Exception",
                    null);

            ASMHelper.FieldVisitor fv;
            ASMHelper.MethodVisitor mv;

            String beanClassCode = helper.getClassCode(bean);
            fv = cw.visitField(0, "faultInfo", beanClassCode, null, null);
            fv.visitEnd();


            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                    "(Ljava/lang/String;" + beanClassCode + ")V", null, null);
            mv.visitCode();
            mv.visitLabel(helper.createLabel());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Exception",
                    "<init>", "(Ljava/lang/String;)V", false);
            mv.visitLabel(helper.createLabel());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.PUTFIELD, newClassName, "faultInfo", beanClassCode);
            mv.visitLabel(helper.createLabel());
            mv.visitInsn(Opcodes.RETURN);
            mv.visitLabel(helper.createLabel());
            mv.visitMaxs(2, 3);
            mv.visitEnd();

            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getFaultInfo",
                    "()" + beanClassCode, null, null);
            mv.visitCode();
            mv.visitLabel(helper.createLabel());
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, newClassName, "faultInfo", beanClassCode);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(helper.createLabel());
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            cw.visitEnd();

            return loadClass(bean.getName() + "_Exception", bean, cw.toByteArray());
        }
        return cls;
    }
}
