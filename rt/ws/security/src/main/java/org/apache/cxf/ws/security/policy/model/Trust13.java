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

/**
 * Model bean to capture Trust10 assertion info
 */
public class Trust13 extends AbstractSecurityAssertion {

    private boolean mustSupportClientChallenge;
    private boolean mustSupportServerChallenge;
    private boolean requireClientEntropy;
    private boolean requireServerEntropy;
    private boolean mustSupportIssuedTokens;
    private boolean requireRequestSecurityTokenCollection;
    private boolean requireAppliesTo;

    public Trust13(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the mustSupportClientChallenge.
     */
    public boolean isMustSupportClientChallenge() {
        return mustSupportClientChallenge;
    }

    /**
     * @param mustSupportClientChallenge The mustSupportClientChallenge to set.
     */
    public void setMustSupportClientChallenge(boolean mustSupportClientChallenge) {
        this.mustSupportClientChallenge = mustSupportClientChallenge;
    }

    /**
     * @return Returns the mustSupportIssuedTokens.
     */
    public boolean isMustSupportIssuedTokens() {
        return mustSupportIssuedTokens;
    }

    /**
     * @param mustSupportIssuedTokens The mustSupportIssuedTokens to set.
     */
    public void setMustSupportIssuedTokens(boolean mustSupportIssuedTokens) {
        this.mustSupportIssuedTokens = mustSupportIssuedTokens;
    }

    /**
     * @return Returns the mustSupportServerChallenge.
     */
    public boolean isMustSupportServerChallenge() {
        return mustSupportServerChallenge;
    }

    /**
     * @param mustSupportServerChallenge The mustSupportServerChallenge to set.
     */
    public void setMustSupportServerChallenge(boolean mustSupportServerChallenge) {
        this.mustSupportServerChallenge = mustSupportServerChallenge;
    }

    /**
     * @return Returns the requireClientEntropy.
     */
    public boolean isRequireClientEntropy() {
        return requireClientEntropy;
    }

    /**
     * @param requireClientEntropy The requireClientEntropy to set.
     */
    public void setRequireClientEntropy(boolean requireClientEntropy) {
        this.requireClientEntropy = requireClientEntropy;
    }

    /**
     * @return Returns the requireServerEntropy.
     */
    public boolean isRequireServerEntropy() {
        return requireServerEntropy;
    }

    /**
     * @param requireServerEntropy The requireServerEntropy to set.
     */
    public void setRequireServerEntropy(boolean requireServerEntropy) {
        this.requireServerEntropy = requireServerEntropy;
    }

    /**
     * @return Returns the requireRequestSecurityTokenCollection.
     */
    public boolean isRequireRequestSecurityTokenCollection() {
        return requireRequestSecurityTokenCollection;
    }

    /**
     * @param requireRequestSecurityTokenCollection The requireRequestSecurityTokenCollection to set.
     */
    public void setRequireRequestSecurityTokenCollection(boolean requireRequestSecurityTokenCollection) {
        this.requireRequestSecurityTokenCollection = requireRequestSecurityTokenCollection;
    }

    /**
     * @return Returns the requireAppliesTo.
     */
    public boolean isRequireAppliesTo() {
        return requireAppliesTo;
    }

    /**
     * @param requireAppliesTo The requireAppliesTo to set.
     */
    public void setRequireAppliesTo(boolean requireAppliesTo) {
        this.requireAppliesTo = requireAppliesTo;
    }

    public QName getRealName() {
        return SP12Constants.TRUST_13;
    }
    public QName getName() {
        return SP12Constants.TRUST_13;
    }


    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localname = getRealName().getLocalPart();
        String namespaceURI = getRealName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = getRealName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:Trust13>
        writer.writeStartElement(prefix, localname, namespaceURI);
        // xmlns:sp=".."
        writer.writeNamespace(prefix, namespaceURI);

        String wspPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());

        if (wspPrefix == null) {
            wspPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(wspPrefix, SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getLocalPart(),
                                 SPConstants.POLICY.getNamespaceURI());

        if (isMustSupportClientChallenge()) {
            // <sp:MustSupportClientChallenge />
            writer.writeStartElement(prefix, SPConstants.MUST_SUPPORT_CLIENT_CHALLENGE, namespaceURI);
            writer.writeEndElement();
        }

        if (isMustSupportServerChallenge()) {
            // <sp:MustSupportServerChallenge />
            writer.writeStartElement(prefix, SPConstants.MUST_SUPPORT_SERVER_CHALLENGE, namespaceURI);
            writer.writeEndElement();
        }

        if (isRequireClientEntropy()) {
            // <sp:RequireClientEntropy />
            writer.writeStartElement(prefix, SPConstants.REQUIRE_CLIENT_ENTROPY, namespaceURI);
            writer.writeEndElement();
        }

        if (isRequireServerEntropy()) {
            // <sp:RequireServerEntropy />
            writer.writeStartElement(prefix, SPConstants.REQUIRE_SERVER_ENTROPY, namespaceURI);
            writer.writeEndElement();
        }

        if (isMustSupportIssuedTokens()) {
            // <sp:MustSupportIssuedTokens />
            writer.writeStartElement(prefix, SPConstants.MUST_SUPPORT_ISSUED_TOKENS, namespaceURI);
            writer.writeEndElement();
        }

        if (isRequireRequestSecurityTokenCollection()) {
            // <sp:RequireRequestSecurityTokenCollection />
            writer.writeStartElement(prefix, SPConstants.REQUIRE_REQUEST_SECURITY_TOKEN_COLLECTION,
                                     namespaceURI);
            writer.writeEndElement();
        }

        if (isRequireAppliesTo()) {
            // <sp:RequireAppliesTo />
            writer.writeStartElement(prefix, SPConstants.REQUIRE_APPLIES_TO, namespaceURI);
            writer.writeEndElement();
        }

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:Trust13>
        writer.writeEndElement();

    }

    public short getType() {
        return org.apache.neethi.Constants.TYPE_ASSERTION;
    }

}
