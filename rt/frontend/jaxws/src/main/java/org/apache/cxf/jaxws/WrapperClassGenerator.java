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

package org.apache.cxf.jaxws;


import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class WrapperClassGenerator extends ASMHelper {
    private static final Logger LOG = LogUtils.getL7dLogger(WrapperClassGenerator.class);
    private Set<Class<?>> wrapperBeans = new LinkedHashSet<Class<?>>();
    private InterfaceInfo interfaceInfo;
    private boolean qualified;
    private JaxWsServiceFactoryBean factory;
    
    public WrapperClassGenerator(JaxWsServiceFactoryBean fact, InterfaceInfo inf, boolean q) {
        factory = fact;
        interfaceInfo = inf;
        qualified = q;
    }

    private String getPackageName(Method method) {
        String pkg = PackageUtils.getPackageName(method.getDeclaringClass());
        return pkg.length() == 0 ? ToolConstants.DEFAULT_PACKAGE_NAME : pkg;
    }

    private Annotation[] getMethodParameterAnnotations(final MessagePartInfo mpi) {
        Annotation[] a = (Annotation[])mpi.getProperty(ReflectionServiceFactoryBean.PARAM_ANNOTATION);
        if (a != null) {
            return a;
        }
        
        Annotation[][] paramAnno = (Annotation[][])mpi
            .getProperty(ReflectionServiceFactoryBean.METHOD_PARAM_ANNOTATIONS);
        int index = mpi.getIndex();
        if (paramAnno != null && index < paramAnno.length && index >= 0) {
            return paramAnno[index];
        }
        return null;
    }

    private List<Annotation> getJaxbAnnos(MessagePartInfo mpi) {
        List<Annotation> list = new java.util.concurrent.CopyOnWriteArrayList<Annotation>();
        Annotation[] anns = getMethodParameterAnnotations(mpi);
        if (anns != null) {
            for (Annotation anno : anns) {
                if (anno.annotationType() == XmlList.class 
                    || anno.annotationType() == XmlAttachmentRef.class
                    || anno.annotationType() == XmlJavaTypeAdapter.class
                    || anno.annotationType() == XmlMimeType.class) {
                    list.add(anno);
                }
            }
        }
        return list;
    }

    public Set<Class<?>> generate() {
        try {
            if (createClassWriter() == null) {
                throw new ClassNotFoundException();
            }
        } catch (Throwable t) {
            for (OperationInfo opInfo : interfaceInfo.getOperations()) {
                if (opInfo.isUnwrappedCapable()
                    && (opInfo.getUnwrappedOperation()
                        .getProperty(ReflectionServiceFactoryBean.WRAPPERGEN_NEEDED) != null)) {
                    LOG.warning(opInfo.getName() + "requires a wrapper bean but problems with"
                                + " ASM has prevented creating one.  Operation may not work correctly.");
                }
            }
            return wrapperBeans;
        }
        for (OperationInfo opInfo : interfaceInfo.getOperations()) {
            if (opInfo.isUnwrappedCapable()) {
                Method method = (Method)opInfo.getProperty(ReflectionServiceFactoryBean.METHOD);
                if (method == null) {
                    continue;
                }
                MessagePartInfo inf = opInfo.getInput().getMessageParts().get(0);
                if (inf.getTypeClass() == null) {
                    MessageInfo messageInfo = opInfo.getUnwrappedOperation().getInput();
                    createWrapperClass(inf,
                                       messageInfo, 
                                       opInfo,
                                       method, 
                                       true);
                }
                MessageInfo messageInfo = opInfo.getUnwrappedOperation().getOutput();
                if (messageInfo != null) {
                    inf = opInfo.getOutput().getMessageParts().get(0);
                    if (inf.getTypeClass() == null) {
                        createWrapperClass(inf,
                                           messageInfo,
                                           opInfo,
                                           method, 
                                           false);
                    }
                }
            }
        }
        return wrapperBeans;
    }

    private void createWrapperClass(MessagePartInfo wrapperPart,
                                        MessageInfo messageInfo,
                                        OperationInfo op,
                                        Method method, 
                                        boolean isRequest) {

        QName wrapperElement = messageInfo.getName();
        
        boolean anonymous = factory.getAnonymousWrapperTypes();

        ClassWriter cw = createClassWriter();
        String pkg = getPackageName(method) + ".jaxws_asm" + (anonymous ? "_an" : "");
        String className =  pkg + "." 
            + StringUtils.capitalize(op.getName().getLocalPart());
        if (!isRequest) {
            className = className + "Response";
        }
        String pname = pkg + ".package-info";
        Class<?> def = findClass(pname, method.getDeclaringClass());
        if (def == null) {
            generatePackageInfo(pname, wrapperElement.getNamespaceURI(),
                                method.getDeclaringClass());
        }
        
        def = findClass(className, method.getDeclaringClass());
        String origClassName = className;
        int count = 0;
        while (def != null) {
            Boolean b = messageInfo.getProperty("parameterized", Boolean.class);
            if (b != null && b) {
                className = origClassName + (++count);
                def = findClass(className, method.getDeclaringClass());
            } else {
                wrapperPart.setTypeClass(def);
                wrapperBeans.add(def);
                return;
            }
        }
        String classFileName = periodToSlashes(className);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, classFileName, null,
                 "java/lang/Object", null);

        AnnotationVisitor av0 = cw.visitAnnotation("Ljavax/xml/bind/annotation/XmlRootElement;", true);
        av0.visit("name", wrapperElement.getLocalPart());
        av0.visit("namespace", wrapperElement.getNamespaceURI());
        av0.visitEnd();

        av0 = cw.visitAnnotation("Ljavax/xml/bind/annotation/XmlAccessorType;", true);
        av0.visitEnum("value", "Ljavax/xml/bind/annotation/XmlAccessType;", "FIELD");
        av0.visitEnd();

        av0 = cw.visitAnnotation("Ljavax/xml/bind/annotation/XmlType;", true);
        if (!anonymous) {
            av0.visit("name", wrapperElement.getLocalPart());
            av0.visit("namespace", wrapperElement.getNamespaceURI());
        } else {
            av0.visit("name", "");            
        }
        av0.visitEnd();

        // add constructor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label lbegin = new Label();
        mv.visitLabel(lbegin);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        Label lend = new Label();
        mv.visitLabel(lend);
        mv.visitLocalVariable("this", "L" + classFileName + ";", null, lbegin, lend, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        for (MessagePartInfo mpi : messageInfo.getMessageParts()) {
            generateMessagePart(cw, mpi, method, classFileName);
        }

        cw.visitEnd();

        Class<?> clz = loadClass(className, method.getDeclaringClass(), cw.toByteArray());
        wrapperPart.setTypeClass(clz);
        wrapperBeans.add(clz);
    }

    private void generatePackageInfo(String className, String ns, Class clz) {
        ClassWriter cw = createClassWriter();
        String classFileName = periodToSlashes(className);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE, classFileName, null,
                 "java/lang/Object", null);
        
        boolean q = qualified;
        SchemaInfo si = interfaceInfo.getService().getSchema(ns);
        if (si != null) {
            q = si.isElementFormQualified();
        }
        AnnotationVisitor av0 = cw.visitAnnotation("Ljavax/xml/bind/annotation/XmlSchema;", true);
        av0.visit("namespace", ns);
        av0.visitEnum("elementFormDefault",
                      getClassCode(XmlNsForm.class),
                      q ? "QUALIFIED" : "UNQUALIFIED");
        av0.visitEnd();
        cw.visitEnd();

        loadClass(className, clz, cw.toByteArray());
    }

    private void generateMessagePart(ClassWriter cw, MessagePartInfo mpi, Method method, String className) {
        if (Boolean.TRUE.equals(mpi.getProperty(ReflectionServiceFactoryBean.HEADER))) {
            return;
        }
        String classFileName = periodToSlashes(className);
        String name = mpi.getName().getLocalPart();
        Class clz = mpi.getTypeClass();
        Object obj = mpi.getProperty(ReflectionServiceFactoryBean.RAW_CLASS);
        if (obj != null) {
            clz = (Class)obj;
        }
        Type genericType = (Type)mpi.getProperty(ReflectionServiceFactoryBean.GENERIC_TYPE);
        if (genericType instanceof ParameterizedType) {
            ParameterizedType tp = (ParameterizedType)genericType;
            if (tp.getRawType() instanceof Class
                && Holder.class.isAssignableFrom((Class)tp.getRawType())) {
                genericType = tp.getActualTypeArguments()[0];
            }
        }
        String classCode = getClassCode(clz);
        String fieldDescriptor = null;
        
        if (genericType instanceof ParameterizedType) {
            if (Collection.class.isAssignableFrom(clz) || clz.isArray()) {
                ParameterizedType ptype = (ParameterizedType)genericType;

                Type[] types = ptype.getActualTypeArguments();
                // TODO: more complex Parameterized type
                if (types.length > 0) {
                    if (types[0] instanceof Class) {
                        fieldDescriptor = getClassCode(genericType);
                    } else if (types[0] instanceof GenericArrayType) {
                        fieldDescriptor = getClassCode(genericType);
                    } else if (types[0] instanceof ParameterizedType) {
                        classCode = getClassCode(((ParameterizedType)types[0]).getRawType());
                        fieldDescriptor = getClassCode(genericType);
                    }
                }
            } else {
                classCode = getClassCode(((ParameterizedType)genericType).getRawType());
                fieldDescriptor = getClassCode(genericType);
            }
        }
        String fieldName = JavaUtils.isJavaKeyword(name) ? JavaUtils.makeNonJavaKeyword(name) : name;
        
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE,
                                        fieldName, 
                                        classCode,
                                        fieldDescriptor,
                                        null);
        
        
        AnnotationVisitor av0 = fv.visitAnnotation("Ljavax/xml/bind/annotation/XmlElement;", true);
        av0.visit("name", name);
        if (factory.isWrapperPartQualified(mpi)) {
            av0.visit("namespace", mpi.getConcreteName().getNamespaceURI());            
        }
        if (factory.isWrapperPartNillable(mpi)) {
            av0.visit("nillable", Boolean.TRUE);
        }
        if (factory.getWrapperPartMinOccurs(mpi) == 1) {
            av0.visit("required", Boolean.TRUE);
        }
        av0.visitEnd();

        List<Annotation> jaxbAnnos = getJaxbAnnos(mpi);
        addJAXBAnnotations(fv, jaxbAnnos);
        fv.visitEnd();

        String methodName = JAXBUtils.nameToIdentifier(name, JAXBUtils.IdentifierType.GETTER);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "()" + classCode, 
                                          fieldDescriptor == null ? null : "()" + fieldDescriptor,
                                          null);
        mv.visitCode();

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, classFileName, fieldName, classCode);
        mv.visitInsn(org.objectweb.asm.Type.getType(classCode).getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        methodName = JAXBUtils.nameToIdentifier(name, JAXBUtils.IdentifierType.SETTER);
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "(" + classCode + ")V",
                            fieldDescriptor == null ? null : "(" + fieldDescriptor + ")V", null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        org.objectweb.asm.Type setType = org.objectweb.asm.Type.getType(classCode);
        mv.visitVarInsn(setType.getOpcode(Opcodes.ILOAD), 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, classCode);       
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }
     
    private void addJAXBAnnotations(FieldVisitor fv, List<Annotation> jaxbAnnos) {
        AnnotationVisitor av0;
        for (Annotation ann : jaxbAnnos) {
            if (ann instanceof XmlMimeType) {
                av0 = fv.visitAnnotation("Ljavax/xml/bind/annotation/XmlMimeType;", true);
                av0.visit("value", ((XmlMimeType)ann).value());
                av0.visitEnd();
            } else if (ann instanceof XmlJavaTypeAdapter) {
                av0 = fv.visitAnnotation("Ljavax/xml/bind/annotation/adapters/XmlJavaTypeAdapter;", true);
                XmlJavaTypeAdapter adapter = (XmlJavaTypeAdapter)ann;
                if (adapter.value() != null) {
                    av0.visit("value", org.objectweb.asm.Type.getType(getClassCode(adapter.value())));
                }
                if (adapter.type() != javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter.DEFAULT.class) {
                    av0.visit("type", org.objectweb.asm.Type.getType(getClassCode(adapter.type())));
                }
                av0.visitEnd();
            } else if (ann instanceof XmlAttachmentRef) {
                av0 = fv.visitAnnotation("Ljavax/xml/bind/annotation/XmlAttachmentRef;", true);
                av0.visitEnd();
            } else if (ann instanceof XmlList) {
                av0 = fv.visitAnnotation("Ljavax/xml/bind/annotation/XmlList;", true);
                av0.visitEnd();
            }
        }
    }
    
    
}
