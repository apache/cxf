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

import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;

public class Layout extends AbstractSecurityAssertion {
    private SPConstants.Layout value = SPConstants.Layout.Lax;

    public Layout(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the value.
     */
    public SPConstants.Layout getValue() {
        return value;
    }

    /**
     * @param value The value to set.
     */
    public void setValue(SPConstants.Layout value) {
        this.value = value;
    }

    public QName getRealName() {
        return constants.getLayout();
    }
    public QName getName() {
        return SP12Constants.INSTANCE.getLayout();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localName = getRealName().getLocalPart();
        String namespaceURI = getRealName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getRealName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:Layout>
        writer.writeStartElement(prefix, localName, namespaceURI);
        String wspPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (wspPrefix == null) {
            wspPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(wspPrefix, SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(wspPrefix, SPConstants.POLICY.getLocalPart(),
                                 SPConstants.POLICY.getNamespaceURI());

        // .. <sp:Strict /> | <sp:Lax /> | <sp:LaxTsFirst /> | <sp:LaxTsLast /> ..
        writer.writeEmptyElement(prefix, value.name(), namespaceURI);

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:Layout>
        writer.writeEndElement();
    }
}
