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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.IdentityMapper;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.RequestParser;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.provider.TokenReference;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.BinarySecretType;
import org.apache.cxf.ws.security.sts.provider.model.EntropyType;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedProofTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueSingleOperation;
import org.apache.ws.security.WSSecurityException;

/**
 * An implementation of the IssueOperation interface.
 */
public class TokenIssueOperation extends AbstractOperation implements IssueOperation, IssueSingleOperation {

    private static final Logger LOG = LogUtils.getL7dLogger(TokenIssueOperation.class);


    public RequestSecurityTokenResponseCollectionType issue(
            RequestSecurityTokenType request,
            WebServiceContext context
    ) {
        RequestSecurityTokenResponseType response = issueSingle(request, context);
        RequestSecurityTokenResponseCollectionType responseCollection = 
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponseCollectionType();
        responseCollection.getRequestSecurityTokenResponse().add(response);
        return responseCollection;
    }

    public RequestSecurityTokenResponseType issueSingle(
            RequestSecurityTokenType request,
            WebServiceContext context
    ) {
        RequestParser requestParser = parseRequest(request, context);

        TokenProviderParameters providerParameters = createTokenProviderParameters(requestParser, context);

        // Check if the requested claims can be handled by the configured claim handlers
        RequestClaimCollection requestedClaims = providerParameters.getRequestedClaims();
        checkClaimsSupport(requestedClaims);
        providerParameters.setClaimsManager(claimsManager);
        
        String realm = providerParameters.getRealm();

        TokenRequirements tokenRequirements = requestParser.getTokenRequirements();
        String tokenType = tokenRequirements.getTokenType();


        // Validate OnBehalfOf token if present
        if (providerParameters.getTokenRequirements().getOnBehalfOf() != null) {
            ReceivedToken validateTarget = providerParameters.getTokenRequirements().getOnBehalfOf();
            TokenValidatorResponse tokenResponse = validateReceivedToken(
                    context, realm, tokenRequirements, validateTarget);
            
            if (tokenResponse == null) {
                LOG.fine("No Token Validator has been found that can handle this token");

            } else if (validateTarget.getValidationState().equals(STATE.VALID)) {
                // Map the principal (if it exists)
                Principal responsePrincipal = tokenResponse.getPrincipal();
                if (responsePrincipal != null) {
                    String targetRealm = providerParameters.getRealm();
                    String sourceRealm = tokenResponse.getTokenRealm();
                    IdentityMapper identityMapper = stsProperties.getIdentityMapper();
                    if (sourceRealm != null && !sourceRealm.equals(targetRealm) && identityMapper != null) {
                        Principal targetPrincipal = 
                            identityMapper.mapPrincipal(sourceRealm, responsePrincipal, targetRealm);
                        validateTarget.setPrincipal(targetPrincipal);
                    }
                } 
            } else {
                //[TODO] Add plugin for validation out-of-band
                // Example:
                // If the requestor is in the possession of a certificate (mutual ssl handshake)
                // the STS trusts the token sent in OnBehalfOf element
            }
            if (tokenResponse != null) {
                Map<String, Object> additionalProperties = tokenResponse.getAdditionalProperties();
                if (additionalProperties != null) {
                    providerParameters.setAdditionalProperties(additionalProperties);
                }
            }
        }

        // create token
        TokenProviderResponse tokenResponse = null;
        for (TokenProvider tokenProvider : tokenProviders) {
            boolean canHandle = false;
            if (realm == null) {
                canHandle = tokenProvider.canHandleToken(tokenType);
            } else {
                canHandle = tokenProvider.canHandleToken(tokenType, realm);
            }
            if (canHandle) {
                try {
                    tokenResponse = tokenProvider.createToken(providerParameters);
                } catch (STSException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw ex;
                } catch (RuntimeException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw new STSException("Error in providing a token", ex, STSException.REQUEST_FAILED);
                }
                break;
            }
        }
        if (tokenResponse == null || tokenResponse.getToken() == null) {
            LOG.log(Level.WARNING, "No token provider found for requested token type: " + tokenType);
            throw new STSException(
                    "No token provider found for requested token type: " + tokenType, 
                    STSException.REQUEST_FAILED
            );
        }
        // prepare response
        try {
            KeyRequirements keyRequirements = requestParser.getKeyRequirements();
            EncryptionProperties encryptionProperties = providerParameters.getEncryptionProperties();
            RequestSecurityTokenResponseType response = 
                createResponse(
                        encryptionProperties, tokenResponse, tokenRequirements, keyRequirements, context
                );
            return response;
        } catch (Throwable ex) {
            LOG.log(Level.WARNING, "", ex);
            throw new STSException("Error in creating the response", ex, STSException.REQUEST_FAILED);
        }
    }

