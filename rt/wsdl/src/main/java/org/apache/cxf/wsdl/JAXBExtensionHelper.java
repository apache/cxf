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

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionDeserializer;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.util.StreamReaderDelegate;

import org.w3c.dom.Element;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.ASMHelper.AnnotationVisitor;
import org.apache.cxf.common.util.ASMHelper.ClassWriter;
import org.apache.cxf.common.util.ASMHelper.FieldVisitor;
import org.apache.cxf.common.util.ASMHelper.Label;
import org.apache.cxf.common.util.ASMHelper.MethodVisitor;
import org.apache.cxf.common.util.ASMHelper.Opcodes;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.OutTransformWriter;


/**
 * JAXBExtensionHelper
 */
public class JAXBExtensionHelper implements ExtensionSerializer, ExtensionDeserializer {
    static final Map<Class<?>, Integer> WSDL_INDENT_MAP = new HashMap<Class<?>, Integer>();
    private static final Logger LOG = LogUtils.getL7dLogger(JAXBExtensionHelper.class);
    private static final int DEFAULT_INDENT_LEVEL = 2;

    final Class<?> typeClass;
    final String namespace;
    Class<?> extensionClass;
    String jaxbNamespace;

    private JAXBContext marshalContext;
    private JAXBContext unmarshalContext;
    private Set<Class<?>> classes;

      
    public JAXBExtensionHelper(Class<?> cls,
                               String ns) {
        typeClass = cls;
        namespace = ns;
        extensionClass = cls;
    }
    
    void setJaxbNamespace(String ns) {
        jaxbNamespace = ns;
    }
    void setExtensionClass(Class<?> cls) {
        extensionClass = cls;
    }
    
    private int getIndentLevel(Class<?> parent) {
        Integer result = WSDL_INDENT_MAP.get(parent);
        if (result == null) {
            return DEFAULT_INDENT_LEVEL;
        }
        return result.intValue();
    }

