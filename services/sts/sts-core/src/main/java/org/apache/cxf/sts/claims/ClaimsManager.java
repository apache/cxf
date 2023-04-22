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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.IdentityMapper;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.apache.cxf.sts.token.realm.Relationship;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLVersion;


/**
 * This class holds various ClaimsHandler implementations.
 */
public class ClaimsManager {

    private static final Logger LOG = LogUtils.getL7dLogger(ClaimsManager.class);

    private List<ClaimsParser> claimParsers;
    private List<ClaimsHandler> claimHandlers;
    private List<String> supportedClaimTypes = new ArrayList<>();
    private boolean stopProcessingOnException = true;
    private IdentityMapper identityMapper;


    public IdentityMapper getIdentityMapper() {
        return identityMapper;
    }

    public void setIdentityMapper(IdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }

    public boolean isStopProcessingOnException() {
        return stopProcessingOnException;
    }

    public void setStopProcessingOnException(boolean stopProcessingOnException) {
        this.stopProcessingOnException = stopProcessingOnException;
    }

    public List<String> getSupportedClaimTypes() {
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

    public ProcessedClaimCollection retrieveClaimValues(
        ClaimCollection primaryClaims,
        ClaimCollection secondaryClaims,
        ClaimsParameters parameters
    ) {
        if (primaryClaims == null && secondaryClaims == null) {
            return null;
        } else if (primaryClaims != null && secondaryClaims == null) {
            return retrieveClaimValues(primaryClaims, parameters);
        } else if (secondaryClaims != null && primaryClaims == null) {
            return retrieveClaimValues(secondaryClaims, parameters);
        }

        // Here we have two sets of claims
        if (primaryClaims.getDialect() != null
            && primaryClaims.getDialect().equals(secondaryClaims.getDialect())) {
            // Matching dialects - so we must merge them
            ClaimCollection mergedClaims = mergeClaims(primaryClaims, secondaryClaims);
            return retrieveClaimValues(mergedClaims, parameters);
        }
        // If the dialects don't match then just return all Claims
        ProcessedClaimCollection claims = retrieveClaimValues(primaryClaims, parameters);
        ProcessedClaimCollection claims2 = retrieveClaimValues(secondaryClaims, parameters);
        ProcessedClaimCollection returnedClaims = new ProcessedClaimCollection();
        if (claims != null) {
            returnedClaims.addAll(claims);
        }
        if (claims2 != null) {
            returnedClaims.addAll(claims2);
        }
        return returnedClaims;
    }

    public ProcessedClaimCollection retrieveClaimValues(ClaimCollection claims, ClaimsParameters parameters) {
        if (claims == null || claims.isEmpty()) {
            return null;
        }

        Relationship relationship = null;
        if (parameters.getAdditionalProperties() != null) {
            relationship = (Relationship)parameters.getAdditionalProperties().get(
                    Relationship.class.getName());
        }

        if (relationship == null || relationship.getType().equals(Relationship.FED_TYPE_IDENTITY)) {
            // Federate identity. Identity already mapped.
            // Call all configured claims handlers to retrieve the required claims
            ProcessedClaimCollection returnCollection = handleClaims(claims, parameters);
            validateClaimValues(claims, returnCollection);
            return returnCollection;

        }
        // Federate claims
        ClaimsMapper claimsMapper = relationship.getClaimsMapper();
        if (claimsMapper == null) {
            LOG.log(Level.SEVERE, "ClaimsMapper required to federate claims but not configured.");
            throw new STSException("ClaimsMapper required to federate claims but not configured",
                                   STSException.BAD_REQUEST);
        }

        // Get the claims of the received token (only SAML supported)
        // Consider refactoring to use a CallbackHandler and keep ClaimsManager token independent
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)parameters.getAdditionalProperties().get(SamlAssertionWrapper.class.getName());
        final List<ProcessedClaim> claimList;
        if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
            claimList = this.parseClaimsInAssertion(assertion.getSaml2());
        } else {
            claimList = this.parseClaimsInAssertion(assertion.getSaml1());
        }
        ProcessedClaimCollection sourceClaims = new ProcessedClaimCollection();
        sourceClaims.addAll(claimList);

