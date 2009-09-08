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
package org.apache.cxf.jaxrs.provider;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

public final class JSONUtils {

    private static final String XSI_PREFIX = "xsi";
    private static final String XSI_URI = WSDLConstants.NS_SCHEMA_XSI; 
    private static final Charset UTF8 = Charset.forName("utf-8");

    private JSONUtils() {
    }
    
    public static XMLStreamWriter createStreamWriter(OutputStream os, 
                                                     QName qname, 
                                                     boolean writeXsiType,
                                                     ConcurrentHashMap<String, String> namespaceMap,
                                                     boolean serializeAsArray,
                                                     List<String> arrayKeys,
                                                     boolean dropRootElement) throws Exception {
        if (writeXsiType) {
            namespaceMap.putIfAbsent(XSI_URI, XSI_PREFIX);
        }
        Configuration c = new Configuration(namespaceMap);
        MappedNamespaceConvention convention = new PrefixRespectingMappedNamespaceConvention(c);
        AbstractXMLStreamWriter xsw = new MappedXMLStreamWriter(
                                            convention, 
                                            new OutputStreamWriter(os, UTF8));
        if (serializeAsArray) {
            if (arrayKeys != null) {
                for (String key : arrayKeys) {
                    xsw.seriliazeAsArray(key);
                }
            } else {
                String key = getKey(convention, qname);
                xsw.seriliazeAsArray(key);
            }
        }
        XMLStreamWriter writer = !writeXsiType || dropRootElement 
            ? new IgnoreContentJettisonWriter(xsw, writeXsiType, 
                                              dropRootElement ? qname : null) : xsw;
        
        return writer;
    }    
    
    public static XMLStreamWriter createIgnoreMixedContentWriterIfNeeded(XMLStreamWriter writer, 
                                                                         boolean ignoreMixedContent) {
        return ignoreMixedContent ? new IgnoreMixedContentWriter(writer) : writer; 
    }
    
    public static XMLStreamWriter createIgnoreNsWriterIfNeeded(XMLStreamWriter writer, 
                                                               boolean ignoreNamespaces) {
        return ignoreNamespaces ? new IgnoreNsWriter(writer) : writer; 
    }
    
    private static String getKey(MappedNamespaceConvention convention, QName qname) throws Exception {
        return convention.createKey(qname.getPrefix(), 
                                    qname.getNamespaceURI(),
                                    qname.getLocalPart());
            
        
    }
    
    public static XMLStreamReader createStreamReader(InputStream is, boolean readXsiType,
        ConcurrentHashMap<String, String> namespaceMap) throws Exception {
        if (readXsiType) {
            namespaceMap.putIfAbsent(XSI_URI, XSI_PREFIX);
        }
        MappedXMLInputFactory factory = new MappedXMLInputFactory(namespaceMap);
        return new JettisonReader(namespaceMap, factory.createXMLStreamReader(is));
    }
    
    private static class JettisonReader extends DepthXMLStreamReader {
        private Map<String, String> namespaceMap;
        public JettisonReader(Map<String, String> nsMap,
                                      XMLStreamReader reader) {
            super(reader);
            this.namespaceMap = nsMap;
        }
        
        @Override
        public String getAttributePrefix(int n) {
            QName name = getAttributeName(n);
            if (name != null 
                && XSI_URI.equals(name.getNamespaceURI())) {
                return XSI_PREFIX;
            } else {
                return super.getAttributePrefix(n);
            }
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

                public Iterator getPrefixes(String ns) {
                    String prefix = getPrefix(ns);
                    return prefix == null ? null : Collections.singletonList(prefix).iterator();
                }
                
            };
        }
    }
    
    private static class IgnoreContentJettisonWriter extends DelegatingXMLStreamWriter {
        
        private boolean writeXsiType;
        private QName ignoredQName;
        
        public IgnoreContentJettisonWriter(XMLStreamWriter writer, boolean writeXsiType, QName qname) {
            super(writer);
            this.writeXsiType = writeXsiType;
            ignoredQName = qname;
        }
        
        public void writeAttribute(String prefix, String uri,
                                   String local, String value) throws XMLStreamException {
            if (!writeXsiType && "type".equals(local) && "xsi".equals(prefix)) {
                return;
            }
            super.writeAttribute(prefix, uri, local, value);
            
        }
        
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
            if (ignoredQName != null && ignoredQName.getLocalPart().equals(local) 
                && ignoredQName.getNamespaceURI().equals(uri)) {
                return;
            }
            super.writeStartElement(prefix, local, uri);
        }
    }
    
    private static class IgnoreMixedContentWriter extends DelegatingXMLStreamWriter {
        String lastText;
        boolean isMixed;
        List<Boolean> mixed = new LinkedList<Boolean>();
        
        public IgnoreMixedContentWriter(XMLStreamWriter writer) {
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
    
    private static class IgnoreNsWriter extends DelegatingXMLStreamWriter {
        
        public IgnoreNsWriter(XMLStreamWriter writer) {
            super(writer);
        }

        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
            super.writeStartElement(local);
        }
        
        public void writeStartElement(String uri, String local) throws XMLStreamException {
            super.writeStartElement(local);
        }
        
        public void setPrefix(String pfx, String uri) throws XMLStreamException {
            // completed
        }
        
        public void setDefaultNamespace(String uri) throws XMLStreamException {
            // completed
        }
    }
}