    static {
        WSDL_INDENT_MAP.put(Definition.class, Integer.valueOf(DEFAULT_INDENT_LEVEL));
        WSDL_INDENT_MAP.put(Binding.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 2));
        WSDL_INDENT_MAP.put(BindingFault.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(BindingInput.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(BindingOutput.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(BindingOperation.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(Message.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 2));
        WSDL_INDENT_MAP.put(Operation.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(Port.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(Service.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 2));
        WSDL_INDENT_MAP.put(Types.class, Integer.valueOf(DEFAULT_INDENT_LEVEL * 2));
    }
    
    
    public static void addExtensions(ExtensionRegistry registry, String parentType, String elementType)
        throws JAXBException, ClassNotFoundException {
        Class<?> parentTypeClass = ClassLoaderUtils.loadClass(parentType, JAXBExtensionHelper.class);

        Class<? extends ExtensibilityElement> elementTypeClass = 
            ClassLoaderUtils.loadClass(elementType, JAXBExtensionHelper.class)
                .asSubclass(ExtensibilityElement.class);
        addExtensions(registry, parentTypeClass, elementTypeClass, null);
    }
    public static void addExtensions(ExtensionRegistry registry,
                                     String parentType, 
                                     String elementType,
                                     String namespace)
        throws JAXBException, ClassNotFoundException {
        Class<?> parentTypeClass = ClassLoaderUtils.loadClass(parentType, JAXBExtensionHelper.class);

        Class<? extends ExtensibilityElement> elementTypeClass = 
            ClassLoaderUtils.loadClass(elementType, JAXBExtensionHelper.class)
                .asSubclass(ExtensibilityElement.class);
        addExtensions(registry, parentTypeClass, elementTypeClass, namespace);
    }
    public static void addExtensions(ExtensionRegistry registry,
                                     Class<?> parentType,
                                     Class<?> cls)
        throws JAXBException {
        addExtensions(registry, parentType, cls, null);
    }
    public static void addExtensions(ExtensionRegistry registry,
                                     Class<?> parentType,
                                     Class<?> cls,
                                     String namespace) throws JAXBException {
        addExtensions(registry, parentType, cls, namespace, cls.getClassLoader());
    }
    public static void addExtensions(ExtensionRegistry registry,
                                     Class<?> parentType,
                                     Class<?> cls,
                                     String namespace,
                                     ClassLoader loader) throws JAXBException {
        
        JAXBExtensionHelper helper = new JAXBExtensionHelper(cls, namespace);
        boolean found = false;
        Class<?> extCls = cls;
        try {
            Class<?> objectFactory = Class.forName(PackageUtils.getPackageName(cls) + ".ObjectFactory",
                                                   true, loader);
            Method methods[] = ReflectionUtil.getDeclaredMethods(objectFactory);
            for (Method method : methods) {
                if (method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].equals(cls)) {
                    
                    XmlElementDecl elementDecl = method.getAnnotation(XmlElementDecl.class);
                    if (null != elementDecl) {
                        String name = elementDecl.name();
                        String ns = namespace != null ? namespace : elementDecl.namespace();
                        if (namespace != null) {
                            helper.setJaxbNamespace(elementDecl.namespace());
                        }
                        QName elementType = new QName(ns, name);
                        if (!ExtensibilityElement.class.isAssignableFrom(extCls)) {
                            extCls = createExtensionClass(cls, elementType, loader);
                            helper.setExtensionClass(extCls);
                        }
                        registry.registerDeserializer(parentType, elementType, helper); 
                        registry.registerSerializer(parentType, elementType, helper);                         
                        registry.mapExtensionTypes(parentType, elementType, extCls);
                        found = true;
                    }                    
                }
            }        
            
        } catch (ClassNotFoundException ex) {
            //ignore
        }        
        if (!found) {
            //not in object factory or no object factory, try other annotations
            XmlRootElement elAnnot = cls.getAnnotation(XmlRootElement.class);
            if (elAnnot != null) {
                String name = elAnnot.name();
                String ns = elAnnot.namespace();
                if (StringUtils.isEmpty(ns)
                    || "##default".equals(ns)) {
                    XmlSchema schema = null;
                    if (cls.getPackage() != null) {
                        schema = cls.getPackage().getAnnotation(XmlSchema.class);
                    }
                    if (schema != null) {
                        ns = schema.namespace();
                    }
                }
                if (!StringUtils.isEmpty(ns) && !StringUtils.isEmpty(name)) {
                    if (namespace != null) {
                        helper.setJaxbNamespace(ns);
                        ns = namespace;
                    }
                    QName elementType = new QName(ns, name);
                    if (!ExtensibilityElement.class.isAssignableFrom(extCls)) {
                        extCls = createExtensionClass(cls, elementType, loader);
                        helper.setExtensionClass(extCls);
                    }
                    registry.registerDeserializer(parentType, elementType, helper); 
                    registry.registerSerializer(parentType, elementType, helper);                         
                    registry.mapExtensionTypes(parentType, elementType, extCls);

                    found = true;
                }
            }
        }
        
        if (!found) {
            LOG.log(Level.WARNING, "EXTENSION_NOT_REGISTERED", 
                    new Object[] {cls.getName(), parentType.getName()});
        }
    }

    private synchronized Unmarshaller createUnmarshaller() throws JAXBException {
        if (unmarshalContext == null || classes == null) {
            try {
                CachedContextAndSchemas ccs 
                    = JAXBContextCache.getCachedContextAndSchemas(extensionClass);
                classes = ccs.getClasses();
                unmarshalContext = ccs.getContext();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return unmarshalContext.createUnmarshaller();
    }
    private synchronized Marshaller createMarshaller() throws JAXBException {
        if (marshalContext == null || classes == null) {
            try {
                CachedContextAndSchemas ccs 
                    = JAXBContextCache.getCachedContextAndSchemas(typeClass);
                classes = ccs.getClasses();
                marshalContext = ccs.getContext();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return marshalContext.createMarshaller();
    }
    
    /* (non-Javadoc)
     * @see javax.wsdl.extensions.ExtensionSerializer#marshall(java.lang.Class,
     *  javax.xml.namespace.QName, javax.wsdl.extensions.ExtensibilityElement,
     *   java.io.PrintWriter, javax.wsdl.Definition, javax.wsdl.extensions.ExtensionRegistry)
     */
    public void marshall(@SuppressWarnings("rawtypes") Class parent, QName qname,
                         ExtensibilityElement obj, PrintWriter pw,
                         final Definition wsdl, ExtensionRegistry registry) throws WSDLException {
        // TODO Auto-generated method stub
        try {
            Marshaller u = createMarshaller();
            u.setProperty("jaxb.encoding", StandardCharsets.UTF_8.name());
            u.setProperty("jaxb.fragment", Boolean.TRUE);
            u.setProperty("jaxb.formatted.output", Boolean.TRUE);
            
            Object mObj = obj;
            
            Class<?> objectFactory = Class.forName(PackageUtils.getPackageName(typeClass) + ".ObjectFactory",
                                                   true,
                                                   obj.getClass().getClassLoader());
            Method methods[] = objectFactory.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].equals(typeClass)) {
                    
                    mObj = method.invoke(objectFactory.newInstance(), new Object[] {obj});
                }
            }
            
            javax.xml.stream.XMLOutputFactory fact = javax.xml.stream.XMLOutputFactory.newInstance();
            XMLStreamWriter writer =
                new PrettyPrintXMLStreamWriter(fact.createXMLStreamWriter(pw), 2, getIndentLevel(parent));
            
            if (namespace != null && !namespace.equals(jaxbNamespace)) {
                Map<String, String> outMap = new HashMap<String, String>();
                outMap.put("{" + jaxbNamespace + "}*", "{" + namespace + "}*");
                writer = new OutTransformWriter(writer,
                                                outMap,
                                                Collections.<String, String>emptyMap(),
                                                Collections.<String>emptyList(),
                                                false,
                                                "");
            }
            Map<String, String> nspref = new HashMap<String, String>();
            for (Object ent : wsdl.getNamespaces().entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>)ent;
                nspref.put((String)entry.getValue(), (String)entry.getKey());
            }
            JAXBUtils.setNamespaceMapper(nspref, u);
            u.marshal(mObj, writer);
            writer.flush();            
        } catch (Exception ex) {
            throw new WSDLException(WSDLException.PARSER_ERROR,
                                    "",
                                    ex);
        }

    }

    /* (non-Javadoc)
     * @see javax.wsdl.extensions.ExtensionDeserializer#unmarshall(java.lang.Class,
     *  javax.xml.namespace.QName, org.w3c.dom.Element,
     *   javax.wsdl.Definition,
     *   javax.wsdl.extensions.ExtensionRegistry)
     */
    public ExtensibilityElement unmarshall(@SuppressWarnings("rawtypes") Class parent, 
                                           QName qname, Element element, Definition wsdl,
                                           ExtensionRegistry registry) throws WSDLException {
        XMLStreamReader reader = null;
        Unmarshaller u = null;
        try {
            u = createUnmarshaller();
        
            Object o = null;
            if (namespace == null) {
                o = u.unmarshal(element, extensionClass);
            } else {
                reader = StaxUtils.createXMLStreamReader(element);
                reader = new MappingReaderDelegate(reader);
                o = u.unmarshal(reader, extensionClass);
            }
            if (o instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)o;
                o = el.getValue();
            }
            
            ExtensibilityElement el = o instanceof ExtensibilityElement ? (ExtensibilityElement)o 
                : new JAXBExtensibilityElement(o);
            el.setElementType(qname);
            return el;
        } catch (Exception ex) {
            throw new WSDLException(WSDLException.PARSER_ERROR,
                                    "Error reading element " + qname,
                                    ex);
        } finally {
            try {
                StaxUtils.close(reader);
            } catch (XMLStreamException ex) {
                throw new WSDLException(WSDLException.PARSER_ERROR, ex.getMessage(), ex);
            }
            JAXBUtils.closeUnmarshaller(u);
        }
    }
    
    class MappingReaderDelegate extends StreamReaderDelegate {
        MappingReaderDelegate(XMLStreamReader reader) {
            super(reader);
        }
        
        @Override
        public NamespaceContext getNamespaceContext() {
            final NamespaceContext ctx = super.getNamespaceContext();
            return new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    String ns = ctx.getNamespaceURI(prefix);
                    if (namespace.equals(ns)) {
                        ns = jaxbNamespace;
                    }                        
                    return ns;
                }

                public String getPrefix(String namespaceURI) {
                    if (jaxbNamespace.equals(namespaceURI)) {
                        return ctx.getPrefix(namespace);
                    }
                    return ctx.getPrefix(namespaceURI);
                }

                @SuppressWarnings("rawtypes")
                public Iterator getPrefixes(String namespaceURI) {
                    if (jaxbNamespace.equals(namespaceURI)) {
                        return ctx.getPrefixes(namespace);
                    }
                    return ctx.getPrefixes(namespaceURI);
                }
            };
        }

