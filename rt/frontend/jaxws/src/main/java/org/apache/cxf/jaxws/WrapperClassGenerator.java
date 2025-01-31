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

import javax.xml.namespace.QName;

import jakarta.xml.bind.annotation.XmlAttachmentRef;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlMimeType;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import jakarta.xml.ws.Holder;
import org.apache.cxf.Bus;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.spi.ClassGeneratorClassLoader;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.OpcodesProxy;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.jaxws.spi.WrapperClassCreator;
import org.apache.cxf.jaxws.spi.WrapperClassNamingConvention;
import org.apache.cxf.jaxws.spi.WrapperClassNamingConvention.DefaultWrapperClassNamingConvention;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

public final class WrapperClassGenerator extends ClassGeneratorClassLoader implements WrapperClassCreator {
    /**
     * Kept for backwards compatibility only
     *
     * @deprecated use {@link WrapperClassNamingConvention#DEFAULT_PACKAGE_NAME} instead
     */
    @Deprecated
    public static final String DEFAULT_PACKAGE_NAME = DefaultWrapperClassNamingConvention.DEFAULT_PACKAGE_NAME;

    private static final Logger LOG = LogUtils.getL7dLogger(WrapperClassGenerator.class);
    private final ASMHelper helper;
    private final WrapperClassNamingConvention wrapperClassNaming;

    public WrapperClassGenerator(Bus bus) {
        super(bus);
        this.helper = bus.getExtension(ASMHelper.class);
        this.wrapperClassNaming = bus.getExtension(WrapperClassNamingConvention.class);
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
        List<Annotation> list = new java.util.concurrent.CopyOnWriteArrayList<>();
        Annotation[] anns = getMethodParameterAnnotations(mpi);
        if (anns != null) {
            for (Annotation anno : anns) {
                if (anno.annotationType() == XmlList.class
                    || anno.annotationType() == XmlAttachmentRef.class
                    || anno.annotationType() == XmlJavaTypeAdapter.class
                    || anno.annotationType() == XmlMimeType.class
                    || anno.annotationType() == XmlElement.class
                    || anno.annotationType() == XmlElementWrapper.class) {
                    list.add(anno);
                }
            }
        }
        return list;
    }

