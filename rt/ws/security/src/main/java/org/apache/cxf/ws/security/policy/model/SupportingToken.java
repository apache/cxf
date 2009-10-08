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
import org.apache.cxf.ws.security.policy.SPConstants.SupportTokenType;
import org.apache.neethi.PolicyComponent;

public class SupportingToken extends AbstractSecurityAssertion implements AlgorithmWrapper, TokenWrapper {

    /**
     * Type of SupportingToken
     * 
     * @see SupportingToken#SUPPORTING
     * @see SupportingToken#ENDORSING
     * @see SupportingToken#SIGNED
     * @see SupportingToken#SIGNED_ENDORSING
     */
    private SupportTokenType type;

    private AlgorithmSuite algorithmSuite;

    private List<Token> tokens = new ArrayList<Token>();

    private SignedEncryptedElements signedElements;

    private SignedEncryptedElements encryptedElements;

    private SignedEncryptedParts signedParts;

    private SignedEncryptedParts encryptedParts;

    public SupportingToken(SupportTokenType type, SPConstants version) {
        super(version);
        this.type = type;
    }

    /**
     * @return Returns the algorithmSuite.
     */
    public AlgorithmSuite getAlgorithmSuite() {
        return algorithmSuite;
    }

    /**
     * @param algorithmSuite The algorithmSuite to set.
     */
    public void setAlgorithmSuite(AlgorithmSuite algorithmSuite) {
        this.algorithmSuite = algorithmSuite;
    }

    /**
     * @return Returns the token.
     */
    public List<Token> getTokens() {
        return tokens;
    }

    /**
     * @param token The token to set.
     */
    public void addToken(Token token) {
        this.tokens.add(token);
    }

    /**
     * @return Returns the type.
     */
    public SupportTokenType getTokenType() {
        return type;
    }

    /**
     * @param type The type to set.
     */
    public void setTokenType(SupportTokenType t) {
        this.type = t;
    }

    /**
     * @return Returns the encryptedElements.
     */
    public SignedEncryptedElements getEncryptedElements() {
        return encryptedElements;
    }

    /**
     * @param encryptedElements The encryptedElements to set.
     */
    public void setEncryptedElements(SignedEncryptedElements encryptedElements) {
        this.encryptedElements = encryptedElements;
    }

    /**
     * @return Returns the encryptedParts.
     */
    public SignedEncryptedParts getEncryptedParts() {
        return encryptedParts;
    }

    /**
     * @param encryptedParts The encryptedParts to set.
     */
    public void setEncryptedParts(SignedEncryptedParts encryptedParts) {
        this.encryptedParts = encryptedParts;
    }

    /**
     * @return Returns the signedElements.
     */
    public SignedEncryptedElements getSignedElements() {
        return signedElements;
    }

    /**
     * @param signedElements The signedElements to set.
     */
    public void setSignedElements(SignedEncryptedElements signedElements) {
        this.signedElements = signedElements;
    }

    /**
     * @return Returns the signedParts.
     */
    public SignedEncryptedParts getSignedParts() {
        return signedParts;
    }

    /**
     * @param signedParts The signedParts to set.
     */
    public void setSignedParts(SignedEncryptedParts signedParts) {
        this.signedParts = signedParts;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.ws.security.policy.TokenWrapper#setToken(org.apache.ws.security.policy.Token)
     */
    public void setToken(Token tok) {
        this.addToken(tok);
    }

    public QName getName() {
        QName ret = null;
        
        switch (type) {
        case SUPPORTING_TOKEN_SUPPORTING:
            ret = constants.getSupportingTokens();
            break;
        case SUPPORTING_TOKEN_SIGNED:
            ret = constants.getSignedSupportingTokens();
            break;
        case SUPPORTING_TOKEN_ENDORSING:
            ret = constants.getEndorsingSupportingTokens();
            break;
        case SUPPORTING_TOKEN_SIGNED_ENDORSING:
            ret = constants.getSignedEndorsingSupportingTokens();
            break;
        case SUPPORTING_TOKEN_ENCRYPTED:
            ret = SP12Constants.ENCRYPTED_SUPPORTING_TOKENS;
            break;
        case SUPPORTING_TOKEN_SIGNED_ENCRYPTED:
            ret = SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS;
            break;
        case SUPPORTING_TOKEN_ENDORSING_ENCRYPTED:
            ret = SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS;
            break;
        case SUPPORTING_TOKEN_SIGNED_ENDORSING_ENCRYPTED:
            ret = SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS;
            break;
        default:
            ret = null;
            break;
        }
        return ret;
    }

    /**
     * @return true if the supporting token should be encrypted
     */

    public boolean isEncryptedToken() {
        return type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_ENCRYPTED
            || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_SIGNED_ENCRYPTED
            || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_ENDORSING_ENCRYPTED
            || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_SIGNED_ENDORSING_ENCRYPTED;
    }

    public PolicyComponent normalize() {
        return this;
    }

    public short getType() {
        return org.apache.neethi.Constants.TYPE_ASSERTION;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        String localname = getName().getLocalPart();

        // <sp:SupportingToken>
        writer.writeStartElement(prefix, localname, namespaceURI);

        // xmlns:sp=".."
        writer.writeNamespace(prefix, namespaceURI);

        String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (pPrefix == null) {
            pPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(pPrefix, SPConstants.POLICY.getNamespaceURI());
        }
        // <wsp:Policy>
        writer.writeStartElement(pPrefix, SPConstants.POLICY.getLocalPart(), SPConstants.POLICY
            .getNamespaceURI());

        Token token;
        for (Iterator iterator = getTokens().iterator(); iterator.hasNext();) {
            // [Token Assertion] +
            token = (Token)iterator.next();
            token.serialize(writer);
        }

        if (signedParts != null) {
            signedParts.serialize(writer);

        } else if (signedElements != null) {
            signedElements.serialize(writer);

        } else if (encryptedParts != null) {
            encryptedParts.serialize(writer);

        } else if (encryptedElements != null) {
            encryptedElements.serialize(writer);
        }
        // </wsp:Policy>
        writer.writeEndElement();

        writer.writeEndElement();
        // </sp:SupportingToken>
    }
}
