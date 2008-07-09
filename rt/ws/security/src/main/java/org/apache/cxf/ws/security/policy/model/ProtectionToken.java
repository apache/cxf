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

public class ProtectionToken extends AbstractSecurityAssertion implements TokenWrapper {

    private Token protectionToken;

    public ProtectionToken(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the protectionToken.
     */
    public Token getProtectionToken() {
        return protectionToken;
    }

    /**
     * @param protectionToken The protectionToken to set.
     */
    public void setProtectionToken(Token protectionToken) {
        this.protectionToken = protectionToken;
    }

    public void setToken(Token tok) {
        this.setProtectionToken(tok);
    }

    public QName getName() {
        return constants.getProtectionToken();
    }

    public PolicyComponent normalize() {
        /*
         * ProtectionToken can not contain multiple values. Hence we consider it to always be in the
         * normalized format.
         */
        return this;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localname = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix;

        String writerPrefix = writer.getPrefix(namespaceURI);
        if (writerPrefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);

        } else {
            prefix = writerPrefix;
        }

        // <sp:ProtectionToken>
        writer.writeStartElement(prefix, localname, namespaceURI);

        if (writerPrefix == null) {
            // xmlns:sp=".."
            writer.writeNamespace(prefix, namespaceURI);
        }

        String policyLocalName = SPConstants.POLICY.getLocalPart();
        String policyNamespaceURI = SPConstants.POLICY.getNamespaceURI();

        String wspPrefix;

        String wspWriterPrefix = writer.getPrefix(policyNamespaceURI);

        if (wspWriterPrefix == null) {
            wspPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(wspPrefix, policyNamespaceURI);
        } else {
            wspPrefix = wspWriterPrefix;
        }

        // <wsp:Policy>
        writer.writeStartElement(wspPrefix, policyLocalName, policyNamespaceURI);

        if (wspWriterPrefix == null) {
            // xmlns:wsp=".."
            writer.writeNamespace(wspPrefix, policyNamespaceURI);
        }

        if (protectionToken == null) {
            throw new RuntimeException("ProtectionToken is not set");
        }

        protectionToken.serialize(writer);

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:ProtectionToken>
        writer.writeEndElement();
    }
}
