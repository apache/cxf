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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.util.stax.JDOMNamespaceContext;
import org.apache.cxf.aegis.xml.AbstractMessageWriter;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;

public class JDOMWriter extends AbstractMessageWriter {
    private Element element;

    public JDOMWriter(Element element) {
        this.element = element;
    }

    public void writeValue(Object value) {
        // an NPE is not helpful, and code at a higher level is responsible for
        // xsi:nil processing.
        if (value != null) {
            element.addContent(value.toString());
        }
    }

    public void writeValue(Object value, String ns, String attr) {
        String prefix = getUniquePrefix(element, ns);

        element.setAttribute(new Attribute(attr, value.toString(), Namespace.getNamespace(prefix, ns)));
    }

    public MessageWriter getElementWriter(String name) {
        return getElementWriter(name, element.getNamespaceURI());
    }

    public MessageWriter getElementWriter(String name, String namespace) {
        String prefix = getUniquePrefix(element, namespace);

        Element child = new Element(name, Namespace.getNamespace(prefix, namespace));
        element.addContent(child);

        return new JDOMWriter(child);
    }

    public MessageWriter getElementWriter(QName qname) {
        return getElementWriter(qname.getLocalPart(), qname.getNamespaceURI());
    }

    public String getPrefixForNamespace(String namespace) {
        return getUniquePrefix(element, namespace);
    }

    public XMLStreamWriter getXMLStreamWriter() {
        throw new UnsupportedOperationException("Stream writing not supported from a W3CDOMWriter.");
    }

    public String getPrefixForNamespace(String namespace, String hint) {
        // todo: this goes for the option of ignoring the hint - we should
        // probably at least attempt to honour it
        return getUniquePrefix(element, namespace);
    }

    public MessageWriter getAttributeWriter(String name) {
        Attribute att = new Attribute(name, "", element.getNamespace());
        element.setAttribute(att);
        return new AttributeWriter(att);
    }

    public MessageWriter getAttributeWriter(String name, String namespace) {
        Attribute att;
        if (namespace != null && namespace.length() > 0) {
            String prefix = getUniquePrefix(element, namespace);
            att = new Attribute(name, "", Namespace.getNamespace(prefix, namespace));
        } else {
            att = new Attribute(name, "");
        }

        element.setAttribute(att);
        return new AttributeWriter(att);
    }

    public MessageWriter getAttributeWriter(QName qname) {
        return getAttributeWriter(qname.getLocalPart(), qname.getNamespaceURI());
    }

    public void close() {
    }
    
    private static String getUniquePrefix(Element el) {
        int n = 1;

        while (true) {
            String nsPrefix = "ns" + n;

            if (el.getNamespace(nsPrefix) == null) {
                return nsPrefix;
            }

            n++;
        }
    }
    
    private static String getUniquePrefix(Element element, String namespaceURI) {
        String prefix = JDOMNamespaceContext.rawGetPrefix(element, namespaceURI);

        // it is OK to have both namespace URI and prefix be empty. 
        if (prefix == null) {
            if ("".equals(namespaceURI)) {
                return "";
            }
            prefix = getUniquePrefix(element);
            element.addNamespaceDeclaration(Namespace.getNamespace(prefix, namespaceURI));
        }
        return prefix;
    }
}
