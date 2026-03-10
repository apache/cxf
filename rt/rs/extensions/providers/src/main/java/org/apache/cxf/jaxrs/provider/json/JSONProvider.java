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

package org.apache.cxf.jaxrs.provider.json;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.cxf.jaxrs.provider.AbstractJAXBProvider;
import org.apache.cxf.jaxrs.provider.json.utils.JSONUtils;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXBUtils;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.codehaus.jettison.JSONSequenceTooLargeException;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.SimpleConverter;
import org.codehaus.jettison.mapped.TypeConverter;
import org.codehaus.jettison.util.StringIndenter;

@Produces({"application/json", "application/*+json", "text/json" /* deprecated */ })
@Consumes({"application/json", "application/*+json", "text/json"/* deprecated */ })
@Provider
public class JSONProvider<T> extends AbstractJAXBProvider<T>  {

    private static final String MAPPED_CONVENTION = "mapped";
    private static final String BADGER_FISH_CONVENTION = "badgerfish";
    private static final String DROP_ROOT_CONTEXT_PROPERTY = "drop.json.root.element";
    private static final String ARRAY_KEYS_PROPERTY = "json.array.keys";
    private static final String ROOT_IS_ARRAY_PROPERTY = "json.root.is.array";
    private static final String DROP_ELEMENT_IN_XML_PROPERTY = "drop.xml.elements";
    private static final String IGNORE_EMPTY_JSON_ARRAY_VALUES_PROPERTY = "ignore.empty.json.array.values";
    static {
        new SimpleConverter();
    }

    private ConcurrentHashMap<String, String> namespaceMap =
        new ConcurrentHashMap<>();
    private boolean serializeAsArray;
    private List<String> arrayKeys;
    private List<String> primitiveArrayKeys;
    private boolean unwrapped;
    private String wrapperName;
    private String namespaceSeparator;
    private Map<String, String> wrapperMap;
    private boolean dropRootElement;
    private boolean dropElementsInXmlStream = true;
    private boolean dropCollectionWrapperElement;
    private boolean ignoreMixedContent;
    private boolean ignoreEmptyArrayValues;
    private boolean writeXsiType = true;
    private boolean readXsiType = true;
    private boolean ignoreNamespaces;
    private String convention = MAPPED_CONVENTION;
    private TypeConverter typeConverter;
    private boolean attributesToElements;
    private boolean writeNullAsString = true;
    private boolean escapeForwardSlashesAlways;

    @Override
    public void setAttributesToElements(boolean value) {
        this.attributesToElements = value;
    }

    public void setConvention(String value) {
        if (!MAPPED_CONVENTION.equals(value) && !BADGER_FISH_CONVENTION.equals(value)) {
            throw new IllegalArgumentException("Unsupported convention \"" + value);
        }
        convention = value;
    }

    public void setConvertTypesToStrings(boolean convert) {
        if (convert) {
            this.setTypeConverter(new SimpleConverter());
        }
    }

    public void setTypeConverter(TypeConverter converter) {
        this.typeConverter = converter;
    }

    public void setIgnoreNamespaces(boolean ignoreNamespaces) {
        this.ignoreNamespaces = ignoreNamespaces;
    }

    @Context
    public void setMessageContext(MessageContext mc) {
        super.setContext(mc);
    }

    public void setDropRootElement(boolean drop) {
        this.dropRootElement = drop;
    }

    public void setDropCollectionWrapperElement(boolean drop) {
        this.dropCollectionWrapperElement = drop;
    }

    public void setIgnoreMixedContent(boolean ignore) {
        this.ignoreMixedContent = ignore;
    }

    public void setSupportUnwrapped(boolean unwrap) {
        this.unwrapped = unwrap;
    }

    public void setWrapperName(String wName) {
        wrapperName = wName;
    }

    public void setWrapperMap(Map<String, String> map) {
        wrapperMap = map;
    }

    public void setSerializeAsArray(boolean asArray) {
        this.serializeAsArray = asArray;
    }

    public void setArrayKeys(List<String> keys) {
        this.arrayKeys = keys;
    }

    public void setNamespaceMap(Map<String, String> namespaceMap) {
        this.namespaceMap.putAll(namespaceMap);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        return super.isReadable(type, genericType, anns, mt) || Document.class.isAssignableFrom(type);
    }

