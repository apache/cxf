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

import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;

public class SymmetricBinding extends SymmetricAsymmetricBindingBase {

    private EncryptionToken encryptionToken;
    private SignatureToken signatureToken;
    private ProtectionToken protectionToken;

    public SymmetricBinding() {
        super(SP12Constants.INSTANCE);
    }
    public SymmetricBinding(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the encryptionToken.
     */
    public EncryptionToken getEncryptionToken() {
        return encryptionToken;
    }

    /**
     * @param encryptionToken The encryptionToken to set.
     */
    public void setEncryptionToken(EncryptionToken encryptionToken) {
        if (this.protectionToken != null) {
            // throw new WSSPolicyException("Cannot use an EncryptionToken in a " +
            // "SymmetricBinding when there is a ProtectionToken");
        }
        this.encryptionToken = encryptionToken;
    }

    /**
     * @return Returns the protectionToken.
     */
    public ProtectionToken getProtectionToken() {
        return protectionToken;
    }

    /**
     * @param protectionToken The protectionToken to set.
     */
    public void setProtectionToken(ProtectionToken protectionToken) {
        if (this.encryptionToken != null || this.signatureToken != null) {
            // throw new WSSPolicyException("Cannot use a ProtectionToken in a " +
            // "SymmetricBinding when there is a SignatureToken or an" +
            // "EncryptionToken");
        }
        this.protectionToken = protectionToken;
    }

    /**
     * @return Returns the signatureToken.
     */
    public SignatureToken getSignatureToken() {
        return signatureToken;
    }

    /**
     * @param signatureToken The signatureToken to set.
     */
    public void setSignatureToken(SignatureToken signatureToken) {
        if (this.protectionToken != null) {
            // throw new WSSPolicyException("Cannot use a SignatureToken in a " +
            // "SymmetricBinding when there is a ProtectionToken");
        }
        this.signatureToken = signatureToken;
    }

    public QName getRealName() {
        return constants.getSymmetricBinding();
    }
    public QName getName() {
        return SP12Constants.INSTANCE.getSymmetricBinding();
    }
    public PolicyComponent normalize() {
        All all = new All();
        all.addPolicyComponent(getPolicy().getFirstPolicyComponent());
        all.addPolicyComponent(this);
        return all;
    }

    public Policy getPolicy() {
        Policy p = new Policy();
        ExactlyOne ea = new ExactlyOne();
        p.addPolicyComponent(ea);
        All all = new All();
        
        if (this.getProtectionToken() != null) {
            all.addPolicyComponent(this.getProtectionToken());
        }
        if (this.getSignatureToken() != null) {
            all.addPolicyComponent(this.getSignatureToken());
        }
        if (this.getEncryptionToken() != null) {
            all.addPolicyComponent(this.getEncryptionToken());
        }
        if (isIncludeTimestamp()) {
            all.addPolicyComponent(new PrimitiveAssertion(SP12Constants.INCLUDE_TIMESTAMP));
        }
        if (getLayout() != null) {
            all.addPolicyComponent(getLayout());
        }

        
        ea.addPolicyComponent(all);
        PolicyComponent pc = p.normalize(true);
        if (pc instanceof Policy) {
            return (Policy)pc;
        } else {
            p = new Policy();
            p.addPolicyComponent(pc);
            return p;
        }
    }
    
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localname = getRealName().getLocalPart();
        String namespaceURI = getRealName().getNamespaceURI();

        String prefix;
        String writerPrefix = writer.getPrefix(namespaceURI);

        if (writerPrefix == null) {
            prefix = getRealName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        } else {
            prefix = writerPrefix;
        }

        // <sp:SymmetricBinding>
        writer.writeStartElement(prefix, localname, namespaceURI);

        // xmlns:sp=".."
        writer.writeNamespace(prefix, namespaceURI);

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

        if (encryptionToken != null) {
            encryptionToken.serialize(writer);

        } else if (protectionToken != null) {
            protectionToken.serialize(writer);

        } else {
            throw new RuntimeException("Either EncryptionToken or ProtectionToken must be set");
        }

        AlgorithmSuite algorithmSuite = getAlgorithmSuite();

        if (algorithmSuite == null) {
            throw new RuntimeException("AlgorithmSuite must be set");
        }
        // <sp:AlgorithmSuite />
        algorithmSuite.serialize(writer);

        Layout layout = getLayout();
        if (layout != null) {
            // <sp:Layout />
            layout.serialize(writer);
        }

        if (isIncludeTimestamp()) {
            // <sp:IncludeTimestamp />
            writer.writeStartElement(prefix, SPConstants.INCLUDE_TIMESTAMP, namespaceURI);
            writer.writeEndElement();
        }

        if (SPConstants.ProtectionOrder.EncryptBeforeSigning == getProtectionOrder()) {
            // <sp:EncryptBeforeSigning />
            writer.writeStartElement(prefix, SPConstants.ENCRYPT_BEFORE_SIGNING, namespaceURI);
            writer.writeEndElement();
        }

        if (isSignatureProtection()) {
            // <sp:EncryptSignature />
            writer.writeStartElement(prefix, SPConstants.ENCRYPT_SIGNATURE, namespaceURI);
            writer.writeEndElement();
        }

        if (isEntireHeadersAndBodySignatures()) {
            writer.writeEmptyElement(prefix, SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY, namespaceURI);
        }
        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:SymmetricBinding>
        writer.writeEndElement();

    }
}
