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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.RealmParser;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.RequestParser;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenReference;
import org.apache.cxf.sts.token.renewer.TokenRenewer;
import org.apache.cxf.sts.token.renewer.TokenRenewerParameters;
import org.apache.cxf.sts.token.renewer.TokenRenewerResponse;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.BinarySecretType;
import org.apache.cxf.ws.security.sts.provider.model.EntropyType;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedProofTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.ws.security.WSSecurityException;

/**
 * An implementation of the IssueOperation interface to renew tokens.
 */
public class TokenRenewOperation extends AbstractOperation implements RenewOperation {

    private static final Logger LOG = LogUtils.getL7dLogger(TokenRenewOperation.class);

    private List<TokenRenewer> tokenRenewers = new ArrayList<TokenRenewer>();
    private boolean allowRenewalBeforeExpiry;
    
    public boolean isAllowRenewalBeforeExpiry() {
        return allowRenewalBeforeExpiry;
    }

    public void setAllowRenewalBeforeExpiry(boolean allowRenewalBeforeExpiry) {
        this.allowRenewalBeforeExpiry = allowRenewalBeforeExpiry;
    }

    public void setTokenRenewers(List<TokenRenewer> tokenRenewerList) {
        this.tokenRenewers = tokenRenewerList;
    }
    
    public List<TokenRenewer> getTokenRenewers() {
        return tokenRenewers;
    }

    public RequestSecurityTokenResponseCollectionType renew(
        RequestSecurityTokenCollectionType requestCollection, WebServiceContext context
    ) {
        RequestSecurityTokenResponseCollectionType responseCollection = 
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponseCollectionType();
        for (RequestSecurityTokenType request : requestCollection.getRequestSecurityToken()) {
            RequestSecurityTokenResponseType response = renew(request, context);
            responseCollection.getRequestSecurityTokenResponse().add(response);
        }
        return responseCollection;
    }
    
    public RequestSecurityTokenResponseType renew(
        RequestSecurityTokenType request, WebServiceContext context
    ) {
        RequestParser requestParser = parseRequest(request, context);

        KeyRequirements keyRequirements = requestParser.getKeyRequirements();
        TokenRequirements tokenRequirements = requestParser.getTokenRequirements();
        
        ReceivedToken renewTarget = tokenRequirements.getRenewTarget();
        if (renewTarget == null || renewTarget.getToken() == null) {
            throw new STSException("No element presented for renewal", STSException.INVALID_REQUEST);
        }
        if (tokenRequirements.getTokenType() == null) {
            LOG.fine("Received TokenType is null");
        }
        
        // Get the realm of the request
        String realm = null;
        if (stsProperties.getRealmParser() != null) {
            RealmParser realmParser = stsProperties.getRealmParser();
            realm = realmParser.parseRealm(context);
        }
        
        // Validate the request
        TokenValidatorResponse tokenResponse = validateReceivedToken(
                context, realm, tokenRequirements, renewTarget);
        
        if (tokenResponse == null) {
            LOG.fine("No Token Validator has been found that can handle this token");
            renewTarget.setState(STATE.INVALID);
            throw new STSException(
                "No Token Validator has been found that can handle this token" 
                + tokenRequirements.getTokenType(), 
                STSException.REQUEST_FAILED
            );
        }
        
        // Reject a non-expired token (valid or invalid) by default
        if (tokenResponse.getToken().getState() != STATE.EXPIRED
            && !(allowRenewalBeforeExpiry && tokenResponse.getToken().getState() == STATE.VALID)) {
            LOG.fine("The token is not expired, and so it cannot be renewed");
            throw new STSException(
                "No Token Validator has been found that can handle this token" 
                + tokenRequirements.getTokenType(), 
                STSException.REQUEST_FAILED
            );
        }
        
        //
        // Renew the token
        //
        TokenRenewerResponse tokenRenewerResponse = null;
        TokenRenewerParameters renewerParameters = createTokenRenewerParameters(requestParser, context);
        Map<String, Object> additionalProperties = tokenResponse.getAdditionalProperties();
        if (additionalProperties != null) {
            renewerParameters.setAdditionalProperties(additionalProperties);
        }
        renewerParameters.setRealm(tokenResponse.getTokenRealm());
        renewerParameters.setToken(tokenResponse.getToken());

        realm = tokenResponse.getTokenRealm();
        for (TokenRenewer tokenRenewer : tokenRenewers) {
            boolean canHandle = false;
            if (realm == null) {
                canHandle = tokenRenewer.canHandleToken(tokenResponse.getToken());
            } else {
                canHandle = tokenRenewer.canHandleToken(tokenResponse.getToken(), realm);
            }
            if (canHandle) {
                try {
                    tokenRenewerResponse = tokenRenewer.renewToken(renewerParameters);
                } catch (STSException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw ex;
                } catch (RuntimeException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw new STSException(
                        "Error in providing a token", ex, STSException.REQUEST_FAILED
                    );
                }
                break;
            }
        }
        if (tokenRenewerResponse == null || tokenRenewerResponse.getToken() == null) {
            LOG.fine("No Token Renewer has been found that can handle this token");
            throw new STSException(
                "No token renewer found for requested token type", STSException.REQUEST_FAILED
            );
        }

        // prepare response
        try {
            EncryptionProperties encryptionProperties = renewerParameters.getEncryptionProperties();
            RequestSecurityTokenResponseType response = 
                createResponse(
                    encryptionProperties, tokenRenewerResponse, tokenRequirements, keyRequirements, context
                );
            return response;
        } catch (Throwable ex) {
            LOG.log(Level.WARNING, "", ex);
            throw new STSException("Error in creating the response", ex, STSException.REQUEST_FAILED);
        }
    }
   