    public T readFrom(Class<T> type, Type genericType, Annotation[] anns, MediaType mt,
        MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {

        if (isPayloadEmpty(headers)) {
            if (AnnotationUtils.getAnnotation(anns, Nullable.class) != null) {
                return null;
            }
            reportEmptyContentLength();
        }

        XMLStreamReader reader = null;
        String enc = HttpUtils.getEncoding(mt, StandardCharsets.UTF_8.name());
        Unmarshaller unmarshaller = null;
        try {
            InputStream realStream = getInputStream(type, genericType, is);
            if (Document.class.isAssignableFrom(type)) {
                W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
                reader = createReader(type, realStream, false, enc);
                copyReaderToWriter(reader, writer);
                return type.cast(writer.getDocument());
            }
            boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(type);
            Class<?> theGenericType = isCollection ? InjectionUtils.getActualType(genericType) : type;
            Class<?> theType = getActualType(theGenericType, genericType, anns);

            unmarshaller = createUnmarshaller(theType, genericType, isCollection);
            XMLStreamReader xsr = createReader(type, realStream, isCollection, enc);

            Object response;
            if (JAXBElement.class.isAssignableFrom(type)
                || !isCollection && (unmarshalAsJaxbElement
                || jaxbElementClassMap != null && jaxbElementClassMap.containsKey(theType.getName()))) {
                response = unmarshaller.unmarshal(xsr, theType);
            } else {
                response = unmarshaller.unmarshal(xsr);
            }
            if (response instanceof JAXBElement && !JAXBElement.class.isAssignableFrom(type)) {
                response = ((JAXBElement<?>)response).getValue();
            }
            if (isCollection) {
                response = ((CollectionWrapper)response).getCollectionOrArray(
                               unmarshaller, theType, type, genericType,
                               org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(theGenericType, anns));
            } else {
                response = checkAdapter(response, type, anns, false);
            }
            return type.cast(response);

        } catch (JAXBException e) {
            handleJAXBException(e, true);
        } catch (XMLStreamException e) {
            if (e.getCause() instanceof JSONSequenceTooLargeException) {
                throw new WebApplicationException(413);
            }
            handleXMLStreamException(e, true);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtils.toBadRequestException(e, null);
        } finally {
            try {
                StaxUtils.close(reader);
            } catch (XMLStreamException e) {
                throw ExceptionUtils.toBadRequestException(e, null);
            }
            JAXBUtils.closeUnmarshaller(unmarshaller);
        }
        // unreachable
        return null;
    }

    protected XMLStreamReader createReader(Class<?> type, InputStream is, boolean isCollection, String enc)
        throws Exception {
        XMLStreamReader reader = createReader(type, is, enc);
        return isCollection ? new JAXBCollectionWrapperReader(reader) : reader;
    }

    protected XMLStreamReader createReader(Class<?> type, InputStream is, String enc)
        throws Exception {
        final XMLStreamReader reader;
        if (BADGER_FISH_CONVENTION.equals(convention)) {
            reader = JSONUtils.createBadgerFishReader(is, enc);
        } else {
            reader = JSONUtils.createStreamReader(is,
                                                  readXsiType,
                                                  namespaceMap,
                                                  namespaceSeparator,
                                                  primitiveArrayKeys,
                                                  getDepthProperties(),
                                                  enc);
        }
        return createTransformReaderIfNeeded(reader, is);
    }

    protected InputStream getInputStream(Class<T> cls, Type type, InputStream is) throws Exception {
        if (unwrapped) {
            String rootName = getRootName(cls, type);
            InputStream isBefore = new ByteArrayInputStream(rootName.getBytes());
            String after = "}";
            InputStream isAfter = new ByteArrayInputStream(after.getBytes());
            final InputStream[] streams = new InputStream[]{isBefore, is, isAfter};

            Enumeration<InputStream> list = new Enumeration<InputStream>() {
                private int index;
                public boolean hasMoreElements() {
                    return index < streams.length;
                }

                public InputStream nextElement() {
                    return streams[index++];
                }

            };
            return new SequenceInputStream(list);
        }
        return is;

    }

    protected String getRootName(Class<T> cls, Type type) throws Exception {
        String name = null;
        if (wrapperName != null) {
            name = wrapperName;
        } else if (wrapperMap != null) {
            name = wrapperMap.get(cls.getName());
        }
        if (name == null) {
            QName qname = getQName(cls, type, null);
            if (qname != null) {
                name = qname.getLocalPart();
                String prefix = qname.getPrefix();
                if (prefix.length() > 0) {
                    name = prefix + "." + name;
                }
            }
        }

        if (name == null) {
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }

        return "{\"" + name + "\":";
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {

        return super.isWriteable(type, genericType, anns, mt)
            || Document.class.isAssignableFrom(type);
    }

    public void writeTo(T obj, Class<?> cls, Type genericType, Annotation[] anns,
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        if (os == null) {
            StringBuilder sb = new StringBuilder(256);
            sb.append("Jettison needs initialized OutputStream");
            if (getContext() != null && getContext().getContent(XMLStreamWriter.class) == null) {
                sb.append("; if you need to customize Jettison output with the custom XMLStreamWriter"
                          + " then extend JSONProvider or when possible configure it directly.");
            }
            throw new IOException(sb.toString());
        }
        XMLStreamWriter writer = null;
        try {

            String enc = HttpUtils.getSetEncoding(m, headers, StandardCharsets.UTF_8.name());
            if (Document.class.isAssignableFrom(cls)) {
                writer = createWriter(obj, cls, genericType, enc, os, false);
                copyReaderToWriter(StaxUtils.createXMLStreamReader((Document)obj), writer);
                return;
            }
            if (InjectionUtils.isSupportedCollectionOrArray(cls)) {
                marshalCollection(cls, obj, genericType, enc, os, m, anns);
            } else {
                Object actualObject = checkAdapter(obj, cls, anns, true);
                Class<?> actualClass = obj != actualObject || cls.isInterface()
                    ? actualObject.getClass() : cls;
                if (cls == genericType) {
                    genericType = actualClass;
                }

                marshal(actualObject, actualClass, genericType, enc, os);
            }

        } catch (JAXBException e) {
            handleJAXBException(e, false);
        } catch (XMLStreamException e) {
            handleXMLStreamException(e, false);
        } catch (Exception e) {
            throw ExceptionUtils.toInternalServerErrorException(e, null);
        } finally {
            StaxUtils.close(writer);
        }
    }

    protected void copyReaderToWriter(XMLStreamReader reader, XMLStreamWriter writer)
        throws Exception {
        writer.writeStartDocument();
        StaxUtils.copy(reader, writer);
        writer.writeEndDocument();
    }

    protected void marshalCollection(Class<?> originalCls, Object collection,
                                     Type genericType, String encoding,
                                     OutputStream os, MediaType m, Annotation[] anns)
        throws Exception {

        Class<?> actualClass = InjectionUtils.getActualType(genericType);
        actualClass = getActualType(actualClass, genericType, anns);

        Collection<?> c = originalCls.isArray() ? Arrays.asList((Object[]) collection)
                                             : (Collection<?>) collection;

        Iterator<?> it = c.iterator();

        Object firstObj = it.hasNext() ? it.next() : null;

        final String startTag;
        final String endTag;
        if (!dropCollectionWrapperElement) {
            final QName qname;
            if (firstObj instanceof JAXBElement) {
                JAXBElement<?> el = (JAXBElement<?>)firstObj;
                qname = el.getName();
                actualClass = el.getDeclaredType();
            } else {
                qname = getCollectionWrapperQName(actualClass, genericType, firstObj, false);
            }
            String prefix = "";
            if (!ignoreNamespaces) {
                prefix = namespaceMap.get(qname.getNamespaceURI());
                if (prefix != null) {
                    if (!prefix.isEmpty()) {
                        prefix += ".";
                    }
                } else if (!qname.getNamespaceURI().isEmpty()) {
                    prefix = "ns1.";
                }
            }
            prefix = (prefix == null) ? "" : prefix;
            startTag = "{\"" + prefix + qname.getLocalPart() + "\":[";
            endTag = "]}";
        } else if (serializeAsArray) {
            startTag = "[";
            endTag = "]";
        } else {
            startTag = "{";
            endTag = "}";
        }

        os.write(startTag.getBytes());
        if (firstObj != null) {
            XmlJavaTypeAdapter adapter =
                org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(firstObj.getClass(), anns);
            marshalCollectionMember(JAXBUtils.useAdapter(firstObj, adapter, true),
                                    actualClass, genericType, encoding, os);
            while (it.hasNext()) {
                os.write(',');
                marshalCollectionMember(JAXBUtils.useAdapter(it.next(), adapter, true),
                                        actualClass, genericType, encoding, os);
            }
        }
        os.write(endTag.getBytes());
    }

    protected void marshalCollectionMember(Object obj, Class<?> cls, Type genericType,
                                           String enc, OutputStream os) throws Exception {
        if (obj instanceof JAXBElement) {
            obj = ((JAXBElement<?>)obj).getValue();
        } else {
            obj = convertToJaxbElementIfNeeded(obj, cls, genericType);
        }

        if (obj instanceof JAXBElement && cls != JAXBElement.class) {
            cls = JAXBElement.class;
        }
        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        marshal(ms, obj, cls, genericType, enc, os, true);

    }

    protected void marshal(Marshaller ms, Object actualObject, Class<?> actualClass,
                  Type genericType, String enc, OutputStream os, boolean isCollection) throws Exception {
        OutputStream actualOs = os;

        MessageContext mc = getContext();
        if (mc != null && PropertyUtils.isTrue(mc.get(Marshaller.JAXB_FORMATTED_OUTPUT))) {
            actualOs = new CachedOutputStream();
        }
        XMLStreamWriter writer = createWriter(actualObject, actualClass, genericType, enc,
                                              actualOs, isCollection);
        if (namespaceMap.size() > 1 || namespaceMap.size() == 1 && !namespaceMap.containsKey(JSONUtils.XSI_URI)) {
            setNamespaceMapper(ms, namespaceMap);
        }
        org.apache.cxf.common.jaxb.JAXBUtils.setNoEscapeHandler(ms);
        ms.marshal(actualObject, writer);
        writer.close();
        if (os != actualOs) {
            StringIndenter formatter = new StringIndenter(
                IOUtils.newStringFromBytes(((CachedOutputStream)actualOs).getBytes()));
            Writer outWriter = new OutputStreamWriter(os, enc);
            IOUtils.copy(new StringReader(formatter.result()), outWriter, 2048);
            outWriter.close();
        }
    }

    protected XMLStreamWriter createWriter(Object actualObject, Class<?> actualClass,
        Type genericType, String enc, OutputStream os, boolean isCollection) throws Exception {

        if (BADGER_FISH_CONVENTION.equals(convention)) {
            return JSONUtils.createBadgerFishWriter(os, enc);
        }
        boolean dropElementsInXmlStreamProp = getBooleanJsonProperty(DROP_ELEMENT_IN_XML_PROPERTY,
                                                                     dropElementsInXmlStream);

        boolean dropRootNeeded = getBooleanJsonProperty(DROP_ROOT_CONTEXT_PROPERTY, dropRootElement);
        boolean dropRootInXmlNeeded = dropRootNeeded && dropElementsInXmlStreamProp;

        QName qname = actualClass == Document.class
            ? org.apache.cxf.helpers.DOMUtils.getElementQName(((Document)actualObject).getDocumentElement())
            : getQName(actualClass, genericType, actualObject);
        if (qname != null && ignoreNamespaces && (isCollection || dropRootInXmlNeeded)) {
            qname = new QName(qname.getLocalPart());
        }

        Configuration config =
            JSONUtils.createConfiguration(namespaceMap,
                                          writeXsiType && !ignoreNamespaces,
                                          attributesToElements,
                                          typeConverter);
        if (namespaceSeparator != null) {
            config.setJsonNamespaceSeparator(namespaceSeparator);
        }
        if (!dropElementsInXmlStreamProp && super.outDropElements != null) {
            config.setIgnoredElements(outDropElements);
        }
        if (!writeNullAsString) {
            config.setWriteNullAsString(writeNullAsString);
        }
        boolean ignoreEmpty = getBooleanJsonProperty(IGNORE_EMPTY_JSON_ARRAY_VALUES_PROPERTY, ignoreEmptyArrayValues);
        if (ignoreEmpty) {
            config.setIgnoreEmptyArrayValues(ignoreEmpty);
        }

        if (escapeForwardSlashesAlways) {
            config.setEscapeForwardSlashAlways(escapeForwardSlashesAlways);
        }


        boolean dropRootInJsonStream = dropRootNeeded && !dropElementsInXmlStreamProp;
        if (dropRootInJsonStream) {
            config.setDropRootElement(true);
        }

        List<String> theArrayKeys = getArrayKeys();
        boolean rootIsArray = isRootArray(theArrayKeys);

        if (ignoreNamespaces && rootIsArray && (theArrayKeys == null || dropRootInJsonStream)) {
            if (theArrayKeys == null) {
                theArrayKeys = new LinkedList<>();
            } else if (dropRootInJsonStream) {
                theArrayKeys = new LinkedList<>(theArrayKeys);
            }
            if (qname != null) {
                theArrayKeys.add(qname.getLocalPart());
            }
        }

        XMLStreamWriter writer = JSONUtils.createStreamWriter(os, qname,
             writeXsiType && !ignoreNamespaces, config, rootIsArray, theArrayKeys,
             isCollection || dropRootInXmlNeeded, enc);
        writer = JSONUtils.createIgnoreMixedContentWriterIfNeeded(writer, ignoreMixedContent);
        writer = JSONUtils.createIgnoreNsWriterIfNeeded(writer, ignoreNamespaces, !writeXsiType);
        return createTransformWriterIfNeeded(writer, os, dropElementsInXmlStreamProp);
    }

    protected List<String> getArrayKeys() {
        MessageContext mc = getContext();
        if (mc != null) {
            Object prop = mc.get(ARRAY_KEYS_PROPERTY);
            if (prop instanceof List) {
                return CastUtils.cast((List<?>)prop);
            }
        }
        return arrayKeys;
    }

    protected boolean isRootArray(List<String> theArrayKeys) {
        return theArrayKeys != null || getBooleanJsonProperty(ROOT_IS_ARRAY_PROPERTY, serializeAsArray);
    }


    protected boolean getBooleanJsonProperty(String name, boolean defaultValue) {
        MessageContext mc = getContext();
        if (mc != null) {
            Object prop = mc.get(name);
            if (prop != null) {
                return PropertyUtils.isTrue(prop);
            }
        }
        return defaultValue;
    }

    protected void marshal(Object actualObject, Class<?> actualClass,
                           Type genericType, String enc, OutputStream os) throws Exception {

        actualObject = convertToJaxbElementIfNeeded(actualObject, actualClass, genericType);
        if (actualObject instanceof JAXBElement && actualClass != JAXBElement.class) {
            actualClass = JAXBElement.class;
        }

        Marshaller ms = createMarshaller(actualObject, actualClass, genericType, enc);
        marshal(ms, actualObject, actualClass, genericType, enc, os, false);
    }

    private QName getQName(Class<?> cls, Type type, Object object)
        throws Exception {
        QName qname = getJaxbQName(cls, type, object, false);
        if (qname != null) {
            String prefix = getPrefix(qname.getNamespaceURI());
            return new QName(qname.getNamespaceURI(), qname.getLocalPart(), prefix);
        }
        return null;
    }

    private String getPrefix(String namespace) {
        String prefix = namespaceMap.get(namespace);
        return prefix == null ? "" : prefix;
    }

    public void setWriteXsiType(boolean writeXsiType) {
        this.writeXsiType = writeXsiType;
    }

    public void setReadXsiType(boolean readXsiType) {
        this.readXsiType = readXsiType;
    }

    public void setPrimitiveArrayKeys(List<String> primitiveArrayKeys) {
        this.primitiveArrayKeys = primitiveArrayKeys;
    }

    public void setDropElementsInXmlStream(boolean drop) {
        this.dropElementsInXmlStream = drop;
    }

    public void setWriteNullAsString(boolean writeNullAsString) {
        this.writeNullAsString = writeNullAsString;
    }

    public void setIgnoreEmptyArrayValues(boolean ignoreEmptyArrayElements) {
        this.ignoreEmptyArrayValues = ignoreEmptyArrayElements;
    }

    protected DocumentDepthProperties getDepthProperties() {
        DocumentDepthProperties depthProperties = super.getDepthProperties();
        if (depthProperties != null) {
            return depthProperties;
        }
        if (getContext() != null) {
            String totalElementCountStr = (String)getContext().getContextualProperty(
                DocumentDepthProperties.TOTAL_ELEMENT_COUNT);
            String innerElementCountStr = (String)getContext().getContextualProperty(
                DocumentDepthProperties.INNER_ELEMENT_COUNT);
            String elementLevelStr = (String)getContext().getContextualProperty(
                DocumentDepthProperties.INNER_ELEMENT_LEVEL);
            if (totalElementCountStr != null || innerElementCountStr != null || elementLevelStr != null) {
                try {
                    int totalElementCount = totalElementCountStr != null
                        ? Integer.parseInt(totalElementCountStr) : -1;
                    int elementLevel = elementLevelStr != null ? Integer.parseInt(elementLevelStr) : -1;
                    int innerElementCount = innerElementCountStr != null
                        ? Integer.parseInt(innerElementCountStr) : -1;
                    return new DocumentDepthProperties(totalElementCount, elementLevel, innerElementCount);
                } catch (Exception ex) {
                    throw ExceptionUtils.toInternalServerErrorException(ex, null);
                }
            }
        }
        return null;
    }

    public void setEscapeForwardSlashesAlways(boolean escape) {
        this.escapeForwardSlashesAlways = escape;
    }

    public void setNamespaceSeparator(String namespaceSeparator) {
        this.namespaceSeparator = namespaceSeparator;
    }
}
