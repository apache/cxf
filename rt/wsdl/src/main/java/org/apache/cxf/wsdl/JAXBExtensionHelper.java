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
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.util.StreamReaderDelegate;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchema;
import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
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
    static final Map<Class<?>, Integer> WSDL_INDENT_MAP = new HashMap<>();
    private static final Logger LOG = LogUtils.getL7dLogger(JAXBExtensionHelper.class);
    private static final int DEFAULT_INDENT_LEVEL = 2;

    final Class<?> typeClass;
    final String namespace;
    Class<?> extensionClass;
    String jaxbNamespace;

    private JAXBContext marshalContext;
    private JAXBContext unmarshalContext;
    private Set<Class<?>> classes;
    private Bus bus;


    public JAXBExtensionHelper(Bus bus, Class<?> cls,
                               String ns) {
        typeClass = cls;
        namespace = ns;
        extensionClass = cls;
        this.bus = bus;
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


    public static void addExtensions(Bus b, ExtensionRegistry registry, String parentType, String elementType)
        throws JAXBException, ClassNotFoundException {
        Class<?> parentTypeClass = ClassLoaderUtils.loadClass(parentType, JAXBExtensionHelper.class);

        Class<? extends ExtensibilityElement> elementTypeClass =
            ClassLoaderUtils.loadClass(elementType, JAXBExtensionHelper.class)
                .asSubclass(ExtensibilityElement.class);
        addExtensions(b, registry, parentTypeClass, elementTypeClass, null);
    }
    public static void addExtensions(Bus b, ExtensionRegistry registry,
                                     String parentType,
                                     String elementType,
                                     String namespace)
        throws JAXBException, ClassNotFoundException {
        Class<?> parentTypeClass = ClassLoaderUtils.loadClass(parentType, JAXBExtensionHelper.class);

        Class<? extends ExtensibilityElement> elementTypeClass =
            ClassLoaderUtils.loadClass(elementType, JAXBExtensionHelper.class)
                .asSubclass(ExtensibilityElement.class);
        addExtensions(b, registry, parentTypeClass, elementTypeClass, namespace);
    }
    public static void addExtensions(Bus b, ExtensionRegistry registry,
                                     Class<?> parentType,
                                     Class<?> cls)
        throws JAXBException {
        addExtensions(b, registry, parentType, cls, null);
    }
    public static void addExtensions(Bus b, ExtensionRegistry registry,
                                     Class<?> parentType,
                                     Class<?> cls,
                                     String namespace) throws JAXBException {
        addExtensions(b, registry, parentType, cls, namespace, cls.getClassLoader());
    }
    public static void addExtensions(Bus b, ExtensionRegistry registry,
                                     Class<?> parentType,
                                     Class<?> cls,
                                     String namespace,
                                     ClassLoader loader) throws JAXBException {

        JAXBExtensionHelper helper = new JAXBExtensionHelper(b, cls, namespace);
        ExtensionClassCreator extensionClassCreator = b.getExtension(ExtensionClassCreator.class);
        boolean found = false;
        Class<?> extCls = cls;
        try {
            Class<?> objectFactory = Class.forName(PackageUtils.getPackageName(cls) + ".ObjectFactory",
                                                   true, loader);
            Method[] methods = ReflectionUtil.getDeclaredMethods(objectFactory);
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
                            extCls = extensionClassCreator.createExtensionClass(cls, elementType, loader);
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
                        extCls = extensionClassCreator.createExtensionClass(cls, elementType, loader);
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
     * @see jakarta.wsdl.extensions.ExtensionSerializer#marshall(java.lang.Class,
     *  javax.xml.namespace.QName, jakarta.wsdl.extensions.ExtensibilityElement,
     *   java.io.PrintWriter, jakarta.wsdl.Definition, jakarta.wsdl.extensions.ExtensionRegistry)
     */
    public void marshall(@SuppressWarnings("rawtypes") Class parent, QName qname,
                         ExtensibilityElement obj, PrintWriter pw,
                         final Definition wsdl, ExtensionRegistry registry) throws WSDLException {
        try {
            Marshaller u = createMarshaller();
            u.setProperty("jaxb.encoding", StandardCharsets.UTF_8.name());
            u.setProperty("jaxb.fragment", Boolean.TRUE);
            u.setProperty("jaxb.formatted.output", Boolean.TRUE);

            Object mObj = obj;

            Class<?> objectFactory = Class.forName(PackageUtils.getPackageName(typeClass) + ".ObjectFactory",
                                                   true,
                                                   obj.getClass().getClassLoader());
            Method[] methods = objectFactory.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].equals(typeClass)) {

                    mObj = method.invoke(objectFactory.getDeclaredConstructor().newInstance(), new Object[] {obj});
                }
            }

            javax.xml.stream.XMLOutputFactory fact = javax.xml.stream.XMLOutputFactory.newInstance();
            XMLStreamWriter writer =
                new PrettyPrintXMLStreamWriter(fact.createXMLStreamWriter(pw), 2, getIndentLevel(parent));

            if (namespace != null && !namespace.equals(jaxbNamespace)) {
                Map<String, String> outMap = new HashMap<>();
                outMap.put("{" + jaxbNamespace + "}*", "{" + namespace + "}*");
                writer = new OutTransformWriter(writer,
                                                outMap,
                                                Collections.<String, String>emptyMap(),
                                                Collections.<String>emptyList(),
                                                false,
                                                "");
            }
            Map<String, String> nspref = new HashMap<>();
            for (Object ent : wsdl.getNamespaces().entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>)ent;
                nspref.put((String)entry.getValue(), (String)entry.getKey());
            }
            JAXBUtils.setNamespaceMapper(bus, nspref, u);
            u.marshal(mObj, writer);
            writer.flush();
        } catch (Exception ex) {
            throw new WSDLException(WSDLException.PARSER_ERROR,
                                    "",
                                    ex);
        }

    }

    /* (non-Javadoc)
     * @see jakarta.wsdl.extensions.ExtensionDeserializer#unmarshall(java.lang.Class,
     *  javax.xml.namespace.QName, org.w3c.dom.Element,
     *   jakarta.wsdl.Definition,
     *   jakarta.wsdl.extensions.ExtensionRegistry)
     */
    public ExtensibilityElement unmarshall(@SuppressWarnings("rawtypes") Class parent,
                                           QName qname, Element element, Definition wsdl,
                                           ExtensionRegistry registry) throws WSDLException {
        XMLStreamReader reader = null;
        Unmarshaller u = null;
        try {
            u = createUnmarshaller();

            Object o;
            if (namespace == null) {
                o = u.unmarshal(element, extensionClass);
            } else {
                reader = StaxUtils.createXMLStreamReader(element);
                reader = new MappingReaderDelegate(reader);
                o = u.unmarshal(reader, extensionClass);
            }
            if (o != null) {
                o = ((JAXBElement<?>)o).getValue();
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

                public Iterator<String> getPrefixes(String namespaceURI) {
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


}