    private RequestSecurityTokenResponseType createResponse(
            EncryptionProperties encryptionProperties,
            TokenRenewerResponse tokenRenewerResponse, 
            TokenRequirements tokenRequirements,
            KeyRequirements keyRequirements,
            WebServiceContext webServiceContext
    ) throws WSSecurityException {
        RequestSecurityTokenResponseType response = 
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponseType();

        String context = tokenRequirements.getContext();
        if (context != null) {
            response.setContext(context);
        }

        // TokenType
        JAXBElement<String> jaxbTokenType = 
            QNameConstants.WS_TRUST_FACTORY.createTokenType(tokenRequirements.getTokenType());
        response.getAny().add(jaxbTokenType);

        // RequestedSecurityToken
        RequestedSecurityTokenType requestedTokenType = 
            QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityTokenType();
        JAXBElement<RequestedSecurityTokenType> requestedToken = 
            QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(requestedTokenType);
        LOG.fine("Encrypting Issued Token: " + encryptIssuedToken);
        if (!encryptIssuedToken) {
            requestedTokenType.setAny(tokenRenewerResponse.getToken());
        } else {
            requestedTokenType.setAny(
                encryptToken(
                    tokenRenewerResponse.getToken(), tokenRenewerResponse.getTokenId(), 
                    encryptionProperties, keyRequirements, webServiceContext
                )
            );
        }
        response.getAny().add(requestedToken);

        if (returnReferences) {
            // RequestedAttachedReference
            TokenReference attachedReference = tokenRenewerResponse.getAttachedReference();
            RequestedReferenceType requestedAttachedReferenceType = null;
            if (attachedReference != null) {
                requestedAttachedReferenceType = createRequestedReference(attachedReference, true);
            } else {
                requestedAttachedReferenceType = 
                    createRequestedReference(
                            tokenRenewerResponse.getTokenId(), tokenRequirements.getTokenType(), true
                    );
            }

            JAXBElement<RequestedReferenceType> requestedAttachedReference = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedAttachedReference(
                        requestedAttachedReferenceType
                );
            response.getAny().add(requestedAttachedReference);

            // RequestedUnattachedReference
            TokenReference unAttachedReference = tokenRenewerResponse.getUnAttachedReference();
            RequestedReferenceType requestedUnattachedReferenceType = null;
            if (unAttachedReference != null) {
                requestedUnattachedReferenceType = createRequestedReference(unAttachedReference, false);
            } else {
                requestedUnattachedReferenceType = 
                    createRequestedReference(
                            tokenRenewerResponse.getTokenId(), tokenRequirements.getTokenType(), false
                    );
            }

            JAXBElement<RequestedReferenceType> requestedUnattachedReference = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedUnattachedReference(
                        requestedUnattachedReferenceType
                );
            response.getAny().add(requestedUnattachedReference);
        }

        // AppliesTo
        response.getAny().add(tokenRequirements.getAppliesTo());

        // RequestedProofToken
        if (tokenRenewerResponse.isComputedKey() && keyRequirements.getComputedKeyAlgorithm() != null) {
            JAXBElement<String> computedKey = 
                QNameConstants.WS_TRUST_FACTORY.createComputedKey(keyRequirements.getComputedKeyAlgorithm());
            RequestedProofTokenType requestedProofTokenType = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedProofTokenType();
            requestedProofTokenType.setAny(computedKey);
            JAXBElement<RequestedProofTokenType> requestedProofToken = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedProofToken(requestedProofTokenType);
            response.getAny().add(requestedProofToken);
        } else if (tokenRenewerResponse.getEntropy() != null) {
            Object token = 
                constructSecretToken(tokenRenewerResponse.getEntropy(), encryptionProperties, keyRequirements);
            RequestedProofTokenType requestedProofTokenType = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedProofTokenType();
            requestedProofTokenType.setAny(token);
            JAXBElement<RequestedProofTokenType> requestedProofToken = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedProofToken(requestedProofTokenType);
            response.getAny().add(requestedProofToken);
        }

        // Entropy
        if (tokenRenewerResponse.isComputedKey() && tokenRenewerResponse.getEntropy() != null) {
            Object token = 
                constructSecretToken(tokenRenewerResponse.getEntropy(), encryptionProperties, keyRequirements);
            EntropyType entropyType = QNameConstants.WS_TRUST_FACTORY.createEntropyType();
            entropyType.getAny().add(token);
            JAXBElement<EntropyType> entropyElement = 
                QNameConstants.WS_TRUST_FACTORY.createEntropy(entropyType);
            response.getAny().add(entropyElement);
        }

        // Lifetime
        LifetimeType lifetime = createLifetime(tokenRenewerResponse.getLifetime());
        JAXBElement<LifetimeType> lifetimeType = QNameConstants.WS_TRUST_FACTORY.createLifetime(lifetime);
        response.getAny().add(lifetimeType);

        // KeySize
        long keySize = tokenRenewerResponse.getKeySize();
        if (keySize <= 0) {
            keySize = keyRequirements.getKeySize();
        }
        if (keyRequirements.getKeySize() > 0) {
            JAXBElement<Long> keySizeType = 
                QNameConstants.WS_TRUST_FACTORY.createKeySize(keySize);
            response.getAny().add(keySizeType);
        }

        return response;
    }

