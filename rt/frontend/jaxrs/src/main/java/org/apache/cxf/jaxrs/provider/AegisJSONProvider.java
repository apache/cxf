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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

@Provider
@Produces({"application/json" })
@Consumes({"application/json" })
public final class AegisJSONProvider extends AegisElementProvider  {
    
    private List<String> arrayKeys;
    private boolean serializeAsArray;
    private ConcurrentHashMap<String, String> namespaceMap = new ConcurrentHashMap<String, String>();
    
    public AegisJSONProvider() {
    }
    
    public void setArrayKeys(List<String> keys) {
        this.arrayKeys = keys;
    }
    
    public void setSerializeAsArray(boolean asArray) {
        this.serializeAsArray = asArray;
    }
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return false;
    }
    
    public void setNamespaceMap(Map<String, String> nsMap) {
        this.namespaceMap = new ConcurrentHashMap<String, String>(nsMap);
    }
    
    @Override
    protected XMLStreamWriter createStreamWriter(Class<?> type, OutputStream os) throws Exception {
        QName qname = getQName(type);
        if (writeXsiType) {
            namespaceMap.putIfAbsent("http://www.w3.org/2001/XMLSchema-instance", "xsins");
        }
        Configuration c = new Configuration(namespaceMap);
        MappedNamespaceConvention convention = new MappedNamespaceConvention(c);
        AbstractXMLStreamWriter xsw = new MappedXMLStreamWriter(
                                            convention, 
                                            new OutputStreamWriter(os, "UTF-8"));
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
    
    @Override
    protected XMLStreamReader createStreamReader(Class<?> type, InputStream is) throws Exception {
        if (readXsiType) {
            namespaceMap.putIfAbsent("http://www.w3.org/2001/XMLSchema-instance", "xsins");
        }
        getQName(type);
        MappedXMLInputFactory factory = new MappedXMLInputFactory(namespaceMap);
        return new NamespaceContextReader(factory.createXMLStreamReader(is));
    }
    
    private QName getQName(Class<?> type) {
        QName qname = JAXRSUtils.getClassQName(type); 
        namespaceMap.putIfAbsent(qname.getNamespaceURI(), "ns1");
        return qname;
    }
    
    private String getKey(MappedNamespaceConvention convention, QName qname) throws Exception {
        return convention.createKey(qname.getPrefix(), 
                                    qname.getNamespaceURI(),
                                    qname.getLocalPart());
    }
    
    private class NamespaceContextReader extends DepthXMLStreamReader {
        public NamespaceContextReader(XMLStreamReader reader) {
            super(reader);
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
