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

public class SamlToken extends Token {
    private boolean useSamlVersion11Profile10;
    private boolean useSamlVersion11Profile11;
    private boolean useSamlVersion20Profile11;
    private boolean requireKeyIdentifierReference;

    public SamlToken(SPConstants version) {
        super(version);
    }

    public boolean isUseSamlVersion11Profile10() {
        return useSamlVersion11Profile10;
    }

    public void setUseSamlVersion11Profile10(boolean useSamlVersion11Profile10) {
        this.useSamlVersion11Profile10 = useSamlVersion11Profile10;
    }
    
    public boolean isUseSamlVersion11Profile11() {
        return useSamlVersion11Profile11;
    }

    public void setUseSamlVersion11Profile11(boolean useSamlVersion11Profile11) {
        this.useSamlVersion11Profile11 = useSamlVersion11Profile11;
    }
    
    public boolean isUseSamlVersion20Profile11() {
        return useSamlVersion20Profile11;
    }

    public void setUseSamlVersion20Profile11(boolean useSamlVersion20Profile11) {
        this.useSamlVersion20Profile11 = useSamlVersion20Profile11;
    }
    
    public boolean isRequireKeyIdentifierReference() {
        return requireKeyIdentifierReference;
    }

    public void setRequireKeyIdentifierReference(boolean requireKeyIdentifierReference) {
        this.requireKeyIdentifierReference = requireKeyIdentifierReference;
    }
    
    public QName getName() {
        return SP12Constants.INSTANCE.getSamlToken();
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

        // <sp:SamlToken
        writer.writeStartElement(prefix, localname, namespaceURI);

        writer.writeNamespace(prefix, namespaceURI);

        String inclusion;

        inclusion = constants.getAttributeValueFromInclusion(getInclusion());

        if (inclusion != null) {
            writer.writeAttribute(prefix, namespaceURI, SPConstants.ATTR_INCLUDE_TOKEN, inclusion);
        }

        if (isUseSamlVersion11Profile10() || isUseSamlVersion11Profile11()
            || isUseSamlVersion20Profile11()) {
            String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
            if (pPrefix == null) {
                pPrefix = SPConstants.POLICY.getPrefix();
                writer.setPrefix(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getNamespaceURI());
            }

            // <wsp:Policy>
            writer.writeStartElement(pPrefix, SPConstants.POLICY.getLocalPart(), SPConstants.POLICY
                .getNamespaceURI());

            // CHECKME
            if (isUseSamlVersion11Profile10()) {
                // <sp:WssSamlV11Token10 />
                writer.writeStartElement(prefix, SPConstants.SAML_11_TOKEN_10, namespaceURI);
            } else if (isUseSamlVersion11Profile11()) {
                // <sp:WssSamlV11Token11 />
                writer.writeStartElement(prefix, SPConstants.SAML_11_TOKEN_11, namespaceURI);
            } else {
                // <sp:WssSamlV20Token11 />
                writer.writeStartElement(prefix, SPConstants.SAML_20_TOKEN_11, namespaceURI);
            }
            
            if (isDerivedKeys()) {
                writer.writeStartElement(prefix, SPConstants.REQUIRE_DERIVED_KEYS, namespaceURI);
                writer.writeEndElement();
            } else if (isExplicitDerivedKeys()) {
                writer.writeStartElement(prefix, SPConstants.REQUIRE_EXPLICIT_DERIVED_KEYS, namespaceURI);
                writer.writeEndElement();
            } else if (isImpliedDerivedKeys()) {
                writer.writeStartElement(prefix, SPConstants.REQUIRE_IMPLIED_DERIVED_KEYS, namespaceURI);
                writer.writeEndElement();
            }
            
            if (isRequireKeyIdentifierReference()) {
                writer.writeStartElement(prefix, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE, namespaceURI);
                writer.writeEndElement();
            }

            writer.writeEndElement();

            // </wsp:Policy>
            writer.writeEndElement();

        }

        writer.writeEndElement();
        // </sp:SamlToken>

    }
}
