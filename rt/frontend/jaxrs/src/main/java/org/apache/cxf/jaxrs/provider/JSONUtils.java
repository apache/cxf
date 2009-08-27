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
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

public final class JSONUtils {

    private static final String XSI_PREFIX = "xsi";
    private static final String XSI_URI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
    private static final Charset UTF8 = Charset.forName("utf-8");

    private JSONUtils() {
    }
    
    public static XMLStreamWriter createStreamWriter(OutputStream os, 
                                                     QName qname, boolean writeXsiType,
                                                     Map<String, String> namespaceMap,
                                                     boolean serializeAsArray,
                                                     List<String> arrayKeys) throws Exception {
        if (writeXsiType) {
            namespaceMap.put(XSI_URI, XSI_PREFIX);
        }
        Configuration c = new Configuration(namespaceMap);
        MappedNamespaceConvention convention = new MappedNamespaceConvention(c);
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
        return xsw;
    }    
    
    private static String getKey(MappedNamespaceConvention convention, QName qname) throws Exception {
        return convention.createKey(qname.getPrefix(), 
                                    qname.getNamespaceURI(),
                                    qname.getLocalPart());
            
        
    }
    
    public static XMLStreamReader createStreamReader(InputStream is, boolean readXsiType,
                                               Map<String, String> namespaceMap) throws Exception {
        if (readXsiType) {
            namespaceMap.put(XSI_URI, XSI_PREFIX);
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
}
