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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;

public class OutTransformWriter extends DelegatingXMLStreamWriter {
    private String defaultNamespace;
    private QNamesMap elementsMap;
    private Map<QName, QName> appendMap = new HashMap<QName, QName>(5);
    private Map<String, String> nsMap = new HashMap<String, String>(5);
    private List<Set<String>> writtenUris = new LinkedList<Set<String>>();
    
    private Set<QName> dropElements;
    private List<Integer> droppingIndexes = new LinkedList<Integer>();
    private List<QName> appendedElements = new LinkedList<QName>();
    private List<Integer> appendedIndexes = new LinkedList<Integer>();
    private int currentDepth;
    private boolean attributesToElements;
    private DelegatingNamespaceContext namespaceContext;
    
    public OutTransformWriter(XMLStreamWriter writer, 
                              Map<String, String> outMap,
                              Map<String, String> append,
                              List<String> dropEls,
                              boolean attributesToElements,
                              String defaultNamespace) {
        super(writer);
        elementsMap = new QNamesMap(outMap == null ? 0 : outMap.size());
        TransformUtils.convertToQNamesMap(outMap, elementsMap, nsMap);
        TransformUtils.convertToMapOfQNames(append, appendMap);
        dropElements = XMLUtils.convertStringsToQNames(dropEls);
        this.attributesToElements = attributesToElements;
        namespaceContext = new DelegatingNamespaceContext(
            writer.getNamespaceContext(), nsMap);
        this.defaultNamespace = defaultNamespace;
    }

    @Override
    public void writeNamespace(String prefix, String uri) throws XMLStreamException {
        if (matchesDropped()) {
            return;
        }
        String value = nsMap.get(uri);
        if (value != null && value.length() == 0) {
            return;
        }
        
        uri = value != null ? value : uri;
        
        if (writtenUris.get(0).contains(uri)) {
            return;
        }
        
        if (defaultNamespace != null && defaultNamespace.equals(uri)) {
            super.writeDefaultNamespace(uri);
        } else {
            if (prefix.length() == 0) {
                prefix = namespaceContext.findUniquePrefix(uri);
            }
            super.writeNamespace(prefix, uri);
        }
        writtenUris.get(0).add(uri);
    }
    
    @Override
    public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
        currentDepth++;
        Set<String> s;
        if (writtenUris.isEmpty()) {
            s = new HashSet<String>();
        } else {
            s = new HashSet<String>(writtenUris.get(0));
        }
        writtenUris.add(0, s);
        QName currentQName = new QName(uri, local);
        
        QName appendQName = appendMap.get(currentQName);
        if (appendQName != null && !appendedElements.contains(appendQName)) {
            currentDepth++;
            String theprefix = uri.equals(appendQName.getNamespaceURI()) ? prefix : "";
            write(new QName(appendQName.getNamespaceURI(), appendQName.getLocalPart(), theprefix));
            if (theprefix.length() > 0) {
                this.writeNamespace(theprefix, uri);
            }
            appendedElements.add(appendQName);
            appendedIndexes.add(currentDepth - 1);
        }
        
        if (dropElements.contains(currentQName)) {
            droppingIndexes.add(currentDepth - 1);
            return;
        }
        write(new QName(uri, local, prefix));
    }
    
    @Override
    public void writeEndElement() throws XMLStreamException {
        if (!writtenUris.isEmpty()) {
            writtenUris.remove(0);
        }
        --currentDepth;
        if (indexRemoved(droppingIndexes)) {
            return;
        }
        super.writeEndElement();
        if (indexRemoved(appendedIndexes)) {
            super.writeEndElement();
        }
    }
    
    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        if (matchesDropped()) {
            return;
        }
        super.writeCharacters(text);
    }
    
    private void write(QName qname) throws XMLStreamException {
        QName name = elementsMap.get(qname);
        if (name == null) {
            name = qname;
        }
        boolean writeNs = false;
        String prefix = "";
        if (name.getNamespaceURI().length() > 0) {
            if (qname.getPrefix().length() == 0) {
                prefix = namespaceContext.findUniquePrefix(name.getNamespaceURI());
                writeNs = true;
            } else {
                prefix = qname.getPrefix();
                namespaceContext.addPrefix(prefix, qname.getNamespaceURI());    
            }
            
        }
        if (defaultNamespace != null && defaultNamespace.equals(name.getNamespaceURI())) {
            prefix = "";
        }
        
        super.writeStartElement(prefix, name.getLocalPart(), name.getNamespaceURI());
        if (writeNs) {
            this.writeNamespace(prefix, name.getNamespaceURI());
        }
    }
    
    private boolean matchesDropped() {
        int size = droppingIndexes.size();
        if (size > 0 && droppingIndexes.get(size - 1) == currentDepth - 1) {
            return true;
        }
        return false;
    }
    
    private boolean indexRemoved(List<Integer> indexes) {
        int size = indexes.size();
        if (size > 0 && indexes.get(size - 1) == currentDepth) {
            indexes.remove(size - 1);
            return true;
        }
        return false;
    }
    
    @Override
    public NamespaceContext getNamespaceContext() {
        return namespaceContext; 
    }
    
    @Override
    public void writeAttribute(String uri, String local, String value) throws XMLStreamException {
        if (!attributesToElements) {
            super.writeAttribute(uri, local, value);
        } else {
            writeAttributeAsElement(uri, local, value);
        }
    }

    @Override
    public void writeAttribute(String local, String value) throws XMLStreamException {
        if (!attributesToElements) {
            super.writeAttribute(local, value);
        } else {
            writeAttributeAsElement("", local, value);
        }
    }
    
    private void writeAttributeAsElement(String uri, String local, String value)
        throws XMLStreamException {
        this.writeStartElement(uri, local);
        this.writeCharacters(value);
        this.writeEndElement();
    }
}
