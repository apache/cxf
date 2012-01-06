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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.RealmParser;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.cache.STSTokenStore;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.RequestParser;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenReference;
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

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.WSSecEncrypt;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.util.XmlSchemaDateFormat;



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
    protected List<TokenProvider> tokenProviders = new ArrayList<TokenProvider>();
    protected List<TokenValidator> tokenValidators = new ArrayList<TokenValidator>();
    protected boolean returnReferences = true;
    protected STSTokenStore tokenStore;
    
    public boolean isReturnReferences() {
        return returnReferences;
    }

    public void setReturnReferences(boolean returnReferences) {
        this.returnReferences = returnReferences;
    }
    
    public STSTokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(STSTokenStore tokenStore) {
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
    
    public List<TokenProvider> getTokenProviders() {
        return tokenProviders;
    }

    public void setTokenValidators(List<TokenValidator> tokenValidators) {
        this.tokenValidators = tokenValidators;
    }

    public List<TokenValidator> getTokenValidators() {
        return tokenValidators;
    }  
    
    /**
     * Check the arguments from the STSProvider and parse the request.
     */
    protected RequestParser parseRequest(
        RequestSecurityTokenType request,
        WebServiceContext context
    ) {
        if (context == null || context.getMessageContext() == null) {
            throw new STSException("No message context found");
        }
        
        if (stsProperties == null) {
            throw new STSException("No STSProperties object found");
        }
        stsProperties.configureProperties();
        
        RequestParser requestParser = new RequestParser();
        requestParser.parseRequest(request, context);
        
        return requestParser;
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
        
        // Create the identifier according to whether it is an attached reference or not
        String identifier = tokenReference.getIdentifier();
        if (attached && identifier.charAt(0) != '#') {
            identifier = "#" + identifier;
        } else if (!attached && identifier.charAt(0) == '#') {
            identifier = identifier.substring(1);
        }
        
        // TokenType
        String tokenType = tokenReference.getWsse11TokenType();
        if (tokenType != null) {
            securityTokenReferenceType.getOtherAttributes().put(TOKEN_TYPE, tokenType);
        }
        
        if (tokenReference.isUseKeyIdentifier()) {
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
        
        if (WSConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType) 
            || WSConstants.SAML_NS.equals(tokenType)) {
            tokenReference.setWsse11TokenType(WSConstants.WSS_SAML_TOKEN_TYPE);
            tokenReference.setUseKeyIdentifier(true);
            tokenReference.setWsseValueType(WSConstants.WSS_SAML_KI_VALUE_TYPE);
        } else if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
            || WSConstants.SAML2_NS.equals(tokenType)) {
            tokenReference.setWsse11TokenType(WSConstants.WSS_SAML2_TOKEN_TYPE);
            tokenReference.setUseKeyIdentifier(true);
            tokenReference.setWsseValueType(WSConstants.WSS_SAML2_KI_VALUE_TYPE);
        } else {
            tokenReference.setUseDirectReference(true);
            tokenReference.setWsseValueType(tokenType);
        }
        
        return createRequestedReference(tokenReference, attached);
    }
    
    /**
     * Create a LifetimeType object given a lifetime in seconds
     */
    protected static LifetimeType createLifetime(long lifetime) {
        AttributedDateTime created = QNameConstants.UTIL_FACTORY.createAttributedDateTime();
        AttributedDateTime expires = QNameConstants.UTIL_FACTORY.createAttributedDateTime();
        
        Date creationTime = new Date();
        Date expirationTime = new Date();
        if (lifetime <= 0) {
            lifetime = 300L;
        }
        expirationTime.setTime(creationTime.getTime() + (lifetime * 1000L));

        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        created.setValue(fmt.format(creationTime));
        LOG.fine("Token lifetime creation: " + created.getValue());
        expires.setValue(fmt.format(expirationTime));
        LOG.fine("Token lifetime expiration: " + expires.getValue());
        
        LifetimeType lifetimeType = QNameConstants.WS_TRUST_FACTORY.createLifetimeType();
        lifetimeType.setCreated(created);
        lifetimeType.setExpires(expires);
        return lifetimeType;
    }
    
    /**
     * Encrypt a Token element using the given arguments.
     */
    protected Element encryptToken(
        Element element, 
        String id, 
        EncryptionProperties encryptionProperties,
        KeyRequirements keyRequirements,
        WebServiceContext context
    ) throws WSSecurityException {
        String name = encryptionProperties.getEncryptionName();
        if (name == null) {
            name = stsProperties.getEncryptionUsername();
        }
        if (name == null) {
            throw new STSException("No encryption alias is configured", STSException.REQUEST_FAILED);
        }
        
        // Get the encryption algorithm to use
        String encryptionAlgorithm = keyRequirements.getEncryptionAlgorithm();
        if (encryptionAlgorithm == null) {
            // If none then default to what is configured
            encryptionAlgorithm = encryptionProperties.getEncryptionAlgorithm();
        } else {
            List<String> supportedAlgorithms = 
                encryptionProperties.getAcceptedEncryptionAlgorithms();
            if (!supportedAlgorithms.contains(encryptionAlgorithm)) {
                encryptionAlgorithm = encryptionProperties.getEncryptionAlgorithm();
                LOG.fine("EncryptionAlgorithm not supported, defaulting to: " + encryptionAlgorithm);
            }
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
                LOG.fine("KeyWrapAlgorithm not supported, defaulting to: " + keyWrapAlgorithm);
            }
        }
        
        WSSecEncrypt builder = new WSSecEncrypt();
        if (WSHandlerConstants.USE_REQ_SIG_CERT.equals(name)) {
            X509Certificate cert = getReqSigCert(context.getMessageContext());
            builder.setUseThisCert(cert);
        } else {
            builder.setUserInfo(name);
        }
        builder.setKeyIdentifierType(encryptionProperties.getKeyIdentifierType());
        builder.setSymmetricEncAlgorithm(encryptionAlgorithm);
        builder.setKeyEncAlgo(keyWrapAlgorithm);
        builder.setEmbedEncryptedKey(true);
        
        WSEncryptionPart encryptionPart = new WSEncryptionPart(id, "Element");
        encryptionPart.setElement(element);
        
        Document doc = element.getOwnerDocument();
        doc.appendChild(element);
                                 
        builder.prepare(element.getOwnerDocument(), stsProperties.getEncryptionCrypto());
        builder.encryptForRef(null, Collections.singletonList(encryptionPart));
        
        return doc.getDocumentElement();
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
                LOG.fine("KeyWrapAlgorithm not supported, defaulting to: " + keyWrapAlgorithm);
            }
        }
        
        WSSecEncryptedKey builder = new WSSecEncryptedKey();
        builder.setUserInfo(name);
        builder.setKeyIdentifierType(encryptionProperties.getKeyIdentifierType());
        builder.setEphemeralKey(secret);
        builder.setKeyEncAlgo(keyWrapAlgorithm);
        
        Document doc = DOMUtils.createDocument();
                                 
        builder.prepare(doc, stsProperties.getEncryptionCrypto());
        
        return builder.getEncryptedKeyElement();
    }

    /**
     * Extract an address from an AppliesTo DOM element
     */
    protected static String extractAddressFromAppliesTo(Element appliesTo) {
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
            }
        }
        LOG.fine("AppliesTo element does not exist or could not be parsed");
        return null;
    }

    /**
     * Create a TokenProviderParameters object given a RequestParser and WebServiceContext object
     */
    protected TokenProviderParameters createTokenProviderParameters(
        RequestParser requestParser, WebServiceContext context
    ) {
        TokenProviderParameters providerParameters = new TokenProviderParameters();
        providerParameters.setStsProperties(stsProperties);
        providerParameters.setPrincipal(context.getUserPrincipal());
        providerParameters.setWebServiceContext(context);
        providerParameters.setTokenStore(getTokenStore());
        
        KeyRequirements keyRequirements = requestParser.getKeyRequirements();
        TokenRequirements tokenRequirements = requestParser.getTokenRequirements();
        providerParameters.setKeyRequirements(keyRequirements);
        providerParameters.setTokenRequirements(tokenRequirements);
        
        // Extract AppliesTo
        String address = extractAddressFromAppliesTo(tokenRequirements.getAppliesTo());
        LOG.fine("The AppliesTo address that has been received is: " + address);
        providerParameters.setAppliesToAddress(address);
        
        // Get the realm of the request
        if (stsProperties.getRealmParser() != null) {
            RealmParser realmParser = stsProperties.getRealmParser();
            String realm = realmParser.parseRealm(context);
            providerParameters.setRealm(realm);
        }
        
        // Set the requested Claims
        RequestClaimCollection claims = tokenRequirements.getClaims();
        providerParameters.setRequestedClaims(claims);
        
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
                LOG.log(Level.WARNING, "The Service cannot match the received AppliesTo address");
                throw new STSException(
                    "No service corresponding to " + address + " is known", STSException.REQUEST_FAILED
                );
            }
        }
        
        providerParameters.setEncryptionProperties(encryptionProperties);
        
        return providerParameters;
    }
    
    /**
     * Get the X509Certificate associated with the signature that was received. This cert is to be used
     * for encrypting the issued token.
     */
    private X509Certificate getReqSigCert(MessageContext context) {
        @SuppressWarnings("unchecked")
        List<WSHandlerResult> results = 
            (List<WSHandlerResult>) context.get(WSHandlerConstants.RECV_RESULTS);
        if (results != null) {
            for (WSHandlerResult rResult : results) {
                List<WSSecurityEngineResult> wsSecEngineResults = rResult.getResults();
                for (WSSecurityEngineResult wser : wsSecEngineResults) {
                    int wserAction = 
                        ((java.lang.Integer)wser.get(WSSecurityEngineResult.TAG_ACTION)).intValue();
                    if (wserAction == WSConstants.SIGN) {
                        X509Certificate cert = 
                            (X509Certificate)wser.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                        if (cert != null) {
                            return cert;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    protected TokenValidatorResponse validateReceivedToken(
            WebServiceContext context, String realm,
            TokenRequirements tokenRequirements, ReceivedToken token) {
        token.setValidationState(STATE.NONE);
        
        tokenRequirements.setValidateTarget(token);

        TokenValidatorParameters validatorParameters = new TokenValidatorParameters();
        validatorParameters.setStsProperties(stsProperties);
        validatorParameters.setPrincipal(context.getUserPrincipal());
        validatorParameters.setWebServiceContext(context);
        validatorParameters.setTokenStore(getTokenStore());
        validatorParameters.setKeyRequirements(null);
        validatorParameters.setTokenRequirements(tokenRequirements);

        TokenValidatorResponse tokenResponse = null;
        for (TokenValidator tokenValidator : tokenValidators) {
            boolean canHandle = false;
            if (realm == null) {
                canHandle = tokenValidator.canHandleToken(token);
            } else {
                canHandle = tokenValidator.canHandleToken(token, realm);
            }
            if (canHandle) {
                try {
                    tokenResponse = tokenValidator.validateToken(validatorParameters);
                    token.setValidationState(
                            tokenResponse.isValid() ? STATE.VALID : STATE.INVALID
                    );
                    // The parsed principal is set if available. It's up to other components to
                    // deal with the STATE of the validation
                    token.setPrincipal(tokenResponse.getPrincipal());
                } catch (RuntimeException ex) {
                    LOG.log(Level.WARNING, "Failed to validate the token", ex);
                    token.setValidationState(STATE.INVALID);
                }
                break;
            }
        }
        return tokenResponse;
    }
    
    
}
