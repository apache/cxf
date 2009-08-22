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

import org.apache.cxf.jaxrs.utils.JAXRSUtils;

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
        return true;
    }
    
    public void setNamespaceMap(Map<String, String> nsMap) {
        this.namespaceMap = new ConcurrentHashMap<String, String>(nsMap);
    }
    
    @Override
    protected XMLStreamWriter createStreamWriter(QName typeQName, OutputStream os) throws Exception {
        namespaceMap.putIfAbsent(typeQName.getNamespaceURI(), "ns1");
        XMLStreamWriter writer = JSONUtils.createStreamWriter(os, typeQName, writeXsiType, namespaceMap, 
                                                              serializeAsArray, arrayKeys);
        return writer;
    }
    
    @Override
    protected XMLStreamReader createStreamReader(Class<?> type, InputStream is) throws Exception {
        getQName(type);
        return JSONUtils.createStreamReader(is, readXsiType, namespaceMap);
    }
    
    private QName getQName(Class<?> type) {
        QName qname = JAXRSUtils.getClassQName(type); 
        namespaceMap.putIfAbsent(qname.getNamespaceURI(), "ns1");
        return qname;
    }
}
