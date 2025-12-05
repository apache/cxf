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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.event.STSIssueFailureEvent;
import org.apache.cxf.sts.event.STSIssueSuccessEvent;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.RequestRequirements;
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
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedProofTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueSingleOperation;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipal;
import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.securityToken.SamlSecurityToken;
import org.apache.xml.security.exceptions.XMLSecurityException;

/**
 * An implementation of the IssueOperation interface.
 */
public class TokenIssueOperation extends AbstractOperation implements IssueOperation, IssueSingleOperation {

    static final Logger LOG = LogUtils.getL7dLogger(TokenIssueOperation.class);


    public RequestSecurityTokenResponseCollectionType issue(
            RequestSecurityTokenType request,
            Principal principal,
            Map<String, Object> messageContext
    ) {
        RequestSecurityTokenResponseType response = issueSingle(request, principal, messageContext);
        RequestSecurityTokenResponseCollectionType responseCollection =
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponseCollectionType();
        responseCollection.getRequestSecurityTokenResponse().add(response);
        return responseCollection;
    }


    public RequestSecurityTokenResponseCollectionType issue(
            RequestSecurityTokenCollectionType requestCollection,
            Principal principal,
            Map<String, Object> messageContext
    ) {
        RequestSecurityTokenResponseCollectionType responseCollection =
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponseCollectionType();
        for (RequestSecurityTokenType request : requestCollection.getRequestSecurityToken()) {
            RequestSecurityTokenResponseType response = issueSingle(request, principal, messageContext);
            responseCollection.getRequestSecurityTokenResponse().add(response);
        }
        return responseCollection;
    }

