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

package org.apache.cxf.wsdl;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class PrettyPrintXMLStreamWriter implements XMLStreamWriter {

    static final Map<Class<?>, Integer> WSDL_INDENT_MAP = new HashMap<Class<?>, Integer>();
    static final int DEFAULT_INDENT_LEVEL = 2;

    XMLStreamWriter baseWriter;

    int indent;
    Stack<CurrentElement> elems = new Stack<CurrentElement>();
    QName currElem;
    boolean nestedStartElement;   

    public PrettyPrintXMLStreamWriter(XMLStreamWriter writer,
                                      Class<?> parent) {
        baseWriter = writer;
        indent = getIndentLevel(parent);
    }

    public void writeSpaces() throws XMLStreamException {
        for (int i = 0; i < indent; i++) {
            baseWriter.writeCharacters(" ");
        }
    }

    public void indentWithSpaces() throws XMLStreamException {
        writeSpaces();
        indent();
    }

    public void indent() {
        indent += DEFAULT_INDENT_LEVEL;
    }
    
    public void unindent() {
        indent -= DEFAULT_INDENT_LEVEL;
    }

    public void close() throws XMLStreamException {
        baseWriter.close();
    }

    public void flush() throws XMLStreamException {
        baseWriter.flush();
    }

    public NamespaceContext getNamespaceContext() {
        return baseWriter.getNamespaceContext();
    }


    public java.lang.String getPrefix(java.lang.String uri) throws XMLStreamException {
        return baseWriter.getPrefix(uri);
    }

    public java.lang.Object getProperty(java.lang.String name) throws IllegalArgumentException {
        return baseWriter.getProperty(name);
    }

    public void setDefaultNamespace(java.lang.String uri) throws XMLStreamException {
        baseWriter.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        baseWriter.setNamespaceContext(context);
    }

    public void setPrefix(java.lang.String prefix, java.lang.String uri)
        throws XMLStreamException {
        baseWriter.setPrefix(prefix, uri);
    }

    public void writeAttribute(java.lang.String localName, java.lang.String value)
        throws XMLStreamException {
        baseWriter.writeAttribute(localName, value);
    }

    public void writeAttribute(java.lang.String namespaceURI,
                        java.lang.String localName,
                        java.lang.String value) throws XMLStreamException {
        baseWriter.writeAttribute(namespaceURI, localName, value);
    }

    public void writeAttribute(java.lang.String prefix,
                        java.lang.String namespaceURI,
                        java.lang.String localName,
                        java.lang.String value) throws XMLStreamException {
        baseWriter.writeAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeCData(java.lang.String data) throws XMLStreamException {
        baseWriter.writeCData(data);
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        baseWriter.writeCharacters(text, start, len);
    }

    public void writeCharacters(java.lang.String text) throws XMLStreamException {
        baseWriter.writeCharacters(text);
    }

    public void writeComment(java.lang.String data) throws XMLStreamException {
        baseWriter.writeComment(data);
    }

    public void writeDefaultNamespace(java.lang.String namespaceURI) throws XMLStreamException {
        baseWriter.writeDefaultNamespace(namespaceURI);
    }

    public void writeDTD(java.lang.String dtd) throws XMLStreamException {
        baseWriter.writeDTD(dtd);
    }

    public void writeEmptyElement(java.lang.String localName) throws XMLStreamException {
        baseWriter.writeEmptyElement(localName);
    }

    public void writeEmptyElement(java.lang.String namespaceURI, java.lang.String localName)
        throws XMLStreamException {
        baseWriter.writeEmptyElement(localName, namespaceURI);
    }

    public void writeEmptyElement(java.lang.String prefix,
                           java.lang.String localName,
                           java.lang.String namespaceURI) throws XMLStreamException {
        baseWriter.writeEmptyElement(prefix, localName, namespaceURI);
    }

    public void writeEndDocument() throws XMLStreamException {
        baseWriter.writeEndDocument();
    }

    public void writeEndElement() throws XMLStreamException {
        CurrentElement elem = (CurrentElement) elems.pop();
        unindent();
        if (elem.hasChildElements()) {
            baseWriter.writeCharacters("\n");
            writeSpaces();
        }
        baseWriter.writeEndElement();
        if (elems.empty()) {
            baseWriter.writeCharacters("\n");
        }
    }

    public void writeEntityRef(java.lang.String name) throws XMLStreamException {
        baseWriter.writeEntityRef(name);
    }

    public void writeNamespace(java.lang.String prefix, java.lang.String namespaceURI)
        throws XMLStreamException {
        baseWriter.writeNamespace(prefix, namespaceURI);
    }

    public void writeProcessingInstruction(java.lang.String target)
        throws XMLStreamException {
        baseWriter.writeProcessingInstruction(target);
    }

    public void writeProcessingInstruction(java.lang.String target, java.lang.String data)
        throws XMLStreamException {
        baseWriter.writeProcessingInstruction(target, data);
    }

    public void writeStartDocument() throws XMLStreamException {
        baseWriter.writeStartDocument();
    }

    public void writeStartDocument(java.lang.String version) throws XMLStreamException {
        baseWriter.writeStartDocument(version);
    }

    public void writeStartDocument(java.lang.String encoding, java.lang.String version)
        throws XMLStreamException {
        baseWriter.writeStartDocument(encoding, version);
    }
     
    public void writeStartElement(java.lang.String localName) throws XMLStreamException {
        writeStartElement(null, localName, null);
    }
     
    public void writeStartElement(java.lang.String namespaceURI, java.lang.String localName)
        throws XMLStreamException {
        writeStartElement(null, localName, namespaceURI);
    }
     
    public void writeStartElement(java.lang.String prefix,
                           java.lang.String localName,
                           java.lang.String namespaceURI) throws XMLStreamException {
        QName currElemName = new QName(namespaceURI, localName);
        if (elems.empty()) {
            indentWithSpaces();
        } else {
            baseWriter.writeCharacters("");
            baseWriter.writeCharacters("\n");
            indentWithSpaces();
            CurrentElement elem = (CurrentElement) elems.peek();
            elem.setChildElements(true);
        }
        if (prefix == null && namespaceURI == null) {
            baseWriter.writeStartElement(localName);
        } else if (prefix == null) {
            baseWriter.writeStartElement(namespaceURI, localName);            
        } else {
            baseWriter.writeStartElement(prefix, localName, namespaceURI);
        }
        elems.push(new CurrentElement(currElemName));
    }

    private int getIndentLevel(Class<?> parent) {
        Integer result = (Integer)WSDL_INDENT_MAP.get(parent);
        if (result == null) {
            return DEFAULT_INDENT_LEVEL;
        }
        return result.intValue();
    }

    static {
        WSDL_INDENT_MAP.put(Definition.class, new Integer(DEFAULT_INDENT_LEVEL));
        WSDL_INDENT_MAP.put(Binding.class, new Integer(DEFAULT_INDENT_LEVEL * 2));
        WSDL_INDENT_MAP.put(BindingFault.class, new Integer(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(BindingInput.class, new Integer(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(BindingOutput.class, new Integer(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(BindingOperation.class, new Integer(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(Message.class, new Integer(DEFAULT_INDENT_LEVEL * 2));
        WSDL_INDENT_MAP.put(Operation.class, new Integer(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(Port.class, new Integer(DEFAULT_INDENT_LEVEL * 3));
        WSDL_INDENT_MAP.put(Service.class, new Integer(DEFAULT_INDENT_LEVEL * 2));
        WSDL_INDENT_MAP.put(Types.class, new Integer(DEFAULT_INDENT_LEVEL * 2));
    }

    class CurrentElement {
        private QName name;
        private boolean hasChildElements;

        CurrentElement(QName qname) {
            name = qname;
        }

        public QName getQName() {
            return name;
        }

        public boolean hasChildElements() {
            return hasChildElements;
        }

        public void setChildElements(boolean childElements) {
            hasChildElements = childElements;
        }
    }
     
}
