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
package org.apache.cxf.aegis.xml.jdom;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.util.stax.JDOMStreamReader;
import org.apache.cxf.aegis.xml.AbstractMessageReader;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.stax.AttributeReader;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;

public class JDOMReader extends AbstractMessageReader implements MessageReader {
    private Element element;
    private int currentChild;
    private int currentAttribute;
    private List elements;
    private QName qname;

    public JDOMReader(Element element) {
        this.element = element;
        this.elements = element.getChildren();
    }

    public String getValue() {
        return element.getValue();
    }

    public String getValue(String ns, String attr) {
        return element.getAttributeValue(attr, ns);
    }

    public boolean hasMoreElementReaders() {
        return currentChild < elements.size();
    }

    public MessageReader getNextElementReader() {
        currentChild++;
        return new JDOMReader((Element)elements.get(currentChild - 1));
    }

    public QName getName() {
        if (qname == null) {
            qname = new QName(element.getNamespaceURI(), element.getName(), element.getNamespacePrefix());
        }
        return qname;
    }

    public String getLocalName() {
        return element.getName();
    }

    public String getNamespace() {
        return element.getNamespaceURI();
    }

    @Override
    public XMLStreamReader getXMLStreamReader() {
        return new JDOMStreamReader(element);
    }

    public boolean hasMoreAttributeReaders() {
        return currentAttribute < element.getAttributes().size();
    }

    public MessageReader getAttributeReader(QName attName) {
        String value = element.getAttributeValue(attName.getLocalPart(), Namespace.getNamespace(attName
            .getNamespaceURI()));
        return new AttributeReader(attName, value);
    }

    public MessageReader getNextAttributeReader() {
        Attribute att = (Attribute)element.getAttributes().get(currentAttribute);
        currentAttribute++;

        return new AttributeReader(new QName(att.getNamespaceURI(), att.getName()), att.getValue());
    }

    public String getNamespaceForPrefix(String prefix) {
        Namespace namespace = element.getNamespace(prefix);
        return null == namespace ? null : namespace.getURI();
    }
}
