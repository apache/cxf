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

public class UsernameToken extends Token {
    private boolean useUTProfile10;
    private boolean useUTProfile11;
    private boolean noPassword;
    private boolean hashPassword;

    public UsernameToken(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the useUTProfile11.
     */
    public boolean isUseUTProfile11() {
        return useUTProfile11;
    }

    /**
     * @param useUTProfile11 The useUTProfile11 to set.
     */
    public void setUseUTProfile11(boolean useUTProfile11) {
        this.useUTProfile11 = useUTProfile11;
    }

    public boolean isNoPassword() {
        return noPassword;
    }

    public void setNoPassword(boolean noPassword) {
        this.noPassword = noPassword;
    }

    public boolean isHashPassword() {
        return hashPassword;
    }

    public void setHashPassword(boolean hashPassword) {
        this.hashPassword = hashPassword;
    }

    public boolean isUseUTProfile10() {
        return useUTProfile10;
    }

    public void setUseUTProfile10(boolean useUTProfile10) {
        this.useUTProfile10 = useUTProfile10;
    }

    public QName getName() {
        return SP12Constants.INSTANCE.getUserNameToken();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        QName name = constants.getUserNameToken();
        String localname = name.getLocalPart();
        String namespaceURI = name.getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = name.getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:UsernameToken
        writer.writeStartElement(prefix, localname, namespaceURI);

        writer.writeNamespace(prefix, namespaceURI);

        String inclusion;

        inclusion = constants.getAttributeValueFromInclusion(getInclusion());

        if (inclusion != null) {
            writer.writeAttribute(prefix, namespaceURI, SPConstants.ATTR_INCLUDE_TOKEN, inclusion);
        }

        if (isUseUTProfile10() || isUseUTProfile11()) {
            String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
            if (pPrefix == null) {
                pPrefix = SPConstants.POLICY.getPrefix();
                writer.setPrefix(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getNamespaceURI());
            }

            // <wsp:Policy>
            writer.writeStartElement(pPrefix, SPConstants.POLICY.getLocalPart(), SPConstants.POLICY
                .getNamespaceURI());

            // CHECKME
            if (isUseUTProfile10()) {
                // <sp:WssUsernameToken10 />
                writer.writeStartElement(prefix, SPConstants.USERNAME_TOKEN10, namespaceURI);
            } else {
                // <sp:WssUsernameToken11 />
                writer.writeStartElement(prefix, SPConstants.USERNAME_TOKEN11, namespaceURI);
            }

            if (constants.getVersion() == SPConstants.Version.SP_V12) {

                if (isNoPassword()) {
                    writer.writeStartElement(prefix, SPConstants.NO_PASSWORD, namespaceURI);
                    writer.writeEndElement();
                } else if (isHashPassword()) {
                    writer.writeStartElement(prefix, SPConstants.HASH_PASSWORD, namespaceURI);
                    writer.writeEndElement();
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

            }
            writer.writeEndElement();

            // </wsp:Policy>
            writer.writeEndElement();

        }

        writer.writeEndElement();
        // </sp:UsernameToken>

    }
}
