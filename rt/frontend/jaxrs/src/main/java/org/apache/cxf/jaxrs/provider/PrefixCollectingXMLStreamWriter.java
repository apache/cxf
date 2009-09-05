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

import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Spy on calls to setPrefix, and collect them. This is coded to assume that
 * a prefix is not reused for different URIs at different places in the tree.
 * If that assumption is not valid, Jettison is hopeless.
 */
public class PrefixCollectingXMLStreamWriter implements XMLStreamWriter {
    private XMLStreamWriter target;
    private Map<String, String> namespaces;
    
    public PrefixCollectingXMLStreamWriter(XMLStreamWriter target, 
                                           Map<String, String> namespaces) {
        this.target = target;
        this.namespaces = namespaces;
    }

    public void close() throws XMLStreamException {
        target.close();
    }

    public void flush() throws XMLStreamException {
        target.flush();
    }

    public NamespaceContext getNamespaceContext() {
        return target.getNamespaceContext();
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return target.getPrefix(uri);
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        return target.getProperty(name);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        target.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        target.setNamespaceContext(context);
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        String already = namespaces.get(uri);
        if (already != null && !prefix.equals(already)) {
            throw new RuntimeException("Reuse of prefix " + prefix);
        }
        namespaces.put(uri, prefix);
        target.setPrefix(prefix, uri);
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
        throws XMLStreamException {
        target.writeAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeAttribute(String namespaceURI, 
                               String localName, 
                               String value) throws XMLStreamException {
        target.writeAttribute(namespaceURI, localName, value);
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        target.writeAttribute(localName, value);
    }

    public void writeCData(String data) throws XMLStreamException {
        target.writeCData(data);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        target.writeCharacters(text, start, len);
    }

    public void writeCharacters(String text) throws XMLStreamException {
        target.writeCharacters(text);
    }

    public void writeComment(String data) throws XMLStreamException {
        target.writeComment(data);
    }

    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        target.writeDefaultNamespace(namespaceURI);
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        target.writeDTD(dtd);
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI)
        throws XMLStreamException {
        target.writeEmptyElement(prefix, localName, namespaceURI);
    }

    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        target.writeEmptyElement(namespaceURI, localName);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        target.writeEmptyElement(localName);
    }

    public void writeEndDocument() throws XMLStreamException {
        target.writeEndDocument();
    }

    public void writeEndElement() throws XMLStreamException {
        target.writeEndElement();
    }

    public void writeEntityRef(String name) throws XMLStreamException {
        target.writeEntityRef(name);
    }

    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        target.writeNamespace(prefix, namespaceURI);
    }

    public void writeProcessingInstruction(String pitarget, String data) throws XMLStreamException {
        target.writeProcessingInstruction(pitarget, data);
    }

    public void writeProcessingInstruction(String pitarget) throws XMLStreamException {
        target.writeProcessingInstruction(pitarget);
    }

    public void writeStartDocument() throws XMLStreamException {
        target.writeStartDocument();
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        target.writeStartDocument(encoding, version);
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        target.writeStartDocument(version);
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI)
        throws XMLStreamException {
        target.writeStartElement(prefix, localName, namespaceURI);
    }

    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        target.writeStartElement(namespaceURI, localName);
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        target.writeStartElement(localName);
    }
    

}