        ProcessedClaimCollection targetClaims = claimsMapper.mapClaims(relationship.getSourceRealm(),
                sourceClaims, relationship.getTargetRealm(), parameters);
        validateClaimValues(claims, targetClaims);
        return targetClaims;
    }

    private ProcessedClaimCollection handleClaims(ClaimCollection claims, ClaimsParameters parameters) {
        ProcessedClaimCollection returnCollection = new ProcessedClaimCollection();
        if (claimHandlers == null) {
            return returnCollection;
        }

        Principal originalPrincipal = parameters.getPrincipal();
        for (ClaimsHandler handler : claimHandlers) {

            ClaimCollection supportedClaims =
                filterHandlerClaims(claims, handler.getSupportedClaimTypes());
            if (supportedClaims.isEmpty()) {
                continue;
            }

            if (isCurrentRealmSupported(handler, parameters)) {
                ProcessedClaimCollection claimCollection = null;
                try {
                    claimCollection = handler.retrieveClaimValues(supportedClaims, parameters);
                } catch (RuntimeException ex) {
                    LOG.log(Level.INFO, "Failed retrieving claims from ClaimsHandler "
                            + handler.getClass().getName(), ex);
                    if (this.isStopProcessingOnException()) {
                        throw ex;
                    }
                } finally {
                    // set original principal again, otherwise wrong principal passed to next claim handler in the list
                    // if no mapping required or wrong source principal used for next identity mapping
                    parameters.setPrincipal(originalPrincipal);
                }

                if (claimCollection != null && !claimCollection.isEmpty()) {
                    returnCollection.addAll(claimCollection);
                }
            }
        }

        return returnCollection;
    }

    private boolean isCurrentRealmSupported(ClaimsHandler handler, ClaimsParameters parameters) {
        if (!(handler instanceof RealmSupport)) {
            return true;
        }

        RealmSupport handlerRealmSupport = (RealmSupport)handler;

        // Check whether the handler supports the current realm
        if (handlerRealmSupport.getSupportedRealms() != null
                && handlerRealmSupport.getSupportedRealms().size() > 0
                && handlerRealmSupport.getSupportedRealms().indexOf(parameters.getRealm()) == -1) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Handler '" + handlerRealmSupport.getClass().getName() + "' doesn't support"
                        + " realm '" + parameters.getRealm()  + "'");
            }
            return false;
        }

        // If handler realm is configured and different from current realm
        // do an identity mapping
        if (handlerRealmSupport.getHandlerRealm() != null
                && !handlerRealmSupport.getHandlerRealm().equalsIgnoreCase(parameters.getRealm())) {
            final Principal targetPrincipal;
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Mapping user '" + parameters.getPrincipal().getName()
                            + "' [" + parameters.getRealm() + "] to realm '"
                            + handlerRealmSupport.getHandlerRealm() + "'");
                }
                targetPrincipal = doMapping(parameters.getRealm(), parameters.getPrincipal(),
                        handlerRealmSupport.getHandlerRealm());
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Failed to map user '" + parameters.getPrincipal().getName()
                        + "' [" + parameters.getRealm() + "] to realm '"
                        + handlerRealmSupport.getHandlerRealm() + "'", ex);
                throw new STSException("Failed to map user for claims handler",
                        STSException.REQUEST_FAILED);
            }

            if (targetPrincipal == null || targetPrincipal.getName() == null) {
                LOG.log(Level.WARNING, "Null. Failed to map user '" + parameters.getPrincipal().getName()
                        + "' [" + parameters.getRealm() + "] to realm '"
                        + handlerRealmSupport.getHandlerRealm() + "'");
                return false;
            }
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Principal '" + targetPrincipal.getName()
                        + "' passed to handler '" + handlerRealmSupport.getClass().getName() + "'");
            }
            parameters.setPrincipal(targetPrincipal);
        } else {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Handler '" + handlerRealmSupport.getClass().getName() + "' doesn't require"
                        + " identity mapping '" + parameters.getRealm()  + "'");
            }
        }

        return true;
    }

    private ClaimCollection filterHandlerClaims(ClaimCollection claims,
                                                         List<String> handlerClaimTypes) {
        ClaimCollection supportedClaims = new ClaimCollection();
        supportedClaims.setDialect(claims.getDialect());
        for (Claim claim : claims) {
            if (handlerClaimTypes.contains(claim.getClaimType())) {
                supportedClaims.add(claim);
            }
        }
        return supportedClaims;
    }

    private boolean validateClaimValues(ClaimCollection requestedClaims, ProcessedClaimCollection claims) {
        for (Claim claim : requestedClaims) {
            String claimType = claim.getClaimType();
            boolean found = false;
            if (!claim.isOptional()) {
                for (ProcessedClaim c : claims) {
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


    protected List<ProcessedClaim> parseClaimsInAssertion(org.opensaml.saml.saml1.core.Assertion assertion) {
        List<org.opensaml.saml.saml1.core.AttributeStatement> attributeStatements =
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("No attribute statements found");
            }
            return Collections.emptyList();
        }
        ProcessedClaimCollection collection = new ProcessedClaimCollection();

        for (org.opensaml.saml.saml1.core.AttributeStatement statement : attributeStatements) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("parsing statement: " + statement.getElementQName());
            }

            List<org.opensaml.saml.saml1.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml.saml1.core.Attribute attribute : attributes) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("parsing attribute: " + attribute.getAttributeName());
                }
                ProcessedClaim c = new ProcessedClaim();
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
                }
                collection.add(c);
            }
        }
        return collection;
    }

    protected List<ProcessedClaim> parseClaimsInAssertion(org.opensaml.saml.saml2.core.Assertion assertion) {
        List<org.opensaml.saml.saml2.core.AttributeStatement> attributeStatements =
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("No attribute statements found");
            }
            return Collections.emptyList();
        }

        List<ProcessedClaim> collection = new ArrayList<>();

        for (org.opensaml.saml.saml2.core.AttributeStatement statement : attributeStatements) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("parsing statement: " + statement.getElementQName());
            }
            List<org.opensaml.saml.saml2.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml.saml2.core.Attribute attribute : attributes) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("parsing attribute: " + attribute.getName());
                }
                ProcessedClaim c = new ProcessedClaim();
                c.setClaimType(URI.create(attribute.getName()));
                c.setIssuer(assertion.getIssuer().getNameQualifier());
                for (XMLObject attributeValue : attribute.getAttributeValues()) {
                    Element attributeValueElement = attributeValue.getDOM();
                    String value = attributeValueElement.getTextContent();
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest(" [" + value + "]");
                    }
                    c.addValue(value);
                }
                collection.add(c);
            }
        }
        return collection;

    }

    /**
     * This method merges the primary claims with the secondary claims (of the same dialect).
     * This facilitates handling claims from a service via wst:SecondaryParameters/wst:Claims
     * with any client-specific claims sent in wst:RequestSecurityToken/wst:Claims
     */
    private ClaimCollection mergeClaims(
        ClaimCollection primaryClaims, ClaimCollection secondaryClaims
    ) {
        ClaimCollection parsedClaims = new ClaimCollection();
        parsedClaims.addAll(secondaryClaims);

        // Merge claims
        ClaimCollection mergedClaims = new ClaimCollection();
        mergedClaims.setDialect(primaryClaims.getDialect());

        for (Claim claim : primaryClaims) {
            Claim matchingClaim = null;
            // Search for a matching claim via the ClaimType URI
            for (Claim secondaryClaim : parsedClaims) {
                if (secondaryClaim.getClaimType().equals(claim.getClaimType())) {
                    matchingClaim = secondaryClaim;
                    break;
                }
            }

            if (matchingClaim == null) {
                mergedClaims.add(claim);
            } else {
                Claim mergedClaim = new Claim();
                mergedClaim.setClaimType(claim.getClaimType());
                if (claim.getValues() != null && !claim.getValues().isEmpty()) {
                    mergedClaim.setValues(claim.getValues());
                    if (matchingClaim.getValues() != null && !matchingClaim.getValues().isEmpty()) {
                        LOG.log(Level.WARNING, "Secondary claim value " + matchingClaim.getValues()
                                + " ignored in favour of primary claim value");
                    }
                } else if (matchingClaim.getValues() != null && !matchingClaim.getValues().isEmpty()) {
                    mergedClaim.setValues(matchingClaim.getValues());
                }
                mergedClaims.add(mergedClaim);

                // Remove from parsed Claims
                parsedClaims.remove(matchingClaim);
            }
        }

        // Now add in any claims from the parsed claims that weren't merged
        mergedClaims.addAll(parsedClaims);

        return mergedClaims;
    }


    protected Principal doMapping(String sourceRealm, Principal sourcePrincipal, String targetRealm) {
        return this.identityMapper.mapPrincipal(
                sourceRealm, sourcePrincipal, targetRealm);

    }

}
