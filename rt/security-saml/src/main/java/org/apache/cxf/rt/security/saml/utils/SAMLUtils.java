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
package org.apache.cxf.rt.security.saml.utils;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.claims.SAMLClaim;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;

public final class SAMLUtils {

    private SAMLUtils() {

    }

    /**
     * Extract Claims from a SAML Assertion
     */
    public static ClaimCollection getClaims(SamlAssertionWrapper assertion) {
        ClaimCollection claims = new ClaimCollection();

        if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
            List<AttributeStatement> statements = assertion.getSaml2().getAttributeStatements();
            for (AttributeStatement as : statements) {
                for (Attribute atr : as.getAttributes()) {
                    SAMLClaim claim = new SAMLClaim();
                    claim.setClaimType(atr.getName());

                    claim.setName(atr.getName());
                    claim.setNameFormat(atr.getNameFormat());
                    claim.setFriendlyName(atr.getFriendlyName());

                    for (XMLObject o : atr.getAttributeValues()) {
                        String attrValue = o.getDOM().getTextContent();
                        claim.getValues().add(attrValue);
                    }

                    claims.add(claim);
                }
            }
        } else {
            List<org.opensaml.saml.saml1.core.AttributeStatement> attributeStatements =
                assertion.getSaml1().getAttributeStatements();

            for (org.opensaml.saml.saml1.core.AttributeStatement statement : attributeStatements) {
                for (org.opensaml.saml.saml1.core.Attribute atr : statement.getAttributes()) {
                    SAMLClaim claim = new SAMLClaim();

                    String claimType = atr.getAttributeName();
                    if (atr.getAttributeNamespace() != null) {
                        claimType = atr.getAttributeNamespace() + "/" + claimType;
                    }
                    claim.setClaimType(claimType);

                    claim.setName(atr.getAttributeName());
                    claim.setNameFormat(atr.getAttributeNamespace());

                    for (XMLObject o : atr.getAttributeValues()) {
                        String attrValue = o.getDOM().getTextContent();
                        claim.getValues().add(attrValue);
                    }

                    claims.add(claim);
                }
            }
        }

        return claims;
    }

    /**
     * Extract roles from the given Claims
     */
    public static Set<Principal> parseRolesFromClaims(
        ClaimCollection claims,
        String name,
        String nameFormat
    ) {
        Set<Principal> roles = new HashSet<>();

        for (Claim claim : claims) {
            if (claim instanceof SAMLClaim && ((SAMLClaim)claim).getName().equals(name)
                && (nameFormat == null
                    || nameFormat.equals(((SAMLClaim)claim).getNameFormat()))) {
                for (Object claimValue : claim.getValues()) {
                    if (claimValue instanceof String) {
                        roles.add(new SimpleGroup((String)claimValue));
                    }
                }
                if (claim.getValues().size() > 1) {
                    // Don't search for other attributes with the same name if > 1 claim value
                    break;
                }
            }
        }

        return roles;
    }

    public static String getIssuer(Object assertion) {
        return ((SamlAssertionWrapper)assertion).getIssuerString();
    }

    public static Element getAssertionElement(Object assertion) {
        return ((SamlAssertionWrapper)assertion).getElement();
    }

    public static List<String> getAudienceRestrictions(Message msg, boolean enableByDefault) {
        // Add Audience Restrictions for SAML
        boolean enableAudienceRestriction =
            SecurityUtils.getSecurityPropertyBoolean(SecurityConstants.AUDIENCE_RESTRICTION_VALIDATION,
                                                     msg, enableByDefault);
        if (enableAudienceRestriction) {
            List<String> audiences = new ArrayList<>();
            // See if we have custom audience restriction values specified first
            String audienceRestrictions =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.AUDIENCE_RESTRICTIONS, msg);
            if (audienceRestrictions != null) {
                for (String audienceRestriction : audienceRestrictions.split(",")) {
                    audiences.add(audienceRestriction);
                }
            }

            // Defaults
            if (audiences.isEmpty()) {
                if (msg.get(org.apache.cxf.message.Message.REQUEST_URL) != null) {
                    audiences.add((String)msg.get(org.apache.cxf.message.Message.REQUEST_URL));
                } else if (msg.get(org.apache.cxf.message.Message.REQUEST_URI) != null) {
                    audiences.add((String)msg.get(org.apache.cxf.message.Message.REQUEST_URI));
                }

                if (msg.getContextualProperty(Message.WSDL_SERVICE) != null) {
                    audiences.add(msg.getContextualProperty(Message.WSDL_SERVICE).toString());
                }
            }
            return audiences;
        }
        return Collections.emptyList();
    }
}
