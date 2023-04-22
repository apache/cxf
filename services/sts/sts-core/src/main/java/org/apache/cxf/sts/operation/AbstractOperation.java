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

package org.apache.cxf.sts.operation;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.IdentityMapper;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.RealmParser;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.event.AbstractSTSEvent;
import org.apache.cxf.sts.event.STSEventListener;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.RequestParser;
import org.apache.cxf.sts.request.RequestRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.apache.cxf.sts.token.delegation.TokenDelegationParameters;
import org.apache.cxf.sts.token.delegation.TokenDelegationResponse;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenReference;
import org.apache.cxf.sts.token.realm.Relationship;
import org.apache.cxf.sts.token.realm.RelationshipResolver;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.secext.KeyIdentifierType;
import org.apache.cxf.ws.security.sts.provider.model.secext.ReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.secext.SecurityTokenReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.utility.AttributedDateTime;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.DateUtil;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncryptedKey;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;

/**
 * This abstract class contains some common functionality for different operations.
 */
public abstract class AbstractOperation {

    public static final QName TOKEN_TYPE =
        new QName(WSConstants.WSSE11_NS, WSConstants.TOKEN_TYPE, WSConstants.WSSE11_PREFIX);

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractOperation.class);

    protected STSPropertiesMBean stsProperties;
    protected boolean encryptIssuedToken;
    protected List<ServiceMBean> services;
    protected List<TokenProvider> tokenProviders = new ArrayList<>();
    protected List<TokenValidator> tokenValidators = new ArrayList<>();
    protected boolean returnReferences = true;
    protected TokenStore tokenStore;
    protected ClaimsManager claimsManager = new ClaimsManager();
    protected STSEventListener eventPublisher;
    protected List<TokenDelegationHandler> delegationHandlers = new ArrayList<>();
    protected TokenWrapper tokenWrapper = new DefaultTokenWrapper();
    protected boolean allowCustomContent;
    protected boolean includeLifetimeElement = true;

    public boolean isAllowCustomContent() {
        return allowCustomContent;
    }

    public void setAllowCustomContent(boolean allowCustomContent) {
        this.allowCustomContent = allowCustomContent;
    }

    public TokenWrapper getTokenWrapper() {
        return tokenWrapper;
    }

    public void setTokenWrapper(TokenWrapper tokenWrapper) {
        this.tokenWrapper = tokenWrapper;
    }

    public boolean isReturnReferences() {
        return returnReferences;
    }

    public void setReturnReferences(boolean returnReferences) {
        this.returnReferences = returnReferences;
    }

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public void setStsProperties(STSPropertiesMBean stsProperties) {
        this.stsProperties = stsProperties;
    }

    public void setEncryptIssuedToken(boolean encryptIssuedToken) {
        this.encryptIssuedToken = encryptIssuedToken;
    }

    public void setServices(List<ServiceMBean> services) {
        this.services = services;
    }

    public void setTokenProviders(List<TokenProvider> tokenProviders) {
        this.tokenProviders = tokenProviders;
    }

    public List<TokenDelegationHandler> getDelegationHandlers() {
        return delegationHandlers;
    }

    public void setDelegationHandlers(List<TokenDelegationHandler> delegationHandlers) {
        this.delegationHandlers = delegationHandlers;
    }

    public List<TokenProvider> getTokenProviders() {
        return tokenProviders;
    }

    public void setTokenValidators(List<TokenValidator> tokenValidators) {
        this.tokenValidators = tokenValidators;
    }

    public List<TokenValidator> getTokenValidators() {
        return tokenValidators;
    }

    public ClaimsManager getClaimsManager() {
        return claimsManager;
    }

    public void setClaimsManager(ClaimsManager claimsManager) {
        this.claimsManager = claimsManager;
    }

    public void setIncludeLifetimeElement(boolean value) {
        this.includeLifetimeElement = value;
    }

    public boolean isIncludeLifetimeElement() {
        return includeLifetimeElement;
    }

    /**
     * Check the arguments from the STSProvider and parse the request.
     */
    protected RequestRequirements parseRequest(
        RequestSecurityTokenType request,
        Map<String, Object> messageContext
    ) {
        if (messageContext == null) {
            throw new STSException("No message context found");
        }

        if (stsProperties == null) {
            throw new STSException("No STSProperties object found");
        }
        stsProperties.configureProperties();

        RequestParser requestParser = new RequestParser();
        requestParser.setAllowCustomContent(allowCustomContent);
        return requestParser.parseRequest(request, messageContext, stsProperties,
                                          claimsManager.getClaimParsers());
    }

    protected void cleanRequest(RequestRequirements requestRequirements) {
        if (requestRequirements.getKeyRequirements() != null
            && requestRequirements.getKeyRequirements().getEntropy() != null) {
            requestRequirements.getKeyRequirements().getEntropy().clean();
        }
    }

    /**
     * Create a RequestedReferenceType object using a TokenReference object
     */
    protected static RequestedReferenceType createRequestedReference(
        TokenReference tokenReference, boolean attached
    ) {
        RequestedReferenceType requestedReferenceType =
            QNameConstants.WS_TRUST_FACTORY.createRequestedReferenceType();
        SecurityTokenReferenceType securityTokenReferenceType =
            QNameConstants.WSSE_FACTORY.createSecurityTokenReferenceType();

        // TokenType
        String tokenType = tokenReference.getWsse11TokenType();
        if (tokenType != null) {
            securityTokenReferenceType.getOtherAttributes().put(TOKEN_TYPE, tokenType);
        }

        if (tokenReference.isUseKeyIdentifier()) {
            String identifier = XMLUtils.getIDFromReference(tokenReference.getIdentifier());

            KeyIdentifierType keyIdentifierType =
                QNameConstants.WSSE_FACTORY.createKeyIdentifierType();
            keyIdentifierType.setValue(identifier);
            String valueType = tokenReference.getWsseValueType();
            if (valueType != null) {
                keyIdentifierType.setValueType(valueType);
            }
            JAXBElement<KeyIdentifierType> keyIdentifier =
                QNameConstants.WSSE_FACTORY.createKeyIdentifier(keyIdentifierType);
            securityTokenReferenceType.getAny().add(keyIdentifier);
        } else if (tokenReference.isUseDirectReference()) {
            String identifier = tokenReference.getIdentifier();
            if (attached && identifier.charAt(0) != '#') {
                identifier = "#" + identifier;
            } else if (!attached && identifier.charAt(0) == '#') {
                identifier = identifier.substring(1);
            }

            ReferenceType referenceType = QNameConstants.WSSE_FACTORY.createReferenceType();
            referenceType.setURI(identifier);

            String valueType = tokenReference.getWsseValueType();
            if (valueType != null) {
                referenceType.setValueType(valueType);
            }
            JAXBElement<ReferenceType> reference =
                QNameConstants.WSSE_FACTORY.createReference(referenceType);
            securityTokenReferenceType.getAny().add(reference);
        }

        requestedReferenceType.setSecurityTokenReference(securityTokenReferenceType);

        return requestedReferenceType;
    }

    /**
     * Create a RequestedReferenceType object using a token id and tokenType
     */
    protected static RequestedReferenceType createRequestedReference(
        String tokenId, String tokenType, boolean attached
    ) {
        TokenReference tokenReference = new TokenReference();
        tokenReference.setIdentifier(tokenId);

        if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
            || WSS4JConstants.SAML_NS.equals(tokenType)) {
            tokenReference.setWsse11TokenType(WSS4JConstants.WSS_SAML_TOKEN_TYPE);
            tokenReference.setUseKeyIdentifier(true);
            tokenReference.setWsseValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
        } else if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
            || WSS4JConstants.SAML2_NS.equals(tokenType)) {
            tokenReference.setWsse11TokenType(WSS4JConstants.WSS_SAML2_TOKEN_TYPE);
            tokenReference.setUseKeyIdentifier(true);
            tokenReference.setWsseValueType(WSS4JConstants.WSS_SAML2_KI_VALUE_TYPE);
        } else {
            tokenReference.setUseDirectReference(true);
            tokenReference.setWsseValueType(tokenType);
        }

        return createRequestedReference(tokenReference, attached);
    }

    /**
     * Create a LifetimeType object given a created + expires Dates
     */
    protected static LifetimeType createLifetime(
        Instant tokenCreated, Instant tokenExpires
    ) {
        AttributedDateTime created = QNameConstants.UTIL_FACTORY.createAttributedDateTime();
        AttributedDateTime expires = QNameConstants.UTIL_FACTORY.createAttributedDateTime();

        Instant now = Instant.now();
        Instant creationTime = tokenCreated;
        if (tokenCreated == null) {
            creationTime = now;
        }

        Instant expirationTime = tokenExpires;
        if (tokenExpires == null) {
            long lifeTimeOfToken = 300L;
            expirationTime = now.plusSeconds(lifeTimeOfToken);
        }

        created.setValue(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        expires.setValue(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Token lifetime creation: " + created.getValue());
            LOG.fine("Token lifetime expiration: " + expires.getValue());
        }

        LifetimeType lifetimeType = QNameConstants.WS_TRUST_FACTORY.createLifetimeType();
        lifetimeType.setCreated(created);
        lifetimeType.setExpires(expires);
        return lifetimeType;
    }

    /**
     * Encrypt a secret using the given arguments producing a DOM EncryptedKey element
     */
    protected Element encryptSecret(
        byte[] secret,
        EncryptionProperties encryptionProperties,
        KeyRequirements keyRequirements
    ) throws WSSecurityException {
        String name = encryptionProperties.getEncryptionName();
        if (name == null) {
            name = stsProperties.getEncryptionUsername();
        }
        if (name == null) {
            throw new STSException("No encryption alias is configured", STSException.REQUEST_FAILED);
        }

        // Get the key-wrap algorithm to use
        String keyWrapAlgorithm = keyRequirements.getKeywrapAlgorithm();
        if (keyWrapAlgorithm == null) {
            // If none then default to what is configured
            keyWrapAlgorithm = encryptionProperties.getKeyWrapAlgorithm();
        } else {
            List<String> supportedAlgorithms =
                encryptionProperties.getAcceptedKeyWrapAlgorithms();
            if (!supportedAlgorithms.contains(keyWrapAlgorithm)) {
                keyWrapAlgorithm = encryptionProperties.getKeyWrapAlgorithm();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("KeyWrapAlgorithm not supported, defaulting to: " + keyWrapAlgorithm);
                }
            }
        }

        Document doc = DOMUtils.getEmptyDocument();

        WSSecEncryptedKey builder = new WSSecEncryptedKey(doc);
        builder.setUserInfo(name);
        builder.setKeyIdentifierType(encryptionProperties.getKeyIdentifierType());
        builder.setKeyEncAlgo(keyWrapAlgorithm);

        final SecretKey symmetricKey;
        if (secret != null) {
            symmetricKey = KeyUtils.prepareSecretKey(encryptionProperties.getEncryptionAlgorithm(), secret);
        } else {
            KeyGenerator keyGen = KeyUtils.getKeyGenerator(encryptionProperties.getEncryptionAlgorithm());
            symmetricKey = keyGen.generateKey();
        }

        builder.prepare(stsProperties.getEncryptionCrypto(), symmetricKey);

        return builder.getEncryptedKeyElement();
    }

    /**
     * Extract an address from an AppliesTo DOM element
     */
    protected String extractAddressFromAppliesTo(Element appliesTo) {
        LOG.fine("Parsing AppliesTo element");
        if (appliesTo != null) {
            Element endpointRef =
                DOMUtils.getFirstChildWithName(
                    appliesTo, STSConstants.WSA_NS_05, "EndpointReference"
                );
            if (endpointRef != null) {
                LOG.fine("Found EndpointReference element");
                Element address =
                    DOMUtils.getFirstChildWithName(
                        endpointRef, STSConstants.WSA_NS_05, "Address");
                if (address != null) {
                    LOG.fine("Found address element");
                    return address.getTextContent();
                }
            } else if (appliesTo.getNamespaceURI() != null) {
                Element uri =
                    DOMUtils.getFirstChildWithName(
                        appliesTo, appliesTo.getNamespaceURI(), "URI"
                    );
                if (uri != null) {
                    LOG.fine("Found URI element");
                    return uri.getTextContent();
                }
            }
        }
        LOG.fine("AppliesTo element does not exist or could not be parsed");
        return null;
    }

    /**
     * Create a TokenProviderParameters object
     */
    protected TokenProviderParameters createTokenProviderParameters(
        RequestRequirements requestRequirements, Principal principal,
        Map<String, Object> messageContext
    ) {
        TokenProviderParameters providerParameters = new TokenProviderParameters();
        providerParameters.setStsProperties(stsProperties);
        providerParameters.setPrincipal(principal);
        providerParameters.setMessageContext(messageContext);
        providerParameters.setTokenStore(getTokenStore());
        providerParameters.setEncryptToken(encryptIssuedToken);

        KeyRequirements keyRequirements = requestRequirements.getKeyRequirements();
        TokenRequirements tokenRequirements = requestRequirements.getTokenRequirements();
        providerParameters.setKeyRequirements(keyRequirements);
        providerParameters.setTokenRequirements(tokenRequirements);

        // Extract AppliesTo
        String address = extractAddressFromAppliesTo(tokenRequirements.getAppliesTo());
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("The AppliesTo address that has been received is: " + address);
        }
        providerParameters.setAppliesToAddress(address);

        // Get the realm of the request
        if (stsProperties.getRealmParser() != null) {
            RealmParser realmParser = stsProperties.getRealmParser();
            String realm = realmParser.parseRealm(messageContext);
            providerParameters.setRealm(realm);
        }

        // Set the requested Claims
        ClaimCollection claims = tokenRequirements.getPrimaryClaims();
        providerParameters.setRequestedPrimaryClaims(claims);
        claims = tokenRequirements.getSecondaryClaims();
        providerParameters.setRequestedSecondaryClaims(claims);

        EncryptionProperties encryptionProperties = stsProperties.getEncryptionProperties();
        if (address != null) {
            boolean foundService = false;
            // Get the stored Service object corresponding to the Service endpoint
            if (services != null) {
                for (ServiceMBean service : services) {
                    if (service.isAddressInEndpoints(address)) {
                        EncryptionProperties svcEncryptionProperties =
                            service.getEncryptionProperties();
                        if (svcEncryptionProperties != null) {
                            encryptionProperties = svcEncryptionProperties;
                        }
                        if (tokenRequirements.getTokenType() == null) {
                            String tokenType = service.getTokenType();
                            tokenRequirements.setTokenType(tokenType);
                            LOG.fine("Using default token type of: " + tokenType);
                        }
                        if (keyRequirements.getKeyType() == null) {
                            String keyType = service.getKeyType();
                            keyRequirements.setKeyType(keyType);
                            LOG.fine("Using default key type of: " + keyType);
                        }
                        foundService = true;
                        break;
                    }
                }
            }
            if (!foundService) {
                String msg = "No service corresponding to "
                             + address
                             + " is known. Check 'services' property configuration in SecurityTokenServiceProvider";
                LOG.log(Level.SEVERE, msg);
                throw new STSException(msg, STSException.REQUEST_FAILED);
            }
        }

        providerParameters.setEncryptionProperties(encryptionProperties);

        return providerParameters;
    }

    protected TokenValidatorResponse validateReceivedToken(
            Principal principal,
            Map<String, Object> messageContext, String realm,
            TokenRequirements tokenRequirements, ReceivedToken token) {
        token.setState(STATE.NONE);

        TokenRequirements validateRequirements = new TokenRequirements();
        validateRequirements.setValidateTarget(token);

        TokenValidatorParameters validatorParameters = new TokenValidatorParameters();
        validatorParameters.setStsProperties(stsProperties);
        validatorParameters.setPrincipal(principal);
        validatorParameters.setMessageContext(messageContext);
        validatorParameters.setTokenStore(getTokenStore());
        validatorParameters.setKeyRequirements(null);
        validatorParameters.setTokenRequirements(validateRequirements);
        validatorParameters.setToken(token);

        if (tokenValidators.isEmpty()) {
            LOG.fine("No token validators have been configured to validate the received token");
        }

        TokenValidatorResponse tokenResponse = null;
        for (TokenValidator tokenValidator : tokenValidators) {
            final boolean canHandle;
            if (realm == null) {
                canHandle = tokenValidator.canHandleToken(token);
            } else {
                canHandle = tokenValidator.canHandleToken(token, realm);
            }
            if (canHandle) {
                try {
                    tokenResponse = tokenValidator.validateToken(validatorParameters);
                    token = tokenResponse.getToken();
                    // The parsed principal/roles is set if available. It's up to other
                    // components to deal with the STATE of the validation
                    token.setPrincipal(tokenResponse.getPrincipal());
                    token.setRoles(tokenResponse.getRoles());
                } catch (RuntimeException ex) {
                    LOG.log(Level.WARNING, "Failed to validate the token", ex);
                    token.setState(STATE.INVALID);
                }
                break;
            }
        }

        if (tokenResponse == null) {
            LOG.fine("No token validator has been configured to validate the received token");
        }
        return tokenResponse;
    }

    protected void performDelegationHandling(
        RequestRequirements requestRequirements, Principal principal,
        Map<String, Object> messageContext, ReceivedToken token,
        Principal tokenPrincipal, Set<Principal> tokenRoles
    ) {
        TokenDelegationParameters delegationParameters = new TokenDelegationParameters();
        delegationParameters.setStsProperties(stsProperties);
        delegationParameters.setPrincipal(principal);
        delegationParameters.setMessageContext(messageContext);
        delegationParameters.setTokenStore(getTokenStore());
        delegationParameters.setTokenPrincipal(tokenPrincipal);
        delegationParameters.setTokenRoles(tokenRoles);

        KeyRequirements keyRequirements = requestRequirements.getKeyRequirements();
        TokenRequirements tokenRequirements = requestRequirements.getTokenRequirements();
        delegationParameters.setKeyRequirements(keyRequirements);
        delegationParameters.setTokenRequirements(tokenRequirements);

        // Extract AppliesTo
        String address = extractAddressFromAppliesTo(tokenRequirements.getAppliesTo());
        delegationParameters.setAppliesToAddress(address);

        delegationParameters.setToken(token);

        TokenDelegationResponse tokenResponse = null;
        for (TokenDelegationHandler delegationHandler : delegationHandlers) {
            if (delegationHandler.canHandleToken(token)) {
                try {
                    tokenResponse = delegationHandler.isDelegationAllowed(delegationParameters);
                } catch (RuntimeException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw new STSException("Error in delegation handling", ex, STSException.REQUEST_FAILED);
                }
                break;
            }
        }

        if (tokenResponse == null || !tokenResponse.isDelegationAllowed()) {
            LOG.log(Level.WARNING, "No matching token delegation handler found");
            throw new STSException(
                "No matching token delegation handler found",
                STSException.REQUEST_FAILED
            );
        }
    }

    protected void processValidToken(TokenProviderParameters providerParameters,
            ReceivedToken validatedToken, TokenValidatorResponse tokenResponse) {
        // Map the principal (if it exists)
        Principal responsePrincipal = tokenResponse.getPrincipal();
        if (responsePrincipal != null) {
            String targetRealm = providerParameters.getRealm();
            String sourceRealm = tokenResponse.getTokenRealm();

            if (sourceRealm != null && targetRealm != null && !sourceRealm.equals(targetRealm)) {
                RelationshipResolver relRes = stsProperties.getRelationshipResolver();
                Relationship relationship = null;
                if (relRes != null) {
                    relationship = relRes.resolveRelationship(sourceRealm, targetRealm);
                    if (relationship != null) {
                        tokenResponse.getAdditionalProperties().put(
                                Relationship.class.getName(), relationship);
                    }
                }
                if (relationship == null || relationship.getType().equals(Relationship.FED_TYPE_IDENTITY)) {
                    // federate identity
                    final IdentityMapper identityMapper;
                    if (relationship == null) {
                        identityMapper = stsProperties.getIdentityMapper();
                    } else {
                        identityMapper = relationship.getIdentityMapper();
                    }
                    if (identityMapper != null) {
                        Principal targetPrincipal =
                            identityMapper.mapPrincipal(sourceRealm, responsePrincipal, targetRealm);
                        validatedToken.setPrincipal(targetPrincipal);
                    } else {
                        LOG.log(Level.SEVERE,
                                "No IdentityMapper configured in STSProperties or Relationship");
                        throw new STSException("Error in providing a token", STSException.REQUEST_FAILED);
                    }
                } else if (relationship.getType().equals(Relationship.FED_TYPE_CLAIMS)) {
                    // federate claims
                    // Claims are transformed at the time when the claims are required to create a token
                    // (ex. ClaimsAttributeStatementProvider)
                    // principal remains unchanged

                } else  {
                    LOG.log(Level.SEVERE, "Unknown federation type: " + relationship.getType());
                    throw new STSException("Error in providing a token", STSException.BAD_REQUEST);
                }
            }
        }
    }

    public void setEventListener(STSEventListener eventListener) {
        this.eventPublisher = eventListener;
    }


    protected void publishEvent(AbstractSTSEvent event) {
        if (eventPublisher != null) {
            eventPublisher.handleSTSEvent(event);
        }
    }

    protected static org.apache.xml.security.stax.securityToken.SecurityToken
    findInboundSecurityToken(SecurityEventConstants.Event event,
                             Map<String, Object> messageContext) throws XMLSecurityException {
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingEventList =
            (List<SecurityEvent>) messageContext.get(SecurityEvent.class.getName() + ".in");
        if (incomingEventList != null) {
            for (SecurityEvent incomingEvent : incomingEventList) {
                if (event == incomingEvent.getSecurityEventType()) {
                    return ((TokenSecurityEvent<?>)incomingEvent).getSecurityToken();
                }
            }
        }
        return null;
    }
}
