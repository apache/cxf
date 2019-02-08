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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;

public class IgnoreNamespacesWriter extends DelegatingXMLStreamWriter {
    private static final String XSI_PREFIX = "xsi";
    private boolean ignoreXsiAttributes;
    public IgnoreNamespacesWriter(XMLStreamWriter writer) {
        this(writer, false);
    }
    public IgnoreNamespacesWriter(XMLStreamWriter writer, boolean ignoreXsiAttributes) {
        super(writer);
        this.ignoreXsiAttributes = ignoreXsiAttributes;
    }

    public void writeAttribute(String prefix, String uri,
                               String local, String value) throws XMLStreamException {
        if (ignoreXsiAttributes && XSI_PREFIX.equals(prefix)
            && ("type".equals(local) || "nil".equals(local))) {
            return;
        }
        super.writeAttribute(local, value);
    }

    public void writeAttribute(String uri, String local, String value) throws XMLStreamException {
        super.writeAttribute(local, value);
    }

    public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
        super.writeStartElement(local);
    }

    public void writeStartElement(String uri, String local) throws XMLStreamException {
        super.writeStartElement(local);
    }

    public void setPrefix(String pfx, String uri) throws XMLStreamException {
        // completed
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        // completed
    }
}
