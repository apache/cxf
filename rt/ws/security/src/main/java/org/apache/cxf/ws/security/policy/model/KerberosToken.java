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

public class KerberosToken extends Token {
    private boolean requireKeyIdentifierReference;
    private boolean useV5ApReqToken11;
    private boolean useGssV5ApReqToken11;

    public KerberosToken(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the requireKeyIdentifierReference.
     */
    public boolean isRequireKeyIdentifierReference() {
        return requireKeyIdentifierReference;
    }

    /**
     * @param requireKeyIdentifierReference The requireKeyIdentifierReference to set.
     */
    public void setRequireKeyIdentifierReference(boolean requireKeyIdentifierReference) {
        this.requireKeyIdentifierReference = requireKeyIdentifierReference;
    }
    
    public boolean isV5ApReqToken11() {
        return useV5ApReqToken11;
    }

    public void setV5ApReqToken11(boolean v5ApReqToken11) {
        this.useV5ApReqToken11 = v5ApReqToken11;
    }

    public boolean isGssV5ApReqToken11() {
        return useGssV5ApReqToken11;
    }

    public void setGssV5ApReqToken11(boolean gssV5ApReqToken11) {
        this.useGssV5ApReqToken11 = gssV5ApReqToken11;
    }
    
    public QName getName() {
        return SP12Constants.INSTANCE.getKerberosToken();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        QName name = constants.getSamlToken();
        String localname = name.getLocalPart();
        String namespaceURI = name.getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = name.getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:KerberosToken
        writer.writeStartElement(prefix, localname, namespaceURI);

        writer.writeNamespace(prefix, namespaceURI);

        String inclusion;

        inclusion = constants.getAttributeValueFromInclusion(getInclusion());

        if (inclusion != null) {
            writer.writeAttribute(prefix, namespaceURI, SPConstants.ATTR_INCLUDE_TOKEN, inclusion);
        }

        String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (pPrefix == null) {
            pPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(pPrefix, SPConstants.POLICY.getLocalPart(), SPConstants.POLICY
                                 .getNamespaceURI());

        if (isRequireKeyIdentifierReference()) {
            // <sp:RequireKeyIdentifierReference />
            writer.writeStartElement(prefix, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE, namespaceURI);
            writer.writeEndElement();
        }

        if (isV5ApReqToken11()) {
            // <sp:WssKerberosV5ApReqToken11 />
            writer.writeStartElement(prefix, SPConstants.KERBEROS_V5_AP_REQ_TOKEN_11, namespaceURI);
            writer.writeEndElement();
        } else if (isGssV5ApReqToken11()) {
            // <sp:WssGssKerberosV5ApReqToken11 />
            writer.writeStartElement(prefix, SPConstants.KERBEROS_GSS_V5_AP_REQ_TOKEN_11, namespaceURI);
            writer.writeEndElement();
        }
        
        if (isDerivedKeys()) {
            // <sp:RequireDerivedKeys />
            writer.writeStartElement(prefix, SPConstants.REQUIRE_DERIVED_KEYS, namespaceURI);
            writer.writeEndElement();
        }

        // </wsp:Policy>
        writer.writeEndElement();


        writer.writeEndElement();
        // </sp:KerberosToken>

    }
}
