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
import org.apache.cxf.ws.security.policy.SPConstants;

/**
 * Model bean for the IssuedToken assertion.
 */
public class IssuedToken extends Token {

    private Element issuerEpr;

    private Element issuerMex;

    private Element rstTemplate;

    private boolean requireExternalReference;

    private boolean requireInternalReference;

    public IssuedToken(SPConstants version) {
        super(version);
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

    /**
     * @return Returns the requireExternalReference.
     */
    public boolean isRequireExternalReference() {
        return requireExternalReference;
    }

    /**
     * @param requireExternalReference The requireExternalReference to set.
     */
    public void setRequireExternalReference(boolean requireExternalReference) {
        this.requireExternalReference = requireExternalReference;
    }

    /**
     * @return Returns the requireInternalReference.
     */
    public boolean isRequireInternalReference() {
        return requireInternalReference;
    }

    /**
     * @param requireInternalReference The requireInternalReference to set.
     */
    public void setRequireInternalReference(boolean requireInternalReference) {
        this.requireInternalReference = requireInternalReference;
    }

    /**
     * @return Returns the rstTemplate.
     */
    public Element getRstTemplate() {
        return rstTemplate;
    }

    /**
     * @param rstTemplate The rstTemplate to set.
     */
    public void setRstTemplate(Element rstTemplate) {
        this.rstTemplate = rstTemplate;
    }

    public QName getName() {
        return constants.getIssuedToken();
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

        // <sp:IssuedToken>
        writer.writeStartElement(prefix, localname, namespaceURI);

        if (writerPrefix == null) {
            writer.writeNamespace(prefix, namespaceURI);
        }

        String inclusion;

        inclusion = constants.getAttributeValueFromInclusion(getInclusion());

        if (inclusion != null) {
            writer.writeAttribute(prefix, namespaceURI, SPConstants.ATTR_INCLUDE_TOKEN, inclusion);
        }

        if (issuerEpr != null) {
            writer.writeStartElement(prefix, SPConstants.ISSUER, namespaceURI);
            StaxUtils.copy(issuerEpr, writer);
            writer.writeEndElement();
        }

        if (rstTemplate != null) {
            // <sp:RequestSecurityTokenTemplate>
            StaxUtils.copy(rstTemplate, writer);
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

        if (isRequireExternalReference() || isRequireInternalReference() || this.isDerivedKeys()) {

            // <wsp:Policy>
            writer.writeStartElement(wspPrefix, policyLocalName, policyNamespaceURI);

            if (wspWriterPrefix == null) {
                // xmlns:wsp=".."
                writer.writeNamespace(wspPrefix, policyNamespaceURI);
            }

            if (isRequireExternalReference()) {
                // <sp:RequireExternalReference />
                writer.writeEmptyElement(prefix, SPConstants.REQUIRE_EXTERNAL_REFERENCE, namespaceURI);
            }

            if (isRequireInternalReference()) {
                // <sp:RequireInternalReference />
                writer.writeEmptyElement(prefix, SPConstants.REQUIRE_INTERNAL_REFERENCE, namespaceURI);
            }

            if (this.isDerivedKeys()) {
                // <sp:RequireDerivedKeys />
                writer.writeEmptyElement(prefix, SPConstants.REQUIRE_DERIVED_KEYS, namespaceURI);
            }

            // <wsp:Policy>
            writer.writeEndElement();
        }

        // </sp:IssuedToken>
        writer.writeEndElement();
    }

    public Element getIssuerMex() {
        return issuerMex;
    }

    public void setIssuerMex(Element issuerMex) {
        this.issuerMex = issuerMex;
    }

}
