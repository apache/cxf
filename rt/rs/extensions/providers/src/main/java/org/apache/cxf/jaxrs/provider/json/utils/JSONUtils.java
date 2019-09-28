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
package org.apache.cxf.jaxrs.provider.json.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.apache.cxf.staxutils.transform.IgnoreNamespacesWriter;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.badgerfish.BadgerFishXMLInputFactory;
import org.codehaus.jettison.badgerfish.BadgerFishXMLOutputFactory;
import org.codehaus.jettison.json.JSONTokener;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.codehaus.jettison.mapped.TypeConverter;

public final class JSONUtils {

    public static final String XSI_PREFIX = "xsi";
    public static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private JSONUtils() {
    }

    public static XMLStreamWriter createBadgerFishWriter(OutputStream os, String enc) throws XMLStreamException {
        XMLOutputFactory factory = new BadgerFishXMLOutputFactory();
        return factory.createXMLStreamWriter(os, enc);
    }

    public static XMLStreamReader createBadgerFishReader(InputStream is, String enc) throws XMLStreamException {
        XMLInputFactory factory = new BadgerFishXMLInputFactory();
        return factory.createXMLStreamReader(is, enc);
    }
    //CHECKSTYLE:OFF
    public static XMLStreamWriter createStreamWriter(OutputStream os,
                                                     QName qname,
                                                     boolean writeXsiType,
                                                     Configuration config,
                                                     boolean serializeAsArray,
                                                     List<String> arrayKeys,
                                                     boolean dropRootElement,
                                                     String enc) throws Exception {
    //CHECKSTYLE:ON
        MappedNamespaceConvention convention = new MappedNamespaceConvention(config);
        AbstractXMLStreamWriter xsw = new MappedXMLStreamWriter(
                                            convention,
                                            new OutputStreamWriter(os, enc));
        if (serializeAsArray) {
            if (arrayKeys != null) {
                for (String key : arrayKeys) {
                    xsw.serializeAsArray(key);
                }
            } else if (qname != null) {
                String key = getKey(convention, qname);
                xsw.serializeAsArray(key);
            }
        }
        return !writeXsiType || dropRootElement
            ? new IgnoreContentJettisonWriter(xsw, writeXsiType, dropRootElement) : xsw;

    }

    public static Configuration createConfiguration(ConcurrentHashMap<String, String> namespaceMap,
                                                    boolean writeXsiType,
                                                    boolean attributesAsElements,
                                                    TypeConverter converter) {
        if (writeXsiType) {
            namespaceMap.putIfAbsent(XSI_URI, XSI_PREFIX);
        }
        Configuration c = new Configuration(namespaceMap);
        c.setSupressAtAttributes(attributesAsElements);
        if (converter != null) {
            c.setTypeConverter(converter);
        }
        return c;
    }

    public static XMLStreamWriter createIgnoreMixedContentWriterIfNeeded(XMLStreamWriter writer,
                                                                         boolean ignoreMixedContent) {
        return ignoreMixedContent ? new IgnoreMixedContentWriter(writer) : writer;
    }

    public static XMLStreamWriter createIgnoreNsWriterIfNeeded(XMLStreamWriter writer,
                                                               boolean ignoreNamespaces,
                                                               boolean ignoreXsiAttributes) {
        return ignoreNamespaces ? new IgnoreNamespacesWriter(writer, ignoreXsiAttributes) : writer;
    }

    private static String getKey(MappedNamespaceConvention convention, QName qname) throws Exception {
        return convention.createKey(qname.getPrefix(),
                                    qname.getNamespaceURI(),
                                    qname.getLocalPart());


    }

    public static XMLStreamReader createStreamReader(InputStream is, boolean readXsiType,
        ConcurrentHashMap<String, String> namespaceMap) throws Exception {
        return createStreamReader(is, readXsiType, namespaceMap, null, null, null, StandardCharsets.UTF_8.name());
    }

    public static XMLStreamReader createStreamReader(InputStream is, boolean readXsiType,
        ConcurrentHashMap<String, String> namespaceMap,
        String namespaceSeparator,
        List<String> primitiveArrayKeys,
        DocumentDepthProperties depthProps,
        String enc) throws Exception {
        if (readXsiType) {
            namespaceMap.putIfAbsent(XSI_URI, XSI_PREFIX);
        }
        Configuration conf = new Configuration(namespaceMap);
        if (namespaceSeparator != null) {
            conf.setJsonNamespaceSeparator(namespaceSeparator);
        }
        if (primitiveArrayKeys != null) {
            conf.setPrimitiveArrayKeys(
                new HashSet<>(primitiveArrayKeys));
        }

        XMLInputFactory factory = depthProps != null
            ? new JettisonMappedReaderFactory(conf, depthProps)
            : new MappedXMLInputFactory(conf);
        return new JettisonReader(namespaceMap, factory.createXMLStreamReader(is, enc));
    }

