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
package org.apache.cxf.aegis.xml.stax;

import java.io.OutputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.aegis.xml.AbstractMessageWriter;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.common.util.StringUtils;

public class ElementWriter extends AbstractMessageWriter implements MessageWriter {
    private XMLStreamWriter writer;

    private String namespace;
    private String name;
    private String prefix;

    /**
     * Create an ElementWriter but without writing an element name.
     * 
     * @param writer
     */
    public ElementWriter(XMLStreamWriter writer) {
        this.writer = writer;
    }

    public ElementWriter(XMLStreamWriter writer, String name, String namespace) {
        this(writer, name, namespace, null);
    }

    public ElementWriter(XMLStreamWriter streamWriter, QName name) {
        this(streamWriter, name.getLocalPart(), name.getNamespaceURI());
    }

    public ElementWriter(XMLStreamWriter writer, String name, String namespace, String prefix) {
        this.writer = writer;
        this.namespace = namespace;
        this.name = name;
        this.prefix = prefix;

        try {
            writeStartElement();
        } catch (XMLStreamException e) {
            throw new DatabindingException("Error writing document.", e);
        }
    }

    /**
     * @param os
     * @throws XMLStreamException
     */
    public ElementWriter(OutputStream os, String name, String namespace) throws XMLStreamException {
        XMLOutputFactory ofactory = XMLOutputFactory.newInstance();
        this.writer = ofactory.createXMLStreamWriter(os);

        this.namespace = namespace;
        this.name = name;

        try {
            writeStartElement();
        } catch (XMLStreamException e) {
            throw new DatabindingException("Error writing document.", e);
        }
    }

    private void writeStartElement() throws XMLStreamException {
        if (!StringUtils.isEmpty(namespace)) {
            boolean declare = false;
            // Did the user declare a prefix?
            String decPrefix = writer.getNamespaceContext().getPrefix(namespace);

            // If the user didn't specify a prefix, create one
            if (StringUtils.isEmpty(prefix) 
                && decPrefix == null) {
               
                if (!StringUtils.isEmpty(namespace)) {
                    declare = true;
                    prefix = NamespaceHelper.getUniquePrefix(writer);
                } else {
                    prefix = "";
                    if (!StringUtils.isEmpty(writer.getNamespaceContext().getNamespaceURI(""))) {
                        declare = true;
                    }
                }
            } else if (StringUtils.isEmpty(prefix)) {
                prefix = decPrefix;
            } else if (!prefix.equals(decPrefix)) {
                declare = true;
            }

            writer.writeStartElement(prefix, name, namespace);

            if (declare) {
                writer.setPrefix(prefix, namespace);
                writer.writeNamespace(prefix, namespace);
            }
        } else {
            writer.writeStartElement(name);
        }
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageWriter#writeValue(java.lang.Object)
     */
    public void writeValue(Object value) {
        try {
            if (value != null) {
                writer.writeCharacters(value.toString());
            }
        } catch (XMLStreamException e) {
            throw new DatabindingException("Error writing document.", e);
        }
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageWriter#getWriter(java.lang.String)
     */
    public MessageWriter getElementWriter(String nm) {
        return new ElementWriter(writer, nm, namespace);
    }

    public MessageWriter getElementWriter(String nm, String ns) {
        return new ElementWriter(writer, nm, ns);
    }

    public MessageWriter getElementWriter(QName qname) {
        /*
         * No one really wants xmlns= in their XML, prefixes are preferred.
         * If the input qname has no prefix, go ahead and use the constructor that will
         * generate one.
         */
        if ("".equals(qname.getPrefix())) {
            return new ElementWriter(writer, qname.getLocalPart(), qname.getNamespaceURI());
        } else {
            return new ElementWriter(writer, qname.getLocalPart(), 
                                     qname.getNamespaceURI(), 
                                     qname.getPrefix());
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public void close() {
        try {
            writer.writeEndElement();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new DatabindingException("Error writing document.", e);
        }
    }

    public void flush() throws XMLStreamException {
        writer.flush();
    }

    public XMLStreamWriter getXMLStreamWriter() {
        return writer;
    }

    public MessageWriter getAttributeWriter(String nm) {
        return new AttributeWriter(writer, nm, namespace);
    }

    public MessageWriter getAttributeWriter(String nm, String ns) {
        return new AttributeWriter(writer, nm, ns);
    }

    public MessageWriter getAttributeWriter(QName qname) {
        return new AttributeWriter(writer, qname.getLocalPart(), qname.getNamespaceURI());
    }

    public String getPrefixForNamespace(String ns) {
        try {
            String pfx = writer.getPrefix(ns);

            if (pfx == null) {
                pfx = NamespaceHelper.getUniquePrefix(writer);

                writer.setPrefix(pfx, ns);
                writer.writeNamespace(pfx, ns);
            }

            return prefix;
        } catch (XMLStreamException e) {
            throw new DatabindingException("Error writing document.", e);
        }
    }

    public String getPrefixForNamespace(String ns, String hint) {
        try {
            String pfx = writer.getPrefix(ns);
            String contextPfx = writer.getNamespaceContext().getPrefix(ns);

            if (pfx == null) {
                String ns2 = writer.getNamespaceContext().getNamespaceURI(hint);
                // if the hint is "" (the default) and the context does 
                if (ns2 == null && !"".equals(hint)) { 
                    pfx = hint;
                } else if (ns.equals(ns2)) {
                    // just because it's in the context, doesn't mean it has been written.
                    pfx = hint;
                } else if (contextPfx != null) {
                    pfx = contextPfx;
                } else {
                    pfx = NamespaceHelper.getUniquePrefix(writer);
                }

                writer.setPrefix(pfx, ns);
                writer.writeNamespace(pfx, ns);
            }

            return pfx;
        } catch (XMLStreamException e) {
            throw new DatabindingException("Error writing document.", e);
        }
    }
}
