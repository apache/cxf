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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.neethi.PolicyComponent;

public class Layout extends AbstractSecurityAssertion {

    private String value = SPConstants.LAYOUT_LAX;

    public Layout(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value The value to set.
     */
    public void setValue(String value) {
        if (SPConstants.LAYOUT_LAX.equals(value) || SPConstants.LAYOUT_STRICT.equals(value)
            || SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST.equals(value)
            || SPConstants.LAYOUT_LAX_TIMESTAMP_LAST.equals(value)) {
            this.value = value;
        } else {
            // throw new WSSPolicyException("Incorrect layout value : " +
            // value);
        }
    }

    public QName getName() {
        return constants.getLayout();
    }

    public PolicyComponent normalize() {
        throw new UnsupportedOperationException();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localName = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:Layout>
        writer.writeStartElement(prefix, localName, namespaceURI);

        // <wsp:Policy>
        writer.writeStartElement(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getLocalPart(),
                                 SPConstants.POLICY.getNamespaceURI());

        // .. <sp:Strict /> | <sp:Lax /> | <sp:LaxTsFirst /> | <sp:LaxTsLast /> ..
        if (SPConstants.LAYOUT_STRICT.equals(value)) {
            writer.writeStartElement(prefix, SPConstants.LAYOUT_STRICT, namespaceURI);

        } else if (SPConstants.LAYOUT_LAX.equals(value)) {
            writer.writeStartElement(prefix, SPConstants.LAYOUT_LAX, namespaceURI);

        } else if (SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST.equals(value)) {
            writer.writeStartElement(prefix, SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST, namespaceURI);

        } else if (SPConstants.LAYOUT_LAX_TIMESTAMP_LAST.equals(value)) {
            writer.writeStartElement(prefix, SPConstants.LAYOUT_LAX_TIMESTAMP_LAST, namespaceURI);
        }

        writer.writeEndElement();

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:Layout>
        writer.writeEndElement();
    }
}
