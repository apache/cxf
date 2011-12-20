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

import org.w3c.dom.Element;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;

/**
 * Model class for SpnegoContextToken
 */
public class SpnegoContextToken extends Token {
    
    private Element issuerEpr;
    
    public SpnegoContextToken(SPConstants version) {
        super(version);
    }
    
    public QName getName() {
        return SP12Constants.INSTANCE.getSpnegoContextToken();
    }
    
    /**
     * @return Returns the issuerEpr.
     */
    public Element getIssuerEpr() {
        return issuerEpr;
    }

    /**
     * @param issuerEpr The issuerEpr to set.
     */
    public void setIssuerEpr(Element issuerEpr) {
        this.issuerEpr = issuerEpr;
    }

    
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        QName name = constants.getSpnegoContextToken();
        String localname = name.getLocalPart();
        String namespaceURI = name.getNamespaceURI();
        String prefix;

        String writerPrefix = writer.getPrefix(namespaceURI);

        if (writerPrefix == null) {
            prefix = name.getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        } else {
            prefix = writerPrefix;
        }

        // <sp:SpnegoContextToken>
        writer.writeStartElement(prefix, localname, namespaceURI);

        if (writerPrefix == null) {
            // xmlns:sp=".."
            writer.writeNamespace(prefix, namespaceURI);
        }

        String inclusion;

        inclusion = constants.getAttributeValueFromInclusion(getInclusion());

        if (inclusion != null) {
            writer.writeAttribute(prefix, namespaceURI, SPConstants.ATTR_INCLUDE_TOKEN, inclusion);
        }

        if (issuerEpr != null) {
            // <sp:Issuer>
            writer.writeStartElement(prefix, SPConstants.ISSUER, namespaceURI);

            StaxUtils.copy(issuerEpr, writer);

            writer.writeEndElement();
        }

        if (isDerivedKeys()) {

            String wspNamespaceURI = SPConstants.POLICY.getNamespaceURI();

            String wspPrefix;

            String wspWriterPrefix = writer.getPrefix(wspNamespaceURI);

            if (wspWriterPrefix == null) {
                wspPrefix = SPConstants.POLICY.getPrefix();
                writer.setPrefix(wspPrefix, wspNamespaceURI);

            } else {
                wspPrefix = wspWriterPrefix;
            }

            // <wsp:Policy>
            writer.writeStartElement(wspPrefix, SPConstants.POLICY.getLocalPart(), wspNamespaceURI);

            if (wspWriterPrefix == null) {
                // xmlns:wsp=".."
                writer.writeNamespace(wspPrefix, wspNamespaceURI);
            }

            if (isDerivedKeys()) {
                // <sp:RequireDerivedKeys />
                writer.writeEmptyElement(prefix, SPConstants.REQUIRE_DERIVED_KEYS, namespaceURI);
            }

            // </wsp:Policy>
            writer.writeEndElement();
        }

        // </sp:SpnegoContextToken>
        writer.writeEndElement();
    }
    
    
}