    private static class JettisonMappedReaderFactory extends MappedXMLInputFactory {
        private DocumentDepthProperties depthProps;
        JettisonMappedReaderFactory(Configuration conf, DocumentDepthProperties depthProps) {
            super(conf);
            this.depthProps = depthProps;
        }
        protected JSONTokener createNewJSONTokener(String doc) {
            return new JSONTokener(doc, depthProps.getInnerElementCountThreshold());
        }
    }

    private static class JettisonReader extends DepthXMLStreamReader {
        private Map<String, String> namespaceMap;
        JettisonReader(Map<String, String> nsMap,
                                      XMLStreamReader reader) {
            super(reader);
            this.namespaceMap = nsMap;
        }


        public String getNamespaceURI(String arg0) {
            String uri = super.getNamespaceURI(arg0);
            if (uri == null) {
                uri = getNamespaceContext().getNamespaceURI(arg0);
            }
            return uri;
        }

        @Override
        public String getAttributePrefix(int n) {
            QName name = getAttributeName(n);
            if (name != null
                && XSI_URI.equals(name.getNamespaceURI())) {
                return XSI_PREFIX;
            }
            return super.getAttributePrefix(n);
        }

        @Override
        public NamespaceContext getNamespaceContext() {
            return new NamespaceContext() {

                public String getNamespaceURI(String prefix) {
                    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                        if (entry.getValue().equals(prefix)) {
                            return entry.getKey();
                        }
                    }
                    return null;
                }

                public String getPrefix(String ns) {
                    return namespaceMap.get(ns);
                }

                public Iterator<String> getPrefixes(String ns) {
                    String prefix = getPrefix(ns);
                    return prefix == null ? null : Collections.singletonList(prefix).iterator();
                }

            };
        }
    }

    private static class IgnoreContentJettisonWriter extends DelegatingXMLStreamWriter {

        private boolean writeXsiType;
        private boolean dropRootElement;
        private boolean rootDropped;
        private int index;

        IgnoreContentJettisonWriter(XMLStreamWriter writer, boolean writeXsiType,
                                    boolean dropRootElement) {
            super(writer);
            this.writeXsiType = writeXsiType;
            this.dropRootElement = dropRootElement;
        }

        public void writeAttribute(String prefix, String uri,
                                   String local, String value) throws XMLStreamException {
            if (!writeXsiType && XSI_PREFIX.equals(prefix)
                    && ("type".equals(local) || "nil".equals(local))) {
                return;
            }
            super.writeAttribute(prefix, uri, local, value);

        }

        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
            index++;
            if (dropRootElement && index - 1 == 0) {
                rootDropped = true;
                return;
            }
            super.writeStartElement(prefix, local, uri);
        }

        @Override
        public void writeStartElement(String local) throws XMLStreamException {
            this.writeStartElement("", local, "");
        }

        @Override
        public void writeEndElement() throws XMLStreamException {
            index--;
            if (rootDropped && index == 0) {
                return;
            }
            super.writeEndElement();
        }
    }

    private static class IgnoreMixedContentWriter extends DelegatingXMLStreamWriter {
        String lastText;
        boolean isMixed;
        List<Boolean> mixed = new LinkedList<>();

        IgnoreMixedContentWriter(XMLStreamWriter writer) {
            super(writer);
        }

        public void writeCharacters(String text) throws XMLStreamException {
            if (StringUtils.isEmpty(text.trim())) {
                lastText = text;
            } else if (lastText != null) {
                lastText += text;
            } else if (!isMixed) {
                super.writeCharacters(text);
            } else {
                lastText = text;
            }
        }

        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
            if (lastText != null) {
                isMixed = true;
            }
            mixed.add(0, isMixed);
            lastText = null;
            isMixed = false;
            super.writeStartElement(prefix, local, uri);
        }
        public void writeStartElement(String uri, String local) throws XMLStreamException {
            if (lastText != null) {
                isMixed = true;
            }
            mixed.add(0, isMixed);
            lastText = null;
            isMixed = false;
            super.writeStartElement(uri, local);
        }
        public void writeStartElement(String local) throws XMLStreamException {
            if (lastText != null) {
                isMixed = true;
            }
            mixed.add(0, isMixed);
            lastText = null;
            isMixed = false;
            super.writeStartElement(local);
        }
        public void writeEndElement() throws XMLStreamException {
            if (lastText != null && (!isMixed || !StringUtils.isEmpty(lastText.trim()))) {
                super.writeCharacters(lastText.trim());
            }
            super.writeEndElement();
            isMixed = mixed.get(0);
            mixed.remove(0);
        }


    }


}
