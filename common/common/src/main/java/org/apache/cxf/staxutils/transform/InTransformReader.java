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
package org.apache.cxf.staxutils.transform;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.staxutils.DepthXMLStreamReader;

public class InTransformReader extends DepthXMLStreamReader {
    
    private static final String INTERN_NAMES = "org.codehaus.stax2.internNames";
    private static final String INTERN_NS = "org.codehaus.stax2.internNsUris";
    
    private QNamesMap inElementsMap;
    private Map<QName, QName> inAppendMap = new HashMap<QName, QName>(5);
    private Map<String, String> nsMap = new HashMap<String, String>(5);
    private QName currentQName;
    private QName previousQName;
    private int previousDepth = -1;
    private boolean blockOriginalReader = true;
    private DelegatingNamespaceContext namespaceContext;
    
    public InTransformReader(XMLStreamReader reader, 
                             Map<String, String> inMap,
                             Map<String, String> appendMap,
                             boolean blockOriginalReader) {
        super(reader);
        inElementsMap = new QNamesMap(inMap == null ? 0 : inMap.size());
        this.blockOriginalReader = blockOriginalReader;
        TransformUtils.convertToQNamesMap(inMap, inElementsMap, nsMap);
        TransformUtils.convertToMapOfQNames(appendMap, inAppendMap);
        namespaceContext = new DelegatingNamespaceContext(
            reader.getNamespaceContext(), nsMap);
    }
    
    @Override
    // If JAXB schema validation is disabled then returning 
    // the native reader and thus bypassing this reader may work
    public XMLStreamReader getReader() {
        return blockOriginalReader ? this : super.getReader();
    }
    
    public int next() throws XMLStreamException {
        if (currentQName != null) {
            return XMLStreamConstants.START_ELEMENT;
        } else if (previousDepth != -1 && previousDepth == getDepth() + 1) {
            previousDepth = -1;
            return XMLStreamConstants.END_ELEMENT;
        } else {
            return super.next();
        }
    }
    
    public Object getProperty(String name) throws IllegalArgumentException {

        if (INTERN_NAMES.equals(name) || INTERN_NS.equals(name)) {
            return Boolean.FALSE;
        }
        return super.getProperty(name);
    }

    public String getLocalName() {
        QName cQName = getCurrentName();
        if (cQName != null) {
            String name = cQName.getLocalPart();
            resetCurrentQName();
            return name;
        }
        return super.getLocalName();
    }

    private QName getCurrentName() {
        return currentQName != null ? currentQName 
            : previousQName != null ? previousQName : null;
    }
    
    private void resetCurrentQName() {
        currentQName = previousQName;
        previousQName = null;
    }
    
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    public String getPrefix() {
        QName name = readCurrentElement();
        String prefix = name.getPrefix();
        if (prefix.length() == 0) {
            prefix = namespaceContext.findUniquePrefix(name.getNamespaceURI());
        }
        return prefix;
    }
     
    public String getNamespaceURI(int index) {
        String ns = super.getNamespaceURI(index);
        String actualNs = nsMap.get(ns);
        if (actualNs != null) {
            return actualNs;
        } else {
            return ns;
        }
    }
    
    public String getNamespacePrefix(int index) {
        String ns = super.getNamespaceURI(index);
        String actualNs = nsMap.get(ns);
        if (actualNs != null) {
            return namespaceContext.findUniquePrefix(actualNs);
        } else {
            return namespaceContext.getPrefix(ns);
        }
    }
    
    public String getNamespaceURI() {
     
        QName theName = readCurrentElement();
        QName appendQName = inAppendMap.remove(theName);
        if (appendQName != null) {
            previousDepth = getDepth();
            previousQName = theName;
            currentQName = appendQName;
            return currentQName.getNamespaceURI();
        }
        QName expected = inElementsMap.get(theName);
        if (expected == null) {
            return theName.getNamespaceURI();
        }
        currentQName = expected;
        return currentQName.getNamespaceURI();
    }
    
    private QName readCurrentElement() {
        if (currentQName != null) {
            return currentQName;
        }
        String ns = super.getNamespaceURI();
        String name = super.getLocalName();
        String prefix = super.getPrefix();
        return new QName(ns, name, prefix == null ? "" : prefix);
    }
    
    public QName getName() { 
        return new QName(getNamespaceURI(), getLocalName());
    }
}
