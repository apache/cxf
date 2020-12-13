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
import org.apache.cxf.common.util.OpcodesProxy;
import org.apache.cxf.common.util.StringUtils;

public class ExceptionClassGenerator extends ClassGeneratorClassLoader implements ExceptionClassCreator {
    private final ASMHelper helper;

    public ExceptionClassGenerator(Bus bus) {
        super(bus);
        this.helper = bus.getExtension(ASMHelper.class);
    }
    @Override
    public Class<?> createExceptionClass(Class<?> bean) {
        String newClassName = bean.getName() + "_Exception";
        newClassName = newClassName.replaceAll("\\$", ".");
        newClassName = StringUtils.periodToSlashes(newClassName);

        Class<?> cls = findClass(StringUtils.slashesToPeriod(newClassName), bean);
        if (cls == null) {
            ASMHelper.ClassWriter cw = helper.createClassWriter();
            OpcodesProxy opCodes = helper.getOpCodes();

            cw.visit(opCodes.V1_5,
                    opCodes.ACC_PUBLIC | opCodes.ACC_SUPER,
                    newClassName,
                    null,
                    "java/lang/Exception",
                    null);

            ASMHelper.FieldVisitor fv;
            ASMHelper.MethodVisitor mv;

            String beanClassCode = helper.getClassCode(bean);
            fv = cw.visitField(0, "faultInfo", beanClassCode, null, null);
            fv.visitEnd();


            mv = cw.visitMethod(opCodes.ACC_PUBLIC, "<init>",
                    "(Ljava/lang/String;" + beanClassCode + ")V", null, null);
            mv.visitCode();
            mv.visitLabel(helper.createLabel());
            mv.visitVarInsn(opCodes.ALOAD, 0);
            mv.visitVarInsn(opCodes.ALOAD, 1);
            mv.visitMethodInsn(opCodes.INVOKESPECIAL, "java/lang/Exception",
                    "<init>", "(Ljava/lang/String;)V", false);
            mv.visitLabel(helper.createLabel());
            mv.visitVarInsn(opCodes.ALOAD, 0);
            mv.visitVarInsn(opCodes.ALOAD, 2);
            mv.visitFieldInsn(opCodes.PUTFIELD, newClassName, "faultInfo", beanClassCode);
            mv.visitLabel(helper.createLabel());
            mv.visitInsn(opCodes.RETURN);
            mv.visitLabel(helper.createLabel());
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = cw.visitMethod(opCodes.ACC_PUBLIC, "getFaultInfo",
                    "()" + beanClassCode, null, null);
            mv.visitCode();
            mv.visitLabel(helper.createLabel());
            mv.visitVarInsn(opCodes.ALOAD, 0);
            mv.visitFieldInsn(opCodes.GETFIELD, newClassName, "faultInfo", beanClassCode);
            mv.visitInsn(opCodes.ARETURN);
            mv.visitLabel(helper.createLabel());
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            cw.visitEnd();

            return loadClass(bean.getName() + "_Exception", bean, cw.toByteArray());
        }
        return cls;
    }
}
