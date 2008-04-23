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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.api.TypeReference;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;

/**
 * Utility functions for JAXB.
 */
public final class JAXBEncoderDecoder {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXBEncoderDecoder.class);
    private static final Logger LOG = LogUtils.getLogger(JAXBEncoderDecoder.class);

    private JAXBEncoderDecoder() {
    }

    private static Marshaller createMarshaller(JAXBContext context, 
                                               Class<?> cls,
                                               Map<String, Object> marshallerProperties) throws 
                                               JAXBException {
        Marshaller jm = null;
        if (context == null) {
            context = JAXBContext.newInstance(cls);
        }

        jm = context.createMarshaller();
        jm.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        jm.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        if (marshallerProperties != null) {
            for (Map.Entry<String, Object> propEntry : marshallerProperties.entrySet()) {
                try {
                    jm.setProperty(propEntry.getKey(), propEntry.getValue());
                } catch (PropertyException pe) {
                    LOG.log(Level.INFO, "PropertyException setting Marshaller properties", pe);
                }
            }
        }

        return jm;
    }

    @SuppressWarnings("unchecked")
    public static void marshall(JAXBContext context, Schema schema, Object elValue, MessagePartInfo part,
                                Object source, AttachmentMarshaller am,
                                Map<String, Object> marshallerProperties) {
        Class<?> cls = null;
        if (part != null) {
            cls = part.getTypeClass();
        }

        if (cls == null) {
            cls = null != elValue ? elValue.getClass() : null;
        }

        if (cls != null && cls.isArray() && elValue instanceof Collection) {
            Collection<?> col = (Collection<?>)elValue;
            elValue = col.toArray((Object[])Array.newInstance(cls.getComponentType(), col.size()));
        }

        try {
            Marshaller u = createMarshaller(context, cls, marshallerProperties);
            try {
                // The Marshaller.JAXB_FRAGMENT will tell the Marshaller not to
                // generate the xml declaration.
                u.setProperty(Marshaller.JAXB_FRAGMENT, true);
                u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            } catch (javax.xml.bind.PropertyException e) {
                // intentionally empty.
            }
            Object mObj = elValue;
            QName elName = null;
            if (part != null) {
                elName = part.getConcreteName();
            }
            u.setSchema(schema);
            if (am != null) {
                u.setAttachmentMarshaller(am);
            }

            if (null != elName) {

                if (part != null && part.getXmlSchema() instanceof XmlSchemaElement) {

                    XmlSchemaElement el = (XmlSchemaElement)part.getXmlSchema();

                    if (mObj.getClass().isArray()
                        && el.getSchemaType() instanceof XmlSchemaSimpleType
                        && ((XmlSchemaSimpleType)el.getSchemaType()).
                        getContent() instanceof XmlSchemaSimpleTypeList) {
                        mObj = Arrays.asList((Object[])mObj);
                        writeObject(u, source, new JAXBElement(elName, cls, mObj));
                    } else if (part.getMessageInfo().getOperation().isUnwrapped()
                               && (mObj.getClass().isArray() || mObj instanceof List)
                               && el.getMaxOccurs() != 1) {
                        // Have to handle this ourselves.... which really
                        // sucks.... but what can we do?
                        Object objArray;
                        if (mObj instanceof List) {
                            List l = (List)mObj;
                            objArray = l.toArray(new Object[l.size()]);
                            cls = null;
                        } else {
                            objArray = mObj;
                            cls = objArray.getClass().getComponentType();
                        }
                        int len = Array.getLength(objArray);
                        for (int x = 0; x < len; x++) {
                            Object o = Array.get(objArray, x);
                            writeObject(u, source, new JAXBElement(elName, cls == null ? o.getClass() : cls,
                                                                   o));
                        }
                    } else {
                        writeObject(u, source, new JAXBElement(elName, cls, mObj));
                    }
                } else if (byte[].class == cls && part.getTypeQName() != null
                           && part.getTypeQName().getLocalPart().equals("hexBinary")) {
                    mObj = new HexBinaryAdapter().marshal((byte[])mObj);
                    writeObject(u, source, new JAXBElement(elName, String.class, mObj));
                } else {
                    writeObject(u, source, new JAXBElement(elName, cls, mObj));
                }
            } else {
                writeObject(u, source, mObj);
            }
        } catch (Fault ex) {
            throw (Fault)ex.fillInStackTrace();
        } catch (Exception ex) {
            if (ex instanceof javax.xml.bind.MarshalException) {
                javax.xml.bind.MarshalException marshalEx = (javax.xml.bind.MarshalException)ex;
                Message faultMessage = new Message("MARSHAL_ERROR", BUNDLE, marshalEx.getLinkedException()
                    .getMessage());
                throw new Fault(faultMessage, ex);
            } else {
                throw new Fault(new Message("MARSHAL_ERROR", BUNDLE, ex.getMessage()), ex);
            }
        }
    }
    //TODO: cache the JAXBRIContext
    @SuppressWarnings("unchecked")
    public static void marshalWithBridge(TypeReference ref,
                                         Set<Class<?>> ctxClasses,
                                         Object elValue,
                                         Object source, AttachmentMarshaller am) {
        List<TypeReference> typeRefs = new ArrayList<TypeReference>();
        typeRefs.add(ref);
        List<Class<?>> clses = new ArrayList<Class<?>>(ctxClasses);
        clses.add(ref.type.getClass());
        try {
            JAXBRIContext riContext = JAXBRIContext.newInstance(clses.toArray(new Class[clses.size()]),
                                                                    typeRefs, null, null, true, null);
            Bridge bridge = riContext.createBridge(ref);

            if (source instanceof XMLStreamWriter) {
                bridge.marshal(elValue, (XMLStreamWriter)source);
            } else if (source instanceof OutputStream) {
                //the namespace is missing when marshal the xsd:QName type 
                //to the OutputStream directly 
                java.io.StringWriter sw = new java.io.StringWriter();
                StreamResult s1 = new StreamResult(sw);
                bridge.marshal(elValue, s1);
                ((OutputStream)source).write(sw.toString().getBytes());
            } else if (source instanceof Node) {
                bridge.marshal(elValue, (Node)source);
            } else {
                throw new Fault(new Message("UNKNOWN_SOURCE", BUNDLE, source.getClass().getName()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof javax.xml.bind.MarshalException) {
                javax.xml.bind.MarshalException marshalEx = (javax.xml.bind.MarshalException)ex;
                Message faultMessage = new Message("MARSHAL_ERROR", BUNDLE, marshalEx.getLinkedException()
                    .getMessage());
                throw new Fault(faultMessage, ex);
            } else {
                throw new Fault(new Message("MARSHAL_ERROR", BUNDLE, ex.getMessage()), ex);
            }
        }

    }
    
//  TODO: cache the JAXBRIContext
    @SuppressWarnings("unchecked")
    public static Object unmarshalWithBridge(TypeReference ref,
                                             Set<Class<?>> ctxClasses,
                                             Object source,
                                             AttachmentUnmarshaller am) {
        List<TypeReference> typeRefs = new ArrayList<TypeReference>();
        typeRefs.add(ref);
        List<Class<?>> clses = new ArrayList<Class<?>>(ctxClasses);
        clses.add(ref.type.getClass());
        try {
            JAXBRIContext riContext = JAXBRIContext.newInstance(clses.toArray(new Class[clses.size()]),
                                                                    typeRefs, null, null, true, null);
            Bridge bridge = riContext.createBridge(ref);
           
            if (source instanceof XMLStreamReader) {
                //DOMUtils.writeXml(StaxUtils.read((XMLStreamReader)source), System.out);
                return bridge.unmarshal((XMLStreamReader)source);               
            } else if (source instanceof InputStream) {
                return bridge.unmarshal((InputStream)source);
            } else if (source instanceof Node) {
                return bridge.unmarshal((Node)source, am);
            } else {
                throw new Fault(new Message("UNKNOWN_SOURCE", BUNDLE, source.getClass().getName()));
            }
        } catch (Exception ex) {
            if (ex instanceof javax.xml.bind.MarshalException) {
                javax.xml.bind.MarshalException marshalEx = (javax.xml.bind.MarshalException)ex;
                Message faultMessage = new Message("MARSHAL_ERROR", BUNDLE, marshalEx.getLinkedException()
                    .getMessage());
                throw new Fault(faultMessage, ex);
            } else {
                throw new Fault(new Message("MARSHAL_ERROR", BUNDLE, ex.getMessage()), ex);
            }
        }

    }    
    

    @SuppressWarnings("unchecked")
    public static void marshallException(JAXBContext context, Schema schema, Exception elValue,
                                         MessagePartInfo part, Object source, AttachmentMarshaller am,
                                         Map<String, Object> marshallerProperties) {
        XMLStreamWriter writer = getStreamWriter(source);
        QName qn = part.getElementQName();
        try {
            writer.writeStartElement("ns1", qn.getLocalPart(), qn.getNamespaceURI());
            Class<?> cls = part.getTypeClass();
            XmlAccessorType accessorType = cls.getAnnotation(XmlAccessorType.class);
            if (accessorType == null && cls.getPackage() != null) {
                accessorType = cls.getPackage().getAnnotation(XmlAccessorType.class);
            }
            XmlAccessType accessType = accessorType != null
                ? accessorType.value() : XmlAccessType.PUBLIC_MEMBER;
            String namespace = part.getElementQName().getNamespaceURI();
            
            SchemaInfo sch = part.getMessageInfo().getOperation().getInterface()
                .getService().getSchema(namespace);
            if (!sch.isElementFormQualified()) {
                namespace = null;
            }
            
            
            Marshaller u = createMarshaller(context, cls, marshallerProperties);
            try {
                // override anything the user asked us to set.
                // The Marshaller.JAXB_FRAGMENT will tell the Marshaller not to
                // generate the xml declaration.
                u.setProperty(Marshaller.JAXB_FRAGMENT, true);
                u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            } catch (javax.xml.bind.PropertyException e) {
                // intentionally empty.
            }

            for (Field f : cls.getDeclaredFields()) {
                if (JAXBContextInitializer.isFieldAccepted(f, accessType)) {
                    QName fname = new QName(namespace, f.getName());
                    f.setAccessible(true);
                    writeObject(u, writer, new JAXBElement(fname, String.class, f.get(elValue)));
                }
            }
            for (Method m : cls.getMethods()) {
                if (JAXBContextInitializer.isMethodAccepted(m, accessType)) {
                    int idx = m.getName().startsWith("get") ? 3 : 2;
                    String name = m.getName().substring(idx);
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    QName mname = new QName(namespace, name);
                    writeObject(u, writer, new JAXBElement(mname, String.class, m.invoke(elValue)));
                }
            }

            writer.writeEndElement();
            writer.flush();
        } catch (Exception e) {
            throw new Fault(new Message("MARSHAL_ERROR", BUNDLE, e.getMessage()), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Exception unmarshallException(JAXBContext context, Schema schema, Object source,
                                                MessagePartInfo part, AttachmentUnmarshaller au) {
        XMLStreamReader reader;
        if (source instanceof XMLStreamReader) {
            reader = (XMLStreamReader)source;
        } else if (source instanceof Element) {
            reader = StaxUtils.createXMLStreamReader((Element)source);
            try {
                // advance into the node
                reader.nextTag();
            } catch (XMLStreamException e) {
                // ignore
            }
        } else {
            throw new Fault(new Message("UNKNOWN_SOURCE", BUNDLE, source.getClass().getName()));
        }
        try {
            QName qn = part.getElementQName();
            if (!qn.equals(reader.getName())) {
                throw new Fault(new Message("ELEMENT_NAME_MISMATCH", BUNDLE, qn, reader.getName()));
            }

            Class<?> cls = part.getTypeClass();
            Object obj = null;
            try {
                Constructor cons = cls.getConstructor();
                obj = cons.newInstance();
            } catch (NoSuchMethodException nse) {
                Constructor cons = cls.getConstructor(new Class[] {String.class});
                obj = cons.newInstance(new Object[1]);
            }

            XmlAccessorType accessorType = cls.getAnnotation(XmlAccessorType.class);
            if (accessorType == null && cls.getPackage() != null) {
                accessorType = cls.getPackage().getAnnotation(XmlAccessorType.class);
            }
            XmlAccessType accessType = accessorType != null
                ? accessorType.value() : XmlAccessType.PUBLIC_MEMBER;
            Unmarshaller u = createUnmarshaller(context, cls);
            try {
                // The Marshaller.JAXB_FRAGMENT will tell the Marshaller not to
                // generate the xml declaration.
                u.setProperty(Marshaller.JAXB_FRAGMENT, true);
                u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            } catch (javax.xml.bind.PropertyException e) {
                // intentionally empty.
            }
            reader.nextTag();
            while (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                QName q = reader.getName();
                try {
                    Field f = cls.getField(q.getLocalPart());
                    if (JAXBContextInitializer.isFieldAccepted(f, accessType)) {
                        f.setAccessible(true);
                        f.set(obj, u.unmarshal(reader, f.getType()));
                    }
                } catch (NoSuchFieldException ex) {
                    String s = Character.toUpperCase(q.getLocalPart().charAt(0))
                               + q.getLocalPart().substring(1);
                    Method m = null;
                    try {
                        m = cls.getMethod("get" + s);
                    } catch (NoSuchMethodException mex) {
                        m = cls.getMethod("is" + s);
                    }
                    Method m2 = cls.getMethod("set" + s, m.getReturnType());
                    Object o = getElementValue(u.unmarshal(reader, m.getReturnType()));
                    m2.invoke(obj, o);
                }
            }
            return (Exception)obj;
        } catch (Exception e) {
            throw new Fault(new Message("MARSHAL_ERROR", BUNDLE, e.getMessage()), e);
        }
    }

    private static void writeObject(Marshaller u, Object source, Object mObj) throws Fault, JAXBException {
        if (source instanceof XMLStreamWriter) {
            u.marshal(mObj, (XMLStreamWriter)source);
        } else if (source instanceof OutputStream) {
            u.marshal(mObj, (OutputStream)source);
        } else if (source instanceof Node) {
            u.marshal(mObj, (Node)source);
        } else if (source instanceof XMLEventWriter) {
            u.marshal(mObj, (XMLEventWriter)source);
        } else {
            throw new Fault(new Message("UNKNOWN_SOURCE", BUNDLE, source.getClass().getName()));
        }
    }

    private static XMLStreamWriter getStreamWriter(Object source) throws Fault {
        if (source instanceof XMLStreamWriter) {
            return (XMLStreamWriter)source;
        } else if (source instanceof OutputStream) {
            return StaxUtils.createXMLStreamWriter((OutputStream)source);
        } else if (source instanceof Node) {
            return new W3CDOMStreamWriter((Element)source);
        }
        throw new Fault(new Message("UNKNOWN_SOURCE", BUNDLE, source.getClass().getName()));
    }

    public static void marshall(JAXBContext context, Schema schema, Object elValue, Object source,
                                Map<String, Object> marshallerProperties) {
        marshall(context, schema, elValue, null, source, null, marshallerProperties);
    }

    @SuppressWarnings("unchecked")
    public static void marshallNullElement(JAXBContext context, Schema schema, Object source,
                                           MessagePartInfo part, Map<String, Object> marshallerProperties) {
        Class<?> clazz = part != null ? (Class)part.getTypeClass() : null;
        try {
            Marshaller u = createMarshaller(context, clazz, marshallerProperties);
            u.setSchema(schema);
            try {
                // The Marshaller.JAXB_FRAGMENT will tell the Marshaller not to
                // generate the xml declaration.
                u.setProperty(Marshaller.JAXB_FRAGMENT, true);
                u.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            } catch (javax.xml.bind.PropertyException e) {
                // intentionally empty.
            }
            writeObject(u, source, new JAXBElement(part.getElementQName(), clazz, null));
        } catch (JAXBException e) {
            throw new Fault(new Message("MARSHAL_ERROR", BUNDLE, e.getMessage()), e);
        }
    }

    public static void marshall(JAXBContext context, Schema schema, Object elValue, MessagePartInfo part,
                                Object source, Map<String, Object> marshallerProperties) {
        marshall(context, schema, elValue, part, source, null, marshallerProperties);
    }

    private static Unmarshaller createUnmarshaller(JAXBContext context, Class<?> cls) throws JAXBException {
        Unmarshaller um = null;
        if (context == null) {
            if (cls == null) {
                throw new IllegalStateException("A JAXBContext or Class to unmarshal must be provided!");
            }
            context = JAXBContext.newInstance(cls);
        }

        um = context.createUnmarshaller();

        return um;
    }

    public static Object unmarshall(JAXBContext context, Schema schema, Object source) {
        return unmarshall(context, schema, source, null, null, true);
    }

    @SuppressWarnings("unchecked")
    public static Object unmarshall(JAXBContext context, Schema schema, Object source, MessagePartInfo part,
                                    AttachmentUnmarshaller au, boolean unwrap) {
        Class<?> clazz = part != null ? (Class)part.getTypeClass() : null;
        if (clazz != null && Exception.class.isAssignableFrom(clazz) && part != null
            && Boolean.TRUE.equals(part.getProperty(JAXBDataBinding.class.getName() + ".CUSTOM_EXCEPTION"))) {
            return unmarshallException(context, schema, source, part, au);
        }

        QName elName = part != null ? part.getConcreteName() : null;
        if (clazz != null && clazz.isArray() && part != null
            && part.getXmlSchema() instanceof XmlSchemaElement) {
            XmlSchemaElement el = (XmlSchemaElement)part.getXmlSchema();

            if (el.getSchemaType() instanceof XmlSchemaSimpleType
                && ((XmlSchemaSimpleType)el.getSchemaType()).getContent() 
                instanceof XmlSchemaSimpleTypeList) {

                Object obj = unmarshall(context, schema, source, elName, null, au, unwrap);
                if (clazz.isArray() && obj instanceof List) {
                    return ((List)obj).toArray((Object[])Array.newInstance(clazz.getComponentType(),
                                                                           ((List)obj).size()));
                }

                return obj;
            } else if (part.getMessageInfo().getOperation().isUnwrapped() && el.getMaxOccurs() != 1) {
                // must read ourselves....
                List<Object> ret = unmarshallArray(context, schema, source, elName, clazz.getComponentType(),
                                                   au, createList(part));
                Object o = ret;
                if (!isList(part)) {
                    if (clazz.getComponentType().isPrimitive()) {
                        o = java.lang.reflect.Array.newInstance(clazz.getComponentType(), ret.size());
                        for (int x = 0; x < ret.size(); x++) {
                            Array.set(o, x, ret.get(x));
                        }
                    } else {
                        o = ret.toArray((Object[])Array.newInstance(clazz.getComponentType(), ret.size()));
                    }
                }
                return o;
            }
        } else if (byte[].class == clazz && part != null && part.getTypeQName() != null
                   && part.getTypeQName().getLocalPart().equals("hexBinary")) {

            String obj = (String)unmarshall(context, schema, source, elName, String.class, au, unwrap);
            return new HexBinaryAdapter().unmarshal(obj);
        }

        Object o = unmarshall(context, schema, source, elName, clazz, au, unwrap);
        if (o != null && o.getClass().isArray() && isList(part)) {
            List<Object> ret = createList(part);
            ret.addAll(Arrays.asList((Object[])o));
            o = ret;
        }
        return o;
    }

    private static List<Object> createList(MessagePartInfo part) {
        Type genericType = (Type)part.getProperty("generic.type");
        if (genericType instanceof ParameterizedType) {
            Type tp2 = ((ParameterizedType)genericType).getRawType();
            if (tp2 instanceof Class) {
                Class<?> cls = (Class)tp2;
                if (!cls.isInterface() && List.class.isAssignableFrom((Class<?>)cls)) {
                    try {
                        return CastUtils.cast((List)cls.newInstance());
                    } catch (Exception e) {
                        // ignore, just return an ArrayList
                    }
                }
            }
        }

        return new ArrayList<Object>();
    }

    private static boolean isList(MessagePartInfo part) {
        if (part.getTypeClass().isArray() && !part.getTypeClass().getComponentType().isPrimitive()) {
            // && Collection.class.isAssignableFrom(part.getTypeClass())) {
            // it's List Para
            //
            Type genericType = (Type)part.getProperty("generic.type");

            if (genericType instanceof ParameterizedType) {
                Type tp2 = ((ParameterizedType)genericType).getRawType();
                if (tp2 instanceof Class) {
                    return Collection.class.isAssignableFrom((Class<?>)tp2);
                }
            }
        }
        return false;
    }

    public static Object unmarshall(JAXBContext context, Schema schema, Object source, QName elName,
                                    Class<?> clazz, AttachmentUnmarshaller au, boolean unwrap) {
        Object obj = null;

        try {
            Unmarshaller u = createUnmarshaller(context, clazz);
            u.setSchema(schema);
            if (au != null) {
                u.setAttachmentUnmarshaller(au);
            }
            boolean unmarshalWithClass = true;

            if (clazz == null
                || (!clazz.isPrimitive() && !clazz.isArray() && !clazz.isEnum() && (Modifier.isAbstract(clazz
                    .getModifiers()) || Modifier.isInterface(clazz.getModifiers())))) {
                unmarshalWithClass = false;
            }

            if (clazz != null
                && (clazz.getName().equals("javax.xml.datatype.XMLGregorianCalendar") || clazz.getName()
                    .equals("javax.xml.datatype.Duration"))) {
                // special treat two jaxb defined built-in abstract types
                unmarshalWithClass = true;
            }
            if (source instanceof Node) {
                obj = unmarshalWithClass ? u.unmarshal((Node)source, clazz) : u.unmarshal((Node)source);
            } else if (source instanceof XMLStreamReader) {

                obj = unmarshalWithClass ? u.unmarshal((XMLStreamReader)source, clazz) : u
                    .unmarshal((XMLStreamReader)source);
            } else if (source instanceof XMLEventReader) {
                obj = unmarshalWithClass ? u.unmarshal((XMLEventReader)source, clazz) : u
                    .unmarshal((XMLEventReader)source);
            } else {
                throw new Fault(new Message("UNKNOWN_SOURCE", BUNDLE, source.getClass().getName()));
            }
        } catch (Fault ex) {
            ex.fillInStackTrace();
            throw ex;
        } catch (Throwable ex) {
            if (ex instanceof javax.xml.bind.UnmarshalException) {
                javax.xml.bind.UnmarshalException unmarshalEx = (javax.xml.bind.UnmarshalException)ex;
                throw new Fault(new Message("UNMARSHAL_ERROR", BUNDLE, unmarshalEx.getLinkedException()
                    .getMessage()), ex);
            } else {
                throw new Fault(new Message("UNMARSHAL_ERROR", BUNDLE, ex.getMessage()), ex);
            }
        }
        return unwrap ? getElementValue(obj) : obj;
    }

    public static Object getElementValue(Object obj) {
        if (null == obj) {
            return null;
        }

        if (obj instanceof JAXBElement) {
            return ((JAXBElement<?>)obj).getValue();
        }
        return obj;
    }

    public static Class<?> getClassFromType(Type t) {
        if (t instanceof Class) {
            return (Class)t;
        } else if (t instanceof GenericArrayType) {
            GenericArrayType g = (GenericArrayType)t;
            return Array.newInstance(getClassFromType(g.getGenericComponentType()), 0).getClass();
        } else if (t instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType)t;
            return getClassFromType(p.getRawType());
        }
        // TypeVariable and WildCardType are not handled as it is unlikely such
        // Types will
        // JAXB Code Generated.
        assert false;
        throw new IllegalArgumentException("Cannot get Class object from unknown Type");
    }

    public static List<Object> unmarshallArray(JAXBContext context, Schema schema, Object source,
                                               QName elName, Class<?> clazz, AttachmentUnmarshaller au,
                                               List<Object> ret) {
        try {
            Unmarshaller u = createUnmarshaller(context, clazz);
            u.setSchema(schema);
            if (au != null) {
                u.setAttachmentUnmarshaller(au);
            }
            XMLStreamReader reader;
            if (source instanceof XMLStreamReader) {
                reader = (XMLStreamReader)source;
            } else if (source instanceof Element) {
                reader = StaxUtils.createXMLStreamReader((Element)source);
            } else {
                throw new Fault(new Message("UNKNOWN_SOURCE", BUNDLE, source.getClass().getName()));
            }
            while (reader.getName().equals(elName)) {
                Object obj = u.unmarshal(reader, clazz);
                if (obj instanceof JAXBElement) {
                    obj = ((JAXBElement)obj).getValue();
                }
                ret.add(obj);
                while (reader.getEventType() != XMLStreamConstants.START_ELEMENT 
                    && reader.getEventType() != XMLStreamConstants.END_ELEMENT) {
                    reader.nextTag();
                }
            }
            return ret;
        } catch (Fault ex) {
            ex.fillInStackTrace();
            throw ex;
        } catch (Exception ex) {
            if (ex instanceof javax.xml.bind.UnmarshalException) {
                javax.xml.bind.UnmarshalException unmarshalEx = (javax.xml.bind.UnmarshalException)ex;
                throw new Fault(new Message("UNMARSHAL_ERROR", BUNDLE, unmarshalEx.getLinkedException()
                    .getMessage()), ex);
            } else {
                throw new Fault(new Message("UNMARSHAL_ERROR", BUNDLE, ex.getMessage()), ex);
            }
        }
    }
}