        @Override
        public String getNamespaceURI(int index) {
            String ns = super.getNamespaceURI(index);
            if (namespace.equals(ns)) {
                ns = jaxbNamespace;
            }                        
            return ns;                     
        }

        @Override
        public String getNamespaceURI(String prefix) {
            String ns = super.getNamespaceURI(prefix);
            if (namespace.equals(ns)) {
                ns = jaxbNamespace;
            }                        
            return ns;
        }

        @Override
        public QName getName() {
            QName qn = super.getName();
            if (namespace.equals(qn.getNamespaceURI())) {
                qn = new QName(jaxbNamespace, qn.getLocalPart());
            }
            return qn;
        }

        @Override
        public String getNamespaceURI() {
            String ns = super.getNamespaceURI();
            if (namespace.equals(ns)) {
                ns = jaxbNamespace;
            }                        
            return ns; 
        }
        
    };
    
    //CHECKSTYLE:OFF - very complicated ASM code
    private static Class<?> createExtensionClass(Class<?> cls, QName qname, ClassLoader loader) {
        
        String className = ASMHelper.periodToSlashes(cls.getName());
        ASMHelper helper = new ASMHelper();
        Class<?> extClass = helper.findClass(className + "Extensibility", loader);
        if (extClass != null) {
            return extClass;
        }
        
        ClassWriter cw = helper.createClassWriter();
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;
        
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
            Label l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(64, l0);
            mv.visitTypeInsn(Opcodes.NEW, "javax/xml/namespace/QName");
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn("http://schemas.xmlsoap.org/wsdl/");
            mv.visitLdcInsn("required");
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "javax/xml/namespace/QName", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", false);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, className + "Extensibility", "WSDL_REQUIRED", "Ljavax/xml/namespace/QName;");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(4, 0);
            mv.visitEnd();
        } else {
            fv = cw.visitField(Opcodes.ACC_PRIVATE, "required", "Ljava/lang/Boolean;", null, null);
            fv.visitEnd();
        }
        
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(33, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", "()V", false);
        Label l1 = helper.createLabel();
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
        Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(34, l2);
        mv.visitInsn(Opcodes.RETURN);
        Label l3 = helper.createLabel();
        mv.visitLabel(l3);
       
        mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l3, 0);
        mv.visitMaxs(5, 1);
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
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getElementType", "()Ljavax/xml/namespace/QName;", null, null);
        av0 = mv.visitAnnotation("Ljavax/xml/bind/annotation/XmlTransient;", true);
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
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        if (hasAttributes) {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getRequired", "()Ljava/lang/Boolean;", null, null);
            mv.visitCode();
            l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(66, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className + "Extensibility", "getOtherAttributes", "()Ljava/util/Map;", false);
            mv.visitFieldInsn(Opcodes.GETSTATIC, className + "Extensibility", "WSDL_REQUIRED", "Ljavax/xml/namespace/QName;");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
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
            mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/String"}, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Ljava/lang/String;)Ljava/lang/Boolean;", false);
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Boolean"});
            mv.visitInsn(Opcodes.ARETURN);
            Label l4 = helper.createLabel();
            mv.visitLabel(l4);
            mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l4, 0);
            mv.visitLocalVariable("s", "Ljava/lang/String;", null, l1, l4, 1);
            mv.visitMaxs(2, 2);
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
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className + "Extensibility", "getOtherAttributes", "()Ljava/util/Map;", false);
            mv.visitFieldInsn(Opcodes.GETSTATIC, className + "Extensibility", "WSDL_REQUIRED", "Ljavax/xml/namespace/QName;");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
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
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className + "Extensibility", "getOtherAttributes", "()Ljava/util/Map;", false);
            mv.visitFieldInsn(Opcodes.GETSTATIC, className + "Extensibility", "WSDL_REQUIRED", "Ljavax/xml/namespace/QName;");
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(Opcodes.POP);
            mv.visitLabel(l4);
            mv.visitLineNumber(81, l4);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(Opcodes.RETURN);
            Label l5 = helper.createLabel();
            mv.visitLabel(l5);
            mv.visitLocalVariable("this", "L" + className + "Extensibility;", null, l0, l5, 0);
            mv.visitLocalVariable("b", "Ljava/lang/Boolean;", null, l0, l5, 1);
            mv.visitMaxs(3, 2);
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
            mv.visitMaxs(1, 1);
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
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }

        cw.visitEnd();

        byte[] bytes = cw.toByteArray();
        return helper.loadClass(className + "Extensibility", loader, bytes);
    }    

}
