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

package org.apache.cxf.sts.claims;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.token.realm.Relationship;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.opensaml.common.SAMLVersion;
import org.opensaml.xml.XMLObject;


/**
 * This class holds various ClaimsHandler implementations.
 */
public class ClaimsManager {

    private static final Logger LOG = LogUtils.getL7dLogger(ClaimsManager.class);

    private List<ClaimsParser> claimParsers;
    private List<ClaimsHandler> claimHandlers;
    private List<URI> supportedClaimTypes = new ArrayList<URI>();

    public List<URI> getSupportedClaimTypes() {
        return supportedClaimTypes;
    }

    public List<ClaimsParser> getClaimParsers() {
        return claimParsers;
    }
    
    public List<ClaimsHandler> getClaimHandlers() {
        return claimHandlers;
    }

    public void setClaimParsers(List<ClaimsParser> claimParsers) {
        this.claimParsers = claimParsers;
    }
    
    public void setClaimHandlers(List<ClaimsHandler> claimHandlers) {
        this.claimHandlers = claimHandlers;
        if (claimHandlers == null) {
            supportedClaimTypes.clear();
        } else {
            for (ClaimsHandler handler : claimHandlers) {
                supportedClaimTypes.addAll(handler.getSupportedClaimTypes());
            }
        }
    }

    public ClaimCollection retrieveClaimValues(RequestClaimCollection claims, ClaimsParameters parameters) {
        Relationship relationship = null;
        if (parameters.getAdditionalProperties() != null) {
            relationship = (Relationship)parameters.getAdditionalProperties().get(
                    Relationship.class.getName());
        }

        if (relationship == null || relationship.getType().equals(Relationship.FED_TYPE_IDENTITY)) {
            // Federate identity. Identity already mapped.
            // Call all configured claims handlers to retrieve the required claims
            if (claimHandlers != null && claimHandlers.size() > 0 && claims != null && claims.size() > 0) {
                ClaimCollection returnCollection = new ClaimCollection();
                for (ClaimsHandler handler : claimHandlers) {
                    ClaimCollection claimCollection = handler.retrieveClaimValues(claims, parameters);
                    if (claimCollection != null && claimCollection.size() != 0) {
                        returnCollection.addAll(claimCollection);
                    }
                }
                validateClaimValues(claims, returnCollection);
                return returnCollection;
            }
            
        } else {
            // Federate claims
            ClaimsMapper claimsMapper = relationship.getClaimsMapper();
            if (claimsMapper == null) {
                LOG.log(Level.SEVERE, "ClaimsMapper required to federate claims but not configured.");
                throw new STSException("ClaimsMapper required to federate claims but not configured",
                                       STSException.BAD_REQUEST);
            }
            
            // Get the claims of the received token (only SAML supported)
            // Consider refactoring to use a CallbackHandler and keep ClaimsManager token independent
            AssertionWrapper assertion = 
                (AssertionWrapper)parameters.getAdditionalProperties().get(AssertionWrapper.class.getName());
            List<Claim> claimList = null;
            if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
                claimList = this.parseClaimsInAssertion(assertion.getSaml2());
            } else {
                claimList = this.parseClaimsInAssertion(assertion.getSaml1());
            }
            ClaimCollection sourceClaims = new ClaimCollection();
            sourceClaims.addAll(claimList);
            
            ClaimCollection targetClaims = claimsMapper.mapClaims(relationship.getSourceRealm(),
                    sourceClaims, relationship.getTargetRealm(), parameters);
            validateClaimValues(claims, targetClaims);
            return targetClaims;
        }

        return null;
    }

    private boolean validateClaimValues(RequestClaimCollection requestedClaims, ClaimCollection claims) {
        for (RequestClaim claim : requestedClaims) {
            URI claimType = claim.getClaimType();
            boolean found = false;
            if (!claim.isOptional()) {
                for (Claim c : claims) {
                    if (c.getClaimType().equals(claimType)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOG.warning("Mandatory claim not found: " + claim.getClaimType());
                    throw new STSException("Mandatory claim '" + claim.getClaimType() + "' not found");
                }
            }
        }
        return true;

    }


    protected List<Claim> parseClaimsInAssertion(org.opensaml.saml1.core.Assertion assertion) {
        List<org.opensaml.saml1.core.AttributeStatement> attributeStatements = 
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("No attribute statements found");
            }            
            return Collections.emptyList();
        }
        ClaimCollection collection = new ClaimCollection();

        for (org.opensaml.saml1.core.AttributeStatement statement : attributeStatements) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("parsing statement: " + statement.getElementQName());
            }

            List<org.opensaml.saml1.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml1.core.Attribute attribute : attributes) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("parsing attribute: " + attribute.getAttributeName());
                }
                Claim c = new Claim();
                c.setIssuer(assertion.getIssuer());
                c.setClaimType(URI.create(attribute.getAttributeName()));
                try {
                    c.setClaimType(new URI(attribute.getAttributeName()));
                } catch (URISyntaxException e) {
                    LOG.warning("Invalid attribute name in attributestatement: " + e.getMessage());
                    continue;
                }
                for (XMLObject attributeValue : attribute.getAttributeValues()) {
                    Element attributeValueElement = attributeValue.getDOM();
                    String value = attributeValueElement.getTextContent();
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest(" [" + value + "]");
                    }
                    c.addValue(value);
                    collection.add(c);
                    break;                    
                }
            }
        }
        return collection;
    }

    protected List<Claim> parseClaimsInAssertion(org.opensaml.saml2.core.Assertion assertion) {
        List<org.opensaml.saml2.core.AttributeStatement> attributeStatements = 
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("No attribute statements found");
            }
            return Collections.emptyList();
        }

        List<Claim> collection = new ArrayList<Claim>();

        for (org.opensaml.saml2.core.AttributeStatement statement : attributeStatements) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("parsing statement: " + statement.getElementQName());
            }
            List<org.opensaml.saml2.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml2.core.Attribute attribute : attributes) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("parsing attribute: " + attribute.getName());
                }
                Claim c = new Claim();
                c.setClaimType(URI.create(attribute.getName()));
                c.setIssuer(assertion.getIssuer().getNameQualifier());
                for (XMLObject attributeValue : attribute.getAttributeValues()) {
                    Element attributeValueElement = attributeValue.getDOM();
                    String value = attributeValueElement.getTextContent();
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest(" [" + value + "]");
                    }
                    c.addValue(value);
                    collection.add(c);
                    break;
                }
            }
        }
        return collection;

    }


}
