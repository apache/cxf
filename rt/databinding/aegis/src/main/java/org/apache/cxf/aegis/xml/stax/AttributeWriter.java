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

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.aegis.xml.AbstractMessageWriter;
import org.apache.cxf.aegis.xml.MessageWriter;

public class AttributeWriter extends AbstractMessageWriter {
    private XMLStreamWriter writer;
    private String namespace;
    private String name;
    private String prefix;

    public AttributeWriter(XMLStreamWriter writer, String name, String namespace) {
        this.writer = writer;
        this.name = name;
        if (namespace == null) {
            namespace = ""; // otherwise, sjsxp is unhappy.
        }
        this.namespace = namespace;

        try {
            if (namespace != null && namespace.length() > 0) {
                if (XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(namespace)) {
                    prefix = NamespaceHelper.getUniquePrefix(writer, namespace, "xsi", true);
                } else {
                    prefix = NamespaceHelper.getUniquePrefix(writer, namespace, null, true);   
                }
            } else {
                prefix = "";
            }
        } catch (XMLStreamException e) {
            throw new DatabindingException("Couldn't write to stream.");
        }
    }

    public void writeValue(Object value) {
        try {
            writer.writeAttribute(prefix, namespace, name, value.toString());
        } catch (XMLStreamException e) {
            throw new DatabindingException("Error writing document.", e);
        }
    }

    public MessageWriter getAttributeWriter(String nm) {
        throw new IllegalStateException();
    }

    public MessageWriter getAttributeWriter(String nm, String ns) {
        throw new IllegalStateException();
    }

    public MessageWriter getAttributeWriter(QName qname) {
        throw new IllegalStateException();
    }

    public MessageWriter getElementWriter(String nm) {
        throw new IllegalStateException();
    }

    public MessageWriter getElementWriter(String nm, String ns) {
        throw new IllegalStateException();
    }

    public MessageWriter getElementWriter(QName qname) {
        throw new IllegalStateException();
    }

    public String getPrefixForNamespace(String ns) {
        throw new IllegalStateException();
    }

    public String getPrefixForNamespace(String ns, String hint) {
        throw new IllegalStateException();
    }

    public void close() {
    }
}
