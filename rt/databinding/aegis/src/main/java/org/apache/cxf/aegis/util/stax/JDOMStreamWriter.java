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
package org.apache.cxf.aegis.util.stax;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.EntityRef;
import org.jdom.Namespace;

public class JDOMStreamWriter implements XMLStreamWriter {
    private Stack<Element> stack = new Stack<Element>();

    private Document document;

    private Element currentNode;

    private NamespaceContext context;

    private Map properties = new HashMap();

    public JDOMStreamWriter() {
    }

    public JDOMStreamWriter(Element e) {
        newChild(e);
    }

    public void close() throws XMLStreamException {
    }

    public void flush() throws XMLStreamException {
    }    

    public void writeStartElement(String local) throws XMLStreamException {
        newChild(new Element(local));
    }

    private void newChild(Element element) {
        if (currentNode != null) {
            stack.push(currentNode);
            currentNode.addContent(element);
        } else {
            if (document != null) {
                document.setRootElement(element);
            }
        }

        JDOMNamespaceContext ctx = new JDOMNamespaceContext();
        ctx.setElement(element);
        this.context = ctx;

        currentNode = element;
    }

    public void writeStartElement(String namespace, String local) throws XMLStreamException {
        newChild(new Element(local, namespace));
    }

    public void writeStartElement(String prefix, String local, String namespace) throws XMLStreamException {
        if (prefix == null || prefix.equals("")) {
            writeStartElement(namespace, local);
        } else {
            newChild(new Element(local, prefix, namespace));
        }
    }

    public void writeEmptyElement(String namespace, String local) throws XMLStreamException {
        writeStartElement(namespace, local);
    }

    public void writeEmptyElement(String prefix, String namespace, String local) throws XMLStreamException {
        writeStartElement(prefix, namespace, local);
    }

    public void writeEmptyElement(String local) throws XMLStreamException {
        writeStartElement(local);
    }

    public void writeEndElement() throws XMLStreamException {
        currentNode = stack.pop();
    }

    public void writeEndDocument() throws XMLStreamException {
    }

    public void writeAttribute(String local, String value) throws XMLStreamException {
        currentNode.setAttribute(new Attribute(local, value));
    }

    public void writeAttribute(String prefix, String namespace, String local, String value)
        throws XMLStreamException {
        currentNode.setAttribute(new Attribute(local, value, Namespace.getNamespace(prefix, namespace)));
    }

    public void writeAttribute(String namespace, String local, String value) throws XMLStreamException {
        currentNode.setAttribute(new Attribute(local, value, Namespace.getNamespace(namespace)));
    }

    public void writeNamespace(String prefix, String namespace) throws XMLStreamException {
        Namespace decNS = currentNode.getNamespace(prefix);

        if (decNS == null || !decNS.getURI().equals(namespace)) {
            currentNode.addNamespaceDeclaration(Namespace.getNamespace(prefix, namespace));
        }
    }

    public void writeDefaultNamespace(String namespace) throws XMLStreamException {
        currentNode.addNamespaceDeclaration(Namespace.getNamespace("", namespace));
    }

    public void writeComment(String value) throws XMLStreamException {
        currentNode.addContent(new Comment(value));
    }

    public void writeProcessingInstruction(String arg0) throws XMLStreamException {
    }

    public void writeProcessingInstruction(String arg0, String arg1) throws XMLStreamException {
    }

    public void writeCData(String data) throws XMLStreamException {
        currentNode.addContent(new CDATA(data));
    }

    public void writeDTD(String arg0) throws XMLStreamException {

    }

    public void writeEntityRef(String ref) throws XMLStreamException {
        currentNode.addContent(new EntityRef(ref));
    }

    public void writeStartDocument() throws XMLStreamException {
        document = new Document(new Element("root"));
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        writeStartDocument();

        // TODO: set encoding/version
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        writeStartDocument();

        // TODO: set encoding/version
    }

    public void writeCharacters(String text) throws XMLStreamException {
        currentNode.addContent(text);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        // TODO Auto-generated method stub
        currentNode.addContent(new String(text, start, len));
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return JDOMNamespaceContext.rawGetPrefix(currentNode, uri);
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

    public Document getDocument() {
        return document;
    }
}
