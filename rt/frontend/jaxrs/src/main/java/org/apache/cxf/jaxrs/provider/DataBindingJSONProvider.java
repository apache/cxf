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

import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

@Provider
@Produces("application/json")
@Consumes("application/json")
public class DataBindingJSONProvider extends DataBindingProvider {
    
    private List<String> arrayKeys;
    private boolean serializeAsArray;
    private ConcurrentHashMap<String, String> namespaceMap = new ConcurrentHashMap<String, String>();
    private boolean writeXsiType = true;
    private boolean readXsiType = true;
    private boolean dropRootElement;
    private boolean ignoreMixedContent; 
    
    public void setDropRootElement(boolean dropRootElement) {
        this.dropRootElement = dropRootElement;
    }
    
    public void setWriteXsiType(boolean write) {
        writeXsiType = write;
    }
    
    public void setReadXsiType(boolean read) {
        readXsiType = read;
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
    protected XMLStreamWriter createWriter(Class<?> type, Type genericType, OutputStream os) 
        throws Exception {
        QName qname = null;
        if (!InjectionUtils.isSupportedCollectionOrArray(type)) {
            qname = getQName(type);
        } else {
            qname = getQName(InjectionUtils.getActualType(genericType));
        }
        XMLStreamWriter writer = JSONUtils.createStreamWriter(os, qname, writeXsiType, namespaceMap, 
                                            serializeAsArray, arrayKeys, dropRootElement);
        return JSONUtils.createIgnoreMixedContentWriterIfNeeded(writer, ignoreMixedContent);
    }
    
    @Override
    protected void writeToWriter(XMLStreamWriter writer, Object o) throws Exception {
        writer.writeStartDocument();
        super.writeToWriter(writer, o);
        writer.writeEndDocument();
    }
    
    @Override
    protected XMLStreamReader createReader(Class<?> type, Type genericType, InputStream is) 
        throws Exception {
        if (!InjectionUtils.isSupportedCollectionOrArray(type)) {
            getQName(type);
        } else {
            getQName(InjectionUtils.getActualType(genericType));
        }
        return JSONUtils.createStreamReader(is, readXsiType, namespaceMap);
    }
    
    private QName getQName(Class<?> type) {
        QName qname = JAXRSUtils.getClassQName(type); 
        namespaceMap.putIfAbsent(qname.getNamespaceURI(), "ns1");
        return qname;
    }

    public void setIgnoreMixedContent(boolean ignoreMixedContent) {
        this.ignoreMixedContent = ignoreMixedContent;
    }
    
}
