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
package org.apache.cxf.staxutils;

import java.util.Collections;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.helpers.XMLUtils;

public class W3CDOMStreamWriter implements XMLStreamWriter {
    static final String XML_NS = "http://www.w3.org/2000/xmlns/";
    private Stack<Node> stack = new Stack<Node>();
    private Document document;
    private Node currentNode;
    private NamespaceContext context = new W3CNamespaceContext();
    private boolean nsRepairing;
    private Map properties = Collections.EMPTY_MAP;

    public W3CDOMStreamWriter() throws ParserConfigurationException {
        document = XMLUtils.newDocument();
    }

    public W3CDOMStreamWriter(DocumentBuilder builder) {
        document = builder.newDocument();
    }

    public W3CDOMStreamWriter(Document document) {
        this.document = document;
    }
    public W3CDOMStreamWriter(DocumentFragment frag) {
        this.document = frag.getOwnerDocument();
        currentNode = frag;
    }

    public W3CDOMStreamWriter(Element e) {
        this.document = e.getOwnerDocument();
        
        currentNode = e;
        ((W3CNamespaceContext)context).setElement(e);
    }
    
    public Element getCurrentNode() {
        if (currentNode instanceof Element) {
            return (Element)currentNode;
        }
        return null;
    }
    public DocumentFragment getCurrentFragment() {
        if (currentNode instanceof DocumentFragment) {
            return (DocumentFragment)currentNode;
        }
        return null;
    }
    
    public void setNsRepairing(boolean b) {
        nsRepairing = b;
    }
    public boolean isNsRepairing() {
        return nsRepairing;
    }
    public void setProperties(Map properties) {
        this.properties = properties;
    }

    public Document getDocument() {
        return document;
    }

    public void writeStartElement(String local) throws XMLStreamException {
        newChild(document.createElementNS(null, local));
    }

    protected void newChild(Element element) {
        setChild(element, true);
    }
    protected void setChild(Element element, boolean append) {
        if (currentNode != null) {
            stack.push(currentNode);
            if (append) {
                currentNode.appendChild(element);
            }
        } else {
            if (append) {
                document.appendChild(element);
            }
        }
        if (!(context instanceof W3CNamespaceContext)) {
            context = new W3CNamespaceContext();
        }
        ((W3CNamespaceContext)context).setElement(element);
        currentNode = element;
    }

    public void writeStartElement(String namespace, String local) throws XMLStreamException {
        newChild(document.createElementNS(namespace, local));
    }

    public void writeStartElement(String prefix, String local, String namespace) throws XMLStreamException {
        if (prefix == null || prefix.equals("")) {
            writeStartElement(namespace, local);
        } else {
            newChild(document.createElementNS(namespace, prefix + ":" + local));
            if (nsRepairing
                && !prefix.equals(getNamespaceContext().getPrefix(namespace))) {
                writeNamespace(prefix, namespace);
            }
        }
    }

    public void writeEmptyElement(String namespace, String local) throws XMLStreamException {
        writeStartElement(namespace, local);
        writeEndElement();
    }

    public void writeEmptyElement(String prefix, String namespace, String local) throws XMLStreamException {
        writeStartElement(prefix, namespace, local);
        writeEndElement();
    }

    public void writeEmptyElement(String local) throws XMLStreamException {
        writeStartElement(local);
        writeEndElement();
    }

    public void writeEndElement() throws XMLStreamException {
        if (stack.size() > 0) {
            currentNode = stack.pop();
        } else {
            currentNode = null;
        }
        if (context instanceof W3CNamespaceContext && currentNode instanceof Element) {
            ((W3CNamespaceContext)context).setElement((Element)currentNode);
        } else if (context instanceof MapNamespaceContext) {
            ((MapNamespaceContext) context).setTargetNode(currentNode);
        }
    }

    public void writeEndDocument() throws XMLStreamException {
    }

    public void writeAttribute(String local, String value) throws XMLStreamException {
        Attr a;
        if (local.startsWith("xmlns:")) {
            a = document.createAttributeNS(XML_NS, local);
        } else {
            a = document.createAttributeNS(null, local);
        }
        a.setValue(value);
        ((Element)currentNode).setAttributeNode(a);
    }

    public void writeAttribute(String prefix, String namespace, String local, String value)
        throws XMLStreamException {
        if (prefix.length() > 0) {
            local = prefix + ":" + local;
        }
        
        Attr a = document.createAttributeNS(namespace, local);
        a.setValue(value);
        ((Element)currentNode).setAttributeNodeNS(a);
        if (nsRepairing
            && !prefix.equals(getNamespaceContext().getPrefix(namespace))) {
            writeNamespace(prefix, namespace);
        }
    }

    public void writeAttribute(String namespace, String local, String value) throws XMLStreamException {
        Attr a = document.createAttributeNS(namespace, local);
        a.setValue(value);
        ((Element)currentNode).setAttributeNodeNS(a);
    }

    public void writeNamespace(String prefix, String namespace) throws XMLStreamException {
        if (prefix.length() == 0) {
            writeDefaultNamespace(namespace);
        } else {
            Attr attr = document.createAttributeNS(XML_NS, "xmlns:" + prefix);
            attr.setValue(namespace);
            ((Element)currentNode).setAttributeNodeNS(attr);
        }
    }

    public void writeDefaultNamespace(String namespace) throws XMLStreamException {
        Attr attr = document.createAttributeNS(XML_NS, "xmlns");
        attr.setValue(namespace);
        ((Element)currentNode).setAttributeNodeNS(attr);
    }

    public void writeComment(String value) throws XMLStreamException {
        if (currentNode == null) {
            document.appendChild(document.createComment(value));
        } else {
            currentNode.appendChild(document.createComment(value));
        }
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        if (currentNode == null) {
            document.appendChild(document.createProcessingInstruction(target, null));
        } else {
            currentNode.appendChild(document.createProcessingInstruction(target, null));
        }
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        if (currentNode == null) {
            document.appendChild(document.createProcessingInstruction(target, data));
        } else {
            currentNode.appendChild(document.createProcessingInstruction(target, data));
        }
    }

    public void writeCData(String data) throws XMLStreamException {
        currentNode.appendChild(document.createCDATASection(data));
    }

    public void writeDTD(String arg0) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    public void writeEntityRef(String ref) throws XMLStreamException {
        currentNode.appendChild(document.createEntityReference(ref));
    }

    public void writeStartDocument() throws XMLStreamException {
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        try {
            document.setXmlVersion(version);
        } catch (Exception ex) {
            //ignore - likely not DOM level 3
        }
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        try {
            document.setXmlVersion(version);
        } catch (Exception ex) {
            //ignore - likely not DOM level 3
        }
    }

    public void writeCharacters(String text) throws XMLStreamException {
        currentNode.appendChild(document.createTextNode(text));
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        writeCharacters(new String(text, start, len));
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return context == null ? null : context.getPrefix(uri);
    }

    public void setPrefix(String arg0, String arg1) throws XMLStreamException {
    }

    public void setDefaultNamespace(String arg0) throws XMLStreamException {
    }

    public void setNamespaceContext(NamespaceContext ctx) throws XMLStreamException {
        this.context = ctx;
    }

    public NamespaceContext getNamespaceContext() {
        return context;
    }

    public Object getProperty(String prop) throws IllegalArgumentException {
        return properties.get(prop);
    }

    public void close() throws XMLStreamException {
    }

    public void flush() throws XMLStreamException {
    }
    
}
