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
package org.apache.cxf.ws.security.policy.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.neethi.PolicyComponent;

public class RequiredParts extends AbstractSecurityAssertion {

    private List<Header> headers = new ArrayList<Header>();

    public RequiredParts(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the headers.
     */
    public List<Header> getHeaders() {
        return this.headers;
    }

    /**
     * @param headers The headers to set.
     */
    public void addHeader(Header header) {
        this.headers.add(header);
    }

    public QName getName() {
        return SP12Constants.REQUIRED_PARTS;
    }

    public PolicyComponent normalize() {
        return this;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localName = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:RequiredParts>
        writer.writeStartElement(prefix, localName, namespaceURI);

        // xmlns:sp=".."
        writer.writeNamespace(prefix, namespaceURI);

        Header header;
        for (Iterator iterator = headers.iterator(); iterator.hasNext();) {
            header = (Header)iterator.next();
            // <sp:Header Name=".." Namespace=".." />
            writer.writeStartElement(prefix, SPConstants.HEADER, namespaceURI);
            // Name attribute is optional
            if (header.getName() != null) {
                writer.writeAttribute("Name", header.getName());
            }
            writer.writeAttribute("Namespace", header.getNamespace());

            writer.writeEndElement();
        }

        // </sp:RequiredParts>
        writer.writeEndElement();
    }

}
