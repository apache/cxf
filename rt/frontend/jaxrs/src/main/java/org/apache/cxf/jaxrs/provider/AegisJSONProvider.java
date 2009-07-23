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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.util.PackageUtils;
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
        MappedXMLInputFactory factory = new MappedXMLInputFactory(namespaceMap);
        return factory.createXMLStreamReader(is);
    }
    
    private QName getQName(Class<?> type) {
        String nsURI = PackageUtils.getNamespace(PackageUtils.getPackageName(type));
        if (nsURI.endsWith("/")) {
            nsURI = nsURI.substring(0, nsURI.length() - 1);
        }
        QName qname = new QName(nsURI, type.getSimpleName(), "ns1"); 
        namespaceMap.putIfAbsent(nsURI, "ns1");
        return qname;
    }
    
    private String getKey(MappedNamespaceConvention convention, QName qname) throws Exception {
        return convention.createKey(qname.getPrefix(), 
                                    qname.getNamespaceURI(),
                                    qname.getLocalPart());
    }
}