    public RequestSecurityTokenResponseType issueSingle(
            RequestSecurityTokenType request,
            Principal principal,
            Map<String, Object> messageContext
    ) {
        long start = System.currentTimeMillis();
        TokenProviderParameters providerParameters = new TokenProviderParameters();
        try {
            RequestRequirements requestRequirements = parseRequest(request, messageContext);

            providerParameters = createTokenProviderParameters(requestRequirements, principal, messageContext);
            providerParameters.setClaimsManager(claimsManager);

            String realm = providerParameters.getRealm();

            TokenRequirements tokenRequirements = requestRequirements.getTokenRequirements();
            String tokenType = tokenRequirements.getTokenType();

            if (stsProperties.getSamlRealmCodec() != null) {
                SamlAssertionWrapper assertion = fetchSAMLAssertionFromWSSecuritySAMLToken(messageContext);

                if (assertion != null) {
                    String wssecRealm = stsProperties.getSamlRealmCodec().getRealmFromToken(assertion);
                    SAMLTokenPrincipal samlPrincipal = new SAMLTokenPrincipalImpl(assertion);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("SAML token realm of user '" + samlPrincipal.getName() + "' is " + wssecRealm);
                    }

                    ReceivedToken wssecToken = new ReceivedToken(assertion.getElement());
                    wssecToken.setState(STATE.VALID);
                    TokenValidatorResponse tokenResponse = new TokenValidatorResponse();
                    tokenResponse.setPrincipal(samlPrincipal);
                    tokenResponse.setToken(wssecToken);
                    tokenResponse.setTokenRealm(wssecRealm);
                    tokenResponse.setAdditionalProperties(new HashMap<String, Object>());
                    processValidToken(providerParameters, wssecToken, tokenResponse);
                    providerParameters.setPrincipal(wssecToken.getPrincipal());
                }
            }

            // Validate OnBehalfOf token if present
            if (providerParameters.getTokenRequirements().getOnBehalfOf() != null) {
                ReceivedToken validateTarget = providerParameters.getTokenRequirements().getOnBehalfOf();
                handleDelegationToken(validateTarget, providerParameters, principal, messageContext,
                                      realm, requestRequirements);
            }

            // See whether ActAs is allowed or not
            if (providerParameters.getTokenRequirements().getActAs() != null) {
                ReceivedToken validateTarget = providerParameters.getTokenRequirements().getActAs();
                handleDelegationToken(validateTarget, providerParameters, principal, messageContext,
                                      realm, requestRequirements);
            }

            // create token
            TokenProviderResponse tokenResponse = null;
            for (TokenProvider tokenProvider : tokenProviders) {
                final boolean canHandle;
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
                KeyRequirements keyRequirements = requestRequirements.getKeyRequirements();
                EncryptionProperties encryptionProperties = providerParameters.getEncryptionProperties();
                RequestSecurityTokenResponseType response =
                    createResponse(
                            encryptionProperties, tokenResponse, tokenRequirements, keyRequirements
                    );
                STSIssueSuccessEvent event = new STSIssueSuccessEvent(providerParameters,
                        System.currentTimeMillis() - start);
                publishEvent(event);

                cleanRequest(requestRequirements);
                return response;
            } catch (Throwable ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new STSException("Error in creating the response", ex, STSException.REQUEST_FAILED);
            }

        } catch (RuntimeException ex) {
            LOG.log(Level.SEVERE, "Cannot issue token: " + ex.getMessage(), ex);
            STSIssueFailureEvent event = new STSIssueFailureEvent(providerParameters,
                                                              System.currentTimeMillis() - start, ex);
            publishEvent(event);
            throw ex;
        }
    }

    private void handleDelegationToken(
        ReceivedToken validateTarget,
        TokenProviderParameters providerParameters,
        Principal principal,
        Map<String, Object> messageContext,
        String realm,
        RequestRequirements requestRequirements
    ) {
        TokenValidatorResponse tokenResponse = validateReceivedToken(
                principal, messageContext, realm, requestRequirements.getTokenRequirements(), validateTarget);

        if (tokenResponse == null) {
            LOG.fine("No Token Validator has been found that can handle this token");
        } else if (validateTarget.getState() == STATE.INVALID) {
            throw new STSException("Incoming token is invalid", STSException.REQUEST_FAILED);
        } else if (validateTarget.getState() == STATE.VALID) {
            processValidToken(providerParameters, validateTarget, tokenResponse);
        } else {
            //[TODO] Add plugin for validation out-of-band
            // Example:
            // If the requestor is in the possession of a certificate (mutual ssl handshake)
            // the STS trusts the token sent in OnBehalfOf element
        }

        Principal tokenPrincipal = null;
        Set<Principal> tokenRoles = null;

        if (tokenResponse != null) {
            Map<String, Object> additionalProperties = tokenResponse.getAdditionalProperties();
            if (additionalProperties != null) {
                providerParameters.setAdditionalProperties(additionalProperties);
            }
            tokenPrincipal = tokenResponse.getPrincipal();
            tokenRoles = tokenResponse.getRoles();
        }

        // See whether OnBehalfOf/ActAs is allowed or not
        performDelegationHandling(requestRequirements, principal, messageContext,
                                  validateTarget, tokenPrincipal, tokenRoles);
    }

    protected RequestSecurityTokenResponseType createResponse(
            EncryptionProperties encryptionProperties,
            TokenProviderResponse tokenResponse,
            TokenRequirements tokenRequirements,
            KeyRequirements keyRequirements
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
        tokenWrapper.wrapToken(tokenResponse.getToken(), requestedTokenType);
        response.getAny().add(requestedToken);

        if (returnReferences) {
            // RequestedAttachedReference
            TokenReference attachedReference = tokenResponse.getAttachedReference();
            final RequestedReferenceType requestedAttachedReferenceType;
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
            final RequestedReferenceType requestedUnattachedReferenceType;
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
        if (includeLifetimeElement) {
            LifetimeType lifetime =
                createLifetime(tokenResponse.getCreated(), tokenResponse.getExpires());
            JAXBElement<LifetimeType> lifetimeType =
                QNameConstants.WS_TRUST_FACTORY.createLifetime(lifetime);
            response.getAny().add(lifetimeType);
        }

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
     * Method to fetch SAML assertion from the WS-Security header
     */
    private static SamlAssertionWrapper fetchSAMLAssertionFromWSSecuritySAMLToken(
        Map<String, Object> messageContext
    ) {
        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>) messageContext.get(WSHandlerConstants.RECV_RESULTS));

        // Try DOM results first
        if (handlerResults != null && !handlerResults.isEmpty()) {
            WSHandlerResult handlerResult = handlerResults.get(0);
            List<WSSecurityEngineResult> engineResults = handlerResult.getResults();

            for (WSSecurityEngineResult engineResult : engineResults) {
                Object token = engineResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                if (token instanceof SamlAssertionWrapper) {
                    return (SamlAssertionWrapper)token;
                }
            }
        }

        // Now try steaming results
        try {
            org.apache.xml.security.stax.securityToken.SecurityToken securityToken =
                findInboundSecurityToken(WSSecurityEventConstants.SAML_TOKEN, messageContext);
            if (securityToken instanceof SamlSecurityToken
                && ((SamlSecurityToken)securityToken).getSamlAssertionWrapper() != null) {
                return ((SamlSecurityToken)securityToken).getSamlAssertionWrapper();
            }
        } catch (XMLSecurityException e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            return null;
        }

        return null;
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
        return QNameConstants.WS_TRUST_FACTORY.createBinarySecret(binarySecretType);
    }
}