    public Set<Class<?>> generate(JaxWsServiceFactoryBean factory, InterfaceInfo interfaceInfo, boolean qualified) {
        Set<Class<?>> wrapperBeans = new LinkedHashSet<>();
        for (OperationInfo opInfo : interfaceInfo.getOperations()) {
            if (opInfo.isUnwrappedCapable()) {
                Method method = (Method)opInfo.getProperty(ReflectionServiceFactoryBean.METHOD);
                if (method == null) {
                    continue;
                }
                MessagePartInfo inf = opInfo.getInput().getFirstMessagePart();
                if (inf.getTypeClass() == null) {
                    MessageInfo messageInfo = opInfo.getUnwrappedOperation().getInput();
                    createWrapperClass(inf,
                                       messageInfo,
                                       opInfo,
                                       method,
                                       true,
                                       wrapperBeans,
                                       factory,
                                       interfaceInfo,
                                       qualified);
                }
                MessageInfo messageInfo = opInfo.getUnwrappedOperation().getOutput();
                if (messageInfo != null) {
                    inf = opInfo.getOutput().getFirstMessagePart();
                    if (inf.getTypeClass() == null) {
                        createWrapperClass(inf,
                                           messageInfo,
                                           opInfo,
                                           method,
                                           false,
                                            wrapperBeans,
                                            factory,
                                            interfaceInfo,
                                            qualified);
                    }
                }
            }
        }
        return wrapperBeans;
    }
    //CHECKSTYLE:OFF
    private void createWrapperClass(MessagePartInfo wrapperPart,
                                        MessageInfo messageInfo,
                                        OperationInfo op,
                                        Method method,
                                        boolean isRequest,
                                        Set<Class<?>> wrapperBeans,
                                        JaxWsServiceFactoryBean factory,
                                        InterfaceInfo interfaceInfo,
                                        boolean qualified) {


        ASMHelper.ClassWriter cw = helper.createClassWriter();
        if (cw == null) {
            LOG.warning(op.getName() + " requires a wrapper bean but problems with"
                + " ASM has prevented creating one. Operation may not work correctly.");
            return;
        }
        QName wrapperElement = messageInfo.getName();
        boolean anonymous = factory.getAnonymousWrapperTypes();

        String pkg = wrapperClassNaming.getWrapperClassPackageName(method.getDeclaringClass(), anonymous);
        String className = pkg + "."
            + StringUtils.capitalize(op.getName().getLocalPart());
        if (!isRequest) {
            className = className + "Response";
        }
        String pname = pkg + ".package-info";
        Class<?> def = findClass(pname, method.getDeclaringClass());
        if (def == null) {
            generatePackageInfo(pname, wrapperElement.getNamespaceURI(), method.getDeclaringClass(), method,
                    interfaceInfo, qualified);
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
        String classFileName = StringUtils.periodToSlashes(className);
        OpcodesProxy opCodes = helper.getOpCodes();
        cw.visit(opCodes.V1_5, opCodes.ACC_PUBLIC + opCodes.ACC_SUPER, classFileName, null,
                 "java/lang/Object", null);

        ASMHelper.AnnotationVisitor av0 = cw.visitAnnotation("Ljakarta/xml/bind/annotation/XmlRootElement;", true);
        av0.visit("name", wrapperElement.getLocalPart());
        av0.visit("namespace", wrapperElement.getNamespaceURI());
        av0.visitEnd();

        av0 = cw.visitAnnotation("Ljakarta/xml/bind/annotation/XmlAccessorType;", true);
        av0.visitEnum("value", "Ljakarta/xml/bind/annotation/XmlAccessType;", "FIELD");
        av0.visitEnd();

        av0 = cw.visitAnnotation("Ljakarta/xml/bind/annotation/XmlType;", true);
        if (!anonymous) {
            av0.visit("name", wrapperElement.getLocalPart());
            av0.visit("namespace", wrapperElement.getNamespaceURI());
        } else {
            av0.visit("name", "");
        }
        av0.visitEnd();

        // add constructor
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        ASMHelper.Label lbegin = helper.createLabel();
        mv.visitLabel(lbegin);
        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(opCodes.RETURN);
        ASMHelper.Label lend = helper.createLabel();
        mv.visitLabel(lend);
        mv.visitLocalVariable("this", "L" + classFileName + ";", null, lbegin, lend, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        for (MessagePartInfo mpi : messageInfo.getMessageParts()) {
            generateMessagePart(cw, mpi, method, classFileName, factory);
        }

        cw.visitEnd();

        Class<?> clz = loadClass(className, method.getDeclaringClass(), cw.toByteArray());
        wrapperPart.setTypeClass(clz);
        wrapperBeans.add(clz);
    }
    //CHECKSTYLE:ON
    private void generatePackageInfo(String className, String ns, Class<?> clz, Method method,
                                    InterfaceInfo interfaceInfo, boolean qualified) {
        ASMHelper.ClassWriter cw = helper.createClassWriter();
        String classFileName = StringUtils.periodToSlashes(className);
        OpcodesProxy opCodes = helper.getOpCodes();
        cw.visit(opCodes.V1_5, opCodes.ACC_ABSTRACT + opCodes.ACC_INTERFACE, classFileName, null,
                 "java/lang/Object", null);

        boolean q = qualified;
        SchemaInfo si = interfaceInfo.getService().getSchema(ns);
        if (si != null) {
            q = si.isElementFormQualified();
        }
        ASMHelper.AnnotationVisitor av0 = cw.visitAnnotation("Ljakarta/xml/bind/annotation/XmlSchema;", true);
        av0.visit("namespace", ns);
        av0.visitEnum("elementFormDefault",
                helper.getClassCode(XmlNsForm.class),
                      q ? "QUALIFIED" : "UNQUALIFIED");
        av0.visitEnd();

        if (clz.getPackage() != null && clz.getPackage().getAnnotations() != null) {
            for (Annotation ann : clz.getPackage().getAnnotations()) {
                if (ann instanceof XmlJavaTypeAdapters) {
                    av0 = cw.visitAnnotation("Ljakarta/xml/bind/annotation/adapters/XmlJavaTypeAdapters;", true);
                    generateXmlJavaTypeAdapters(av0, (XmlJavaTypeAdapters)ann);
                    av0.visitEnd();
                } else if (ann instanceof XmlJavaTypeAdapter) {
                    av0 = cw.visitAnnotation("Ljakarta/xml/bind/annotation/adapters/XmlJavaTypeAdapter;", true);
                    generateXmlJavaTypeAdapter(av0, (XmlJavaTypeAdapter)ann);
                    av0.visitEnd();
                }
            }
        }
        cw.visitEnd();

        loadClass(className, method.getDeclaringClass(), cw.toByteArray());
    }

    private void generateXmlJavaTypeAdapters(ASMHelper.AnnotationVisitor av, XmlJavaTypeAdapters adapters) {
        ASMHelper.AnnotationVisitor av1 = av.visitArray("value");

        for (XmlJavaTypeAdapter adapter : adapters.value()) {
            ASMHelper.AnnotationVisitor av2
                = av1.visitAnnotation(null, "Ljakarta/xml/bind/annotation/adapters/XmlJavaTypeAdapter;");
            generateXmlJavaTypeAdapter(av2, adapter);
            av2.visitEnd();
        }
        av1.visitEnd();
    }
    private void generateXmlJavaTypeAdapter(ASMHelper.AnnotationVisitor av, XmlJavaTypeAdapter adapter) {
        if (adapter.value() != null) {
            av.visit("value", helper.getType(helper.getClassCode(adapter.value())));
        }
        if (adapter.type() != jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter.DEFAULT.class) {
            av.visit("type", helper.getType(helper.getClassCode(adapter.type())));
        }
    }

    private void generateMessagePart(ASMHelper.ClassWriter cw, MessagePartInfo mpi,
                                     Method method, String className, JaxWsServiceFactoryBean factory) {
        if (Boolean.TRUE.equals(mpi.getProperty(ReflectionServiceFactoryBean.HEADER))) {
            return;
        }
        OpcodesProxy opCodes = helper.getOpCodes();
        String classFileName = StringUtils.periodToSlashes(className);
        String name = mpi.getName().getLocalPart();
        Class<?> clz = mpi.getTypeClass();
        Object obj = mpi.getProperty(ReflectionServiceFactoryBean.RAW_CLASS);
        if (obj != null) {
            clz = (Class<?>)obj;
        }
        Type genericType = (Type)mpi.getProperty(ReflectionServiceFactoryBean.GENERIC_TYPE);
        if (genericType instanceof ParameterizedType) {
            ParameterizedType tp = (ParameterizedType)genericType;
            if (tp.getRawType() instanceof Class
                && Holder.class.isAssignableFrom((Class<?>)tp.getRawType())) {
                genericType = tp.getActualTypeArguments()[0];
            }
        }
        String classCode = helper.getClassCode(clz);
        String fieldDescriptor = null;

        if (genericType instanceof ParameterizedType) {
            if (Collection.class.isAssignableFrom(clz) || clz.isArray()) {
                ParameterizedType ptype = (ParameterizedType)genericType;

                java.lang.reflect.Type[] types = ptype.getActualTypeArguments();
                if (types.length > 0) {
                    if (types[0] instanceof Class) {
                        fieldDescriptor = helper.getClassCode(genericType);
                    } else if (types[0] instanceof GenericArrayType) {
                        fieldDescriptor = helper.getClassCode(genericType);
                    } else if (Collection.class.isAssignableFrom(clz)) {
                        fieldDescriptor = helper.getClassCode(genericType);
                    } else if (types[0] instanceof ParameterizedType) {
                        classCode = helper.getClassCode(((ParameterizedType)types[0]).getRawType());
                        fieldDescriptor = helper.getClassCode(genericType);
                    }
                }
            } else {
                classCode = helper.getClassCode(((ParameterizedType)genericType).getRawType());
                fieldDescriptor = helper.getClassCode(genericType);
            }
        }
        String fieldName = JavaUtils.isJavaKeyword(name) ? JavaUtils.makeNonJavaKeyword(name) : name;

        ASMHelper.FieldVisitor fv = cw.visitField(opCodes.ACC_PRIVATE,
                                        fieldName,
                                        classCode,
                                        fieldDescriptor,
                                        null);



        List<Annotation> jaxbAnnos = getJaxbAnnos(mpi);
        if (!addJAXBAnnotations(fv, jaxbAnnos, name)) {
            ASMHelper.AnnotationVisitor av0 = fv.visitAnnotation("Ljakarta/xml/bind/annotation/XmlElement;", true);
            av0.visit("name", name);
            if (Boolean.TRUE.equals(factory.isWrapperPartQualified(mpi))) {
                av0.visit("namespace", mpi.getConcreteName().getNamespaceURI());
            }
            if (factory.isWrapperPartNillable(mpi)) {
                av0.visit("nillable", Boolean.TRUE);
            }
            if (factory.getWrapperPartMinOccurs(mpi) == 1) {
                av0.visit("required", Boolean.TRUE);
            }
            av0.visitEnd();
        }
        fv.visitEnd();

        String methodName = JAXBUtils.nameToIdentifier(name, JAXBUtils.IdentifierType.GETTER);
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC, methodName, "()" + classCode,
                                          fieldDescriptor == null ? null : "()" + fieldDescriptor,
                                          null);
        mv.visitCode();

        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitFieldInsn(opCodes.GETFIELD, classFileName, fieldName, classCode);
        mv.visitInsn(helper.getType(classCode).getOpcode(opCodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        methodName = JAXBUtils.nameToIdentifier(name, JAXBUtils.IdentifierType.SETTER);
        mv = cw.visitMethod(opCodes.ACC_PUBLIC, methodName, "(" + classCode + ")V",
                            fieldDescriptor == null ? null : "(" + fieldDescriptor + ")V", null);
        mv.visitCode();
        mv.visitVarInsn(opCodes.ALOAD, 0);
        ASMHelper.ASMType setType = helper.getType(classCode);
        mv.visitVarInsn(setType.getOpcode(opCodes.ILOAD), 1);
        mv.visitFieldInsn(opCodes.PUTFIELD, className, fieldName, classCode);
        mv.visitInsn(opCodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }

    private boolean addJAXBAnnotations(ASMHelper.FieldVisitor fv,
                                       List<Annotation> jaxbAnnos,
                                       String name) {
        ASMHelper.AnnotationVisitor av0;
        boolean addedEl = false;
        for (Annotation ann : jaxbAnnos) {
            if (ann instanceof XmlMimeType) {
                av0 = fv.visitAnnotation("Ljakarta/xml/bind/annotation/XmlMimeType;", true);
                av0.visit("value", ((XmlMimeType)ann).value());
                av0.visitEnd();
            } else if (ann instanceof XmlJavaTypeAdapter) {
                av0 = fv.visitAnnotation("Ljakarta/xml/bind/annotation/adapters/XmlJavaTypeAdapter;", true);
                generateXmlJavaTypeAdapter(av0, (XmlJavaTypeAdapter)ann);
                av0.visitEnd();
            } else if (ann instanceof XmlAttachmentRef) {
                av0 = fv.visitAnnotation("Ljakarta/xml/bind/annotation/XmlAttachmentRef;", true);
                av0.visitEnd();
            } else if (ann instanceof XmlList) {
                av0 = fv.visitAnnotation("Ljakarta/xml/bind/annotation/XmlList;", true);
                av0.visitEnd();
            } else if (ann instanceof XmlElement) {
                addedEl = true;
                XmlElement el = (XmlElement)ann;
                av0 = fv.visitAnnotation("Ljakarta/xml/bind/annotation/XmlElement;", true);
                if ("##default".equals(el.name())) {
                    av0.visit("name", name);
                } else {
                    av0.visit("name", el.name());
                }
                av0.visit("nillable", el.nillable());
                av0.visit("required", el.required());
                av0.visit("namespace", el.namespace());
                av0.visit("defaultValue", el.defaultValue());
                if (el.type() != XmlElement.DEFAULT.class) {
                    av0.visit("type", el.type());
                }
                av0.visitEnd();
            } else if (ann instanceof XmlElementWrapper) {
                XmlElementWrapper el = (XmlElementWrapper)ann;
                av0 = fv.visitAnnotation("Ljakarta/xml/bind/annotation/XmlElementWrapper;", true);
                av0.visit("name", el.name());
                av0.visit("nillable", el.nillable());
                av0.visit("required", el.required());
                av0.visit("namespace", el.namespace());
                av0.visitEnd();
            }
        }
        return addedEl;
    }


}