    private RequestSecurityTokenResponseType createResponse(
            EncryptionProperties encryptionProperties,
            TokenProviderResponse tokenResponse, 
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
            requestedTokenType.setAny(tokenResponse.getToken());
        } else {
            requestedTokenType.setAny(
                encryptToken(
                    tokenResponse.getToken(), tokenResponse.getTokenId(), 
                    encryptionProperties, keyRequirements, webServiceContext
                )
            );
        }
        response.getAny().add(requestedToken);

        if (returnReferences) {
            // RequestedAttachedReference
            TokenReference attachedReference = tokenResponse.getAttachedReference();
            RequestedReferenceType requestedAttachedReferenceType = null;
            if (attachedReference != null) {
                requestedAttachedReferenceType = createRequestedReference(attachedReference, true);
            } else {
                requestedAttachedReferenceType = 
                    createRequestedReference(
                            tokenResponse.getTokenId(), tokenRequirements.getTokenType(), true
                    );
            }

            JAXBElement<RequestedReferenceType> requestedAttachedReference = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedAttachedReference(
                        requestedAttachedReferenceType
                );
            response.getAny().add(requestedAttachedReference);

            // RequestedUnattachedReference
            TokenReference unAttachedReference = tokenResponse.getUnAttachedReference();
            RequestedReferenceType requestedUnattachedReferenceType = null;
            if (unAttachedReference != null) {
                requestedUnattachedReferenceType = createRequestedReference(unAttachedReference, false);
            } else {
                requestedUnattachedReferenceType = 
                    createRequestedReference(
                            tokenResponse.getTokenId(), tokenRequirements.getTokenType(), false
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
        if (tokenResponse.isComputedKey() && keyRequirements.getComputedKeyAlgorithm() != null) {
            JAXBElement<String> computedKey = 
                QNameConstants.WS_TRUST_FACTORY.createComputedKey(keyRequirements.getComputedKeyAlgorithm());
            RequestedProofTokenType requestedProofTokenType = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedProofTokenType();
            requestedProofTokenType.setAny(computedKey);
            JAXBElement<RequestedProofTokenType> requestedProofToken = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedProofToken(requestedProofTokenType);
            response.getAny().add(requestedProofToken);
        } else if (tokenResponse.getEntropy() != null) {
            Object token = 
                constructSecretToken(tokenResponse.getEntropy(), encryptionProperties, keyRequirements);
            RequestedProofTokenType requestedProofTokenType = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedProofTokenType();
            requestedProofTokenType.setAny(token);
            JAXBElement<RequestedProofTokenType> requestedProofToken = 
                QNameConstants.WS_TRUST_FACTORY.createRequestedProofToken(requestedProofTokenType);
            response.getAny().add(requestedProofToken);
        }

        // Entropy
        if (tokenResponse.isComputedKey() && tokenResponse.getEntropy() != null) {
            Object token = 
                constructSecretToken(tokenResponse.getEntropy(), encryptionProperties, keyRequirements);
            EntropyType entropyType = QNameConstants.WS_TRUST_FACTORY.createEntropyType();
            entropyType.getAny().add(token);
            JAXBElement<EntropyType> entropyElement = 
                QNameConstants.WS_TRUST_FACTORY.createEntropy(entropyType);
            response.getAny().add(entropyElement);
        }

        // Lifetime
        LifetimeType lifetime = createLifetime(tokenResponse.getLifetime());
        JAXBElement<LifetimeType> lifetimeType = QNameConstants.WS_TRUST_FACTORY.createLifetime(lifetime);
        response.getAny().add(lifetimeType);

        // KeySize
        long keySize = tokenResponse.getKeySize();
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
     * Construct a token containing the secret to return to the client. The secret is returned in a 
     * BinarySecretType JAXBElement.
     */
    private Object constructSecretToken(
            byte[] secret,
            EncryptionProperties encryptionProperties, 
            KeyRequirements keyRequirements
    ) throws WSSecurityException {
        /*if (encryptIssuedToken) {
            return encryptSecret(secret, encryptionProperties, keyRequirements);
        } else {
        */
        BinarySecretType binarySecretType = QNameConstants.WS_TRUST_FACTORY.createBinarySecretType();
        String nonce = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Nonce";
        binarySecretType.setType(nonce);
        binarySecretType.setValue(secret);
        JAXBElement<BinarySecretType> binarySecret = 
                QNameConstants.WS_TRUST_FACTORY.createBinarySecret(binarySecretType);
        return binarySecret;
    }

}
