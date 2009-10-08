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

public class TransportToken extends AbstractSecurityAssertion implements TokenWrapper {

    private Token transportToken;

    public TransportToken(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the transportToken.
     */
    public Token getTransportToken() {
        return transportToken;
    }

    public QName getName() {
        return constants.getTransportToken();
    }

    public boolean isOptional() {
        throw new UnsupportedOperationException();
    }

    public PolicyComponent normalize() {
        throw new UnsupportedOperationException();
    }

    public short getType() {
        return org.apache.neethi.Constants.TYPE_ASSERTION;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localName = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:TransportToken>

        writer.writeStartElement(prefix, localName, namespaceURI);

        String wspPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (wspPrefix == null) {
            wspPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(wspPrefix, SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getLocalPart(),
                                 SPConstants.POLICY.getNamespaceURI());

        // serialization of the token ..
        if (transportToken != null) {
            transportToken.serialize(writer);
        }

        // </wsp:Policy>
        writer.writeEndElement();

        writer.writeEndElement();
        // </sp:TransportToken>
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.cxf.ws.security.policy.model.TokenWrapper#setToken(org.apache.cxf.ws.security.policy.model
     * .Token)
     */
    public void setToken(Token tok) {
        this.transportToken = tok;
    }

}
