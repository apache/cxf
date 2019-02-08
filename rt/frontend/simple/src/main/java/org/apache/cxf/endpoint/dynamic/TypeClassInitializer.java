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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBUtils.JType;
import org.apache.cxf.common.jaxb.JAXBUtils.Mapping;
import org.apache.cxf.common.jaxb.JAXBUtils.S2JJAXBModel;
import org.apache.cxf.common.jaxb.JAXBUtils.TypeAndAnnotation;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.UnwrappedOperationInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class TypeClassInitializer extends ServiceModelVisitor {
    private static final Logger LOG = LogUtils.getL7dLogger(TypeClassInitializer.class);

    S2JJAXBModel model;
    boolean allowWrapperOperations;
    boolean isFault;

    public TypeClassInitializer(ServiceInfo serviceInfo,
                                S2JJAXBModel model,
                                boolean allowWr) {
        super(serviceInfo);
        this.model = model;
        this.allowWrapperOperations = allowWr;
    }

    @Override
    public void begin(MessagePartInfo part) {
        OperationInfo op = part.getMessageInfo().getOperation();
        if (!isFault && !allowWrapperOperations && op.isUnwrappedCapable() && !op.isUnwrapped()) {
            return;
        }

        QName name;
        if (part.isElement()) {
            name = part.getElementQName();
        } else {
            name = part.getTypeQName();
        }
        Mapping mapping = model.get(name);

        //String clsName = null;
        JType jType = null;
        if (mapping != null) {
            jType = mapping.getType().getTypeClass();
        }

        if (jType == null) {
            TypeAndAnnotation typeAndAnnotation = model.getJavaType(part.getTypeQName());
            if (typeAndAnnotation != null) {
                jType = typeAndAnnotation.getTypeClass();
            }
        }
        if (jType == null
            && part.isElement()
            && part.getXmlSchema() instanceof XmlSchemaElement
            && ((XmlSchemaElement)part.getXmlSchema()).getSchemaTypeName() == null) {
            //anonymous inner thing.....
            UnwrappedOperationInfo oInfo = (UnwrappedOperationInfo)op;
            op = oInfo.getWrappedOperation();

            if (part.getMessageInfo() == oInfo.getInput()) {
                mapping = model.get(op.getInput().getFirstMessagePart().getElementQName());
            } else {
                mapping = model.get(op.getOutput().getFirstMessagePart().getElementQName());
            }
            if (mapping != null) {
                jType = mapping.getType().getTypeClass();
                try {
                    Iterator<JType> i = jType.classes();
                    while (i.hasNext()) {
                        JType jt = i.next();
                        if (jt.name().equalsIgnoreCase(part.getElementQName().getLocalPart())) {
                            jType = jt;
                        }
                    }
                } catch (Throwable t) {
                    //ignore, JType is a type that doesn't have a classes method
                }
            }

        }

        if (jType == null) {
            throw new ServiceConstructionException(new Message("NO_JAXB_CLASSMapping", LOG, name));
        }

        Class<?> cls;

        try {
            int arrayCount = 0;
            JType rootType = jType;
            while (rootType.isArray()) {
                rootType = rootType.elementType();
                arrayCount++;
            }
            if (arrayCount == 0
                && part.isElement()
                && part.getXmlSchema() instanceof XmlSchemaElement
                && ((XmlSchemaElement)part.getXmlSchema()).getMaxOccurs() > 1) {
                arrayCount = 1;
            }
            cls = getClassByName(rootType);
            // bmargulies cannot find a way to ask the JVM to do this without creating
            // an array object on the way.
            if (arrayCount > 0) {
                int[] dimensions = new int[arrayCount];
                while (arrayCount > 0) {
                    arrayCount--;
                    dimensions[arrayCount] = 0;
                }
                Object emptyArray = Array.newInstance(cls, dimensions);
                cls = emptyArray.getClass();
            }
        } catch (ClassNotFoundException e) {
            throw new ServiceConstructionException(e);
        }

        part.setTypeClass(cls);
        if (isFault) {
            //need to create an Exception class for this
            try {
                part.getMessageInfo().setProperty(Class.class.getName(), createFaultClass(cls));
            } catch (Throwable t) {
                //ignore - probably no asm
            }
        }
        super.begin(part);
    }

    private Class<?> createFaultClass(Class<?> cls) {
        return new ExceptionCreator().createExceptionClass(cls);
    }

    private Class<?> getClassByName(JType jType) throws ClassNotFoundException {
        Class<?> cls;

        if (!jType.isPrimitive()) {
            cls = ClassLoaderUtils.loadClass(jType.binaryName(), getClass());
        } else {
            cls = PrimitiveUtils.getClass(jType.fullName());
        }
        return cls;
    }
    public void begin(FaultInfo fault) {
        isFault = true;
    }
    public void end(FaultInfo fault) {
        isFault = false;
    }


    private static class ExceptionCreator extends ASMHelper {
        public Class<?> createExceptionClass(Class<?> bean) {
            String newClassName = bean.getName() + "_Exception";
            newClassName = newClassName.replaceAll("\\$", ".");
            newClassName = periodToSlashes(newClassName);

            Class<?> cls = super.findClass(newClassName.replace('/', '.'), bean);
            if (cls == null) {
                ClassWriter cw = createClassWriter();
                cw.visit(Opcodes.V1_5,
                         Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                         newClassName,
                         null,
                         "java/lang/Exception",
                         null);

                FieldVisitor fv;
                MethodVisitor mv;

                String beanClassCode = getClassCode(bean);
                fv = cw.visitField(0, "faultInfo", beanClassCode, null, null);
                fv.visitEnd();


                mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                                    "(Ljava/lang/String;" + beanClassCode + ")V", null, null);
                mv.visitCode();
                mv.visitLabel(createLabel());
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Exception",
                                   "<init>", "(Ljava/lang/String;)V", false);
                mv.visitLabel(createLabel());
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitFieldInsn(Opcodes.PUTFIELD, newClassName, "faultInfo", beanClassCode);
                mv.visitLabel(createLabel());
                mv.visitInsn(Opcodes.RETURN);
                mv.visitLabel(createLabel());
                mv.visitMaxs(2, 3);
                mv.visitEnd();

                mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getFaultInfo",
                                    "()" + beanClassCode, null, null);
                mv.visitCode();
                mv.visitLabel(createLabel());
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, newClassName, "faultInfo", beanClassCode);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLabel(createLabel());
                mv.visitMaxs(1, 1);
                mv.visitEnd();

                cw.visitEnd();

                return super.loadClass(bean.getName() + "_Exception", bean, cw.toByteArray());
            }
            return cls;
        }
    }
}
