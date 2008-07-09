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

public class Wss10 extends AbstractSecurityAssertion {

    private boolean mustSupportRefKeyIdentifier;
    private boolean mustSupportRefIssuerSerial;
    private boolean mustSupportRefExternalURI;
    private boolean mustSupportRefEmbeddedToken;

    public Wss10(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the mustSupportRefEmbeddedToken.
     */
    public boolean isMustSupportRefEmbeddedToken() {
        return mustSupportRefEmbeddedToken;
    }

    /**
     * @param mustSupportRefEmbeddedToken The mustSupportRefEmbeddedToken to set.
     */
    public void setMustSupportRefEmbeddedToken(boolean mustSupportRefEmbeddedToken) {
        this.mustSupportRefEmbeddedToken = mustSupportRefEmbeddedToken;
    }

    /**
     * @return Returns the mustSupportRefExternalURI.
     */
    public boolean isMustSupportRefExternalURI() {
        return mustSupportRefExternalURI;
    }

    /**
     * @param mustSupportRefExternalURI The mustSupportRefExternalURI to set.
     */
    public void setMustSupportRefExternalURI(boolean mustSupportRefExternalURI) {
        this.mustSupportRefExternalURI = mustSupportRefExternalURI;
    }

    /**
     * @return Returns the mustSupportRefIssuerSerial.
     */
    public boolean isMustSupportRefIssuerSerial() {
        return mustSupportRefIssuerSerial;
    }

    /**
     * @param mustSupportRefIssuerSerial The mustSupportRefIssuerSerial to set.
     */
    public void setMustSupportRefIssuerSerial(boolean mustSupportRefIssuerSerial) {
        this.mustSupportRefIssuerSerial = mustSupportRefIssuerSerial;
    }

    /**
     * @return Returns the mustSupportRefKeyIdentifier.
     */
    public boolean isMustSupportRefKeyIdentifier() {
        return mustSupportRefKeyIdentifier;
    }

    /**
     * @param mustSupportRefKeyIdentifier The mustSupportRefKeyIdentifier to set.
     */
    public void setMustSupportRefKeyIdentifier(boolean mustSupportRefKeyIdentifier) {
        this.mustSupportRefKeyIdentifier = mustSupportRefKeyIdentifier;
    }

    public QName getName() {
        return constants.getWSS10();
    }

    public PolicyComponent normalize() {
        return this;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localname = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:Wss10>
        writer.writeStartElement(prefix, localname, namespaceURI);

        // xmlns:sp=".."
        writer.writeNamespace(prefix, namespaceURI);

        String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (pPrefix == null) {
            writer.setPrefix(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(prefix, SPConstants.POLICY.getLocalPart(), SPConstants.POLICY
            .getNamespaceURI());

        if (isMustSupportRefKeyIdentifier()) {
            // <sp:MustSupportRefKeyIdentifier />
            writer.writeStartElement(prefix, SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER, namespaceURI);
            writer.writeEndElement();
        }

        if (isMustSupportRefIssuerSerial()) {
            // <sp:MustSupportRefIssuerSerial />
            writer.writeStartElement(prefix, SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL, namespaceURI);
            writer.writeEndElement();
        }

        if (isMustSupportRefExternalURI()) {
            // <sp:MustSupportRefExternalURI />
            writer.writeStartElement(prefix, SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI, namespaceURI);
            writer.writeEndElement();
        }

        if (isMustSupportRefEmbeddedToken()) {
            // <sp:MustSupportRefEmbeddedToken />
            writer.writeStartElement(prefix, SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN, namespaceURI);
            writer.writeEndElement();

        }

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:Wss10>
        writer.writeEndElement();

    }
}