    /**
     * Construct a token containing the secret to return to the client. If encryptIssuedToken is set
     * then the token is wrapped in an EncryptedKey DOM element, otherwise it is returned in a 
     * BinarySecretType JAXBElement.
     */
    private Object constructSecretToken(
            byte[] secret,
            EncryptionProperties encryptionProperties, 
            KeyRequirements keyRequirements
    ) throws WSSecurityException {
        if (encryptIssuedToken) {
            return encryptSecret(secret, encryptionProperties, keyRequirements);
        } else {
            BinarySecretType binarySecretType = QNameConstants.WS_TRUST_FACTORY.createBinarySecretType();
            String nonce = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Nonce";
            binarySecretType.setType(nonce);
            binarySecretType.setValue(secret);
            JAXBElement<BinarySecretType> binarySecret = 
                QNameConstants.WS_TRUST_FACTORY.createBinarySecret(binarySecretType);
            return binarySecret;
        }
    }

    private TokenRenewerParameters createTokenRenewerParameters(
        RequestParser requestParser, WebServiceContext context
    ) {
        TokenProviderParameters providerParameters = 
            createTokenProviderParameters(requestParser, context);
        
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress(providerParameters.getAppliesToAddress());
        renewerParameters.setEncryptionProperties(providerParameters.getEncryptionProperties());
        renewerParameters.setKeyRequirements(providerParameters.getKeyRequirements());
        renewerParameters.setPrincipal(providerParameters.getPrincipal());
        renewerParameters.setStsProperties(providerParameters.getStsProperties());
        renewerParameters.setTokenRequirements(providerParameters.getTokenRequirements());
        renewerParameters.setTokenStore(providerParameters.getTokenStore());
        renewerParameters.setWebServiceContext(providerParameters.getWebServiceContext());
        
        return renewerParameters;
    }

}
