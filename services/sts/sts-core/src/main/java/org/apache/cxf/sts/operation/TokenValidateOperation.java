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

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.RealmParser;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.event.STSValidateFailureEvent;
import org.apache.cxf.sts.event.STSValidateSuccessEvent;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.RequestRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.provider.TokenReference;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;
import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * An implementation of the ValidateOperation interface.
 */
public class TokenValidateOperation extends AbstractOperation implements ValidateOperation {

    private static final Logger LOG = LogUtils.getL7dLogger(TokenValidateOperation.class);

    public RequestSecurityTokenResponseType validate(
        RequestSecurityTokenType request,
        Principal principal,
        Map<String, Object> messageContext
    ) {
        long start = System.currentTimeMillis();
        TokenValidatorParameters validatorParameters = new TokenValidatorParameters();

        try {
            RequestRequirements requestRequirements = parseRequest(request, messageContext);

            TokenRequirements tokenRequirements = requestRequirements.getTokenRequirements();

            validatorParameters.setStsProperties(stsProperties);
            validatorParameters.setPrincipal(principal);
            validatorParameters.setMessageContext(messageContext);
            validatorParameters.setTokenStore(getTokenStore());

            //validatorParameters.setKeyRequirements(keyRequirements);
            validatorParameters.setTokenRequirements(tokenRequirements);

            ReceivedToken validateTarget = tokenRequirements.getValidateTarget();
            if (validateTarget == null || validateTarget.getToken() == null) {
                throw new STSException("No element presented for validation", STSException.INVALID_REQUEST);
            }
            validatorParameters.setToken(validateTarget);

            if (tokenRequirements.getTokenType() == null) {
                tokenRequirements.setTokenType(STSConstants.STATUS);
                LOG.fine(
                    "Received TokenType is null, falling back to default token type: "
                    + STSConstants.STATUS
                );
            }

            // Get the realm of the request
            String realm = null;
            if (stsProperties.getRealmParser() != null) {
                RealmParser realmParser = stsProperties.getRealmParser();
                realm = realmParser.parseRealm(messageContext);
            }
            validatorParameters.setRealm(realm);

            TokenValidatorResponse tokenResponse = validateReceivedToken(
                    principal, messageContext, realm, tokenRequirements, validateTarget);

            if (tokenResponse == null) {
                LOG.fine("No Token Validator has been found that can handle this token");
                tokenResponse = new TokenValidatorResponse();
                validateTarget.setState(STATE.INVALID);
                tokenResponse.setToken(validateTarget);
            }

            //
            // Create a new token (if requested)
            //
            TokenProviderResponse tokenProviderResponse = null;
            String tokenType = tokenRequirements.getTokenType();
            if (tokenResponse.getToken().getState() == STATE.VALID
                && !STSConstants.STATUS.equals(tokenType)) {
                TokenProviderParameters providerParameters =
                     createTokenProviderParameters(requestRequirements, principal, messageContext);

                processValidToken(providerParameters, validateTarget, tokenResponse);

                // Check if the requested claims can be handled by the configured claim handlers
                providerParameters.setClaimsManager(claimsManager);

                Map<String, Object> additionalProperties = tokenResponse.getAdditionalProperties();
                if (additionalProperties != null) {
                    providerParameters.setAdditionalProperties(additionalProperties);
                }
                realm = providerParameters.getRealm();
                for (TokenProvider tokenProvider : tokenProviders) {
                    final boolean canHandle;
                    if (realm == null) {
                        canHandle = tokenProvider.canHandleToken(tokenType);
                    } else {
                        canHandle = tokenProvider.canHandleToken(tokenType, realm);
                    }
                    if (canHandle) {
                        try {
                            tokenProviderResponse = tokenProvider.createToken(providerParameters);
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
                if (tokenProviderResponse == null || tokenProviderResponse.getToken() == null) {
                    LOG.fine("No Token Provider has been found that can handle this token");
                    throw new STSException(
                        "No token provider found for requested token type: " + tokenType,
                        STSException.REQUEST_FAILED
                    );
                }
            }

            // prepare response
            try {
                RequestSecurityTokenResponseType response =
                    createResponse(tokenResponse, tokenProviderResponse, tokenRequirements);
                STSValidateSuccessEvent event = new STSValidateSuccessEvent(validatorParameters,
                        System.currentTimeMillis() - start);
                publishEvent(event);

                cleanRequest(requestRequirements);
                return response;
            } catch (Throwable ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new STSException("Error in creating the response", ex, STSException.REQUEST_FAILED);
            }

        } catch (RuntimeException ex) {
            STSValidateFailureEvent event = new STSValidateFailureEvent(validatorParameters,
                                                              System.currentTimeMillis() - start, ex);
            publishEvent(event);
            throw ex;
        }
    }

    protected RequestSecurityTokenResponseType createResponse(
        TokenValidatorResponse tokenResponse,
        TokenProviderResponse tokenProviderResponse,
        TokenRequirements tokenRequirements
    ) throws WSSecurityException {
        RequestSecurityTokenResponseType response =
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponseType();

        String context = tokenRequirements.getContext();
        if (context != null) {
            response.setContext(context);
        }

        // TokenType
        boolean valid = tokenResponse.getToken().getState() == STATE.VALID;
        String tokenType = tokenRequirements.getTokenType();
        if (valid || STSConstants.STATUS.equals(tokenType)) {
            JAXBElement<String> jaxbTokenType =
                QNameConstants.WS_TRUST_FACTORY.createTokenType(tokenType);
            response.getAny().add(jaxbTokenType);
        }

        // Status
        StatusType statusType = QNameConstants.WS_TRUST_FACTORY.createStatusType();
        if (valid) {
            statusType.setCode(STSConstants.VALID_CODE);
            statusType.setReason(STSConstants.VALID_REASON);
        } else {
            statusType.setCode(STSConstants.INVALID_CODE);
            statusType.setReason(STSConstants.INVALID_REASON);
        }
        JAXBElement<StatusType> status = QNameConstants.WS_TRUST_FACTORY.createStatus(statusType);
        response.getAny().add(status);

        // RequestedSecurityToken
        if (valid && !STSConstants.STATUS.equals(tokenType) && tokenProviderResponse != null
            && tokenProviderResponse.getToken() != null) {
            RequestedSecurityTokenType requestedTokenType =
                QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityTokenType();
            JAXBElement<RequestedSecurityTokenType> requestedToken =
                QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(requestedTokenType);
            tokenWrapper.wrapToken(tokenProviderResponse.getToken(), requestedTokenType);
            response.getAny().add(requestedToken);

            // Lifetime
            if (includeLifetimeElement) {
                LifetimeType lifetime =
                    createLifetime(tokenProviderResponse.getCreated(), tokenProviderResponse.getExpires());
                JAXBElement<LifetimeType> lifetimeType =
                    QNameConstants.WS_TRUST_FACTORY.createLifetime(lifetime);
                response.getAny().add(lifetimeType);
            }

            if (returnReferences) {
                // RequestedAttachedReference
                TokenReference attachedReference = tokenProviderResponse.getAttachedReference();
                final RequestedReferenceType requestedAttachedReferenceType;
                if (attachedReference != null) {
                    requestedAttachedReferenceType = createRequestedReference(attachedReference, true);
                } else {
                    requestedAttachedReferenceType =
                        createRequestedReference(
                            tokenProviderResponse.getTokenId(), tokenRequirements.getTokenType(), true
                        );
                }

                JAXBElement<RequestedReferenceType> requestedAttachedReference =
                    QNameConstants.WS_TRUST_FACTORY.createRequestedAttachedReference(
                        requestedAttachedReferenceType
                    );
                response.getAny().add(requestedAttachedReference);

                // RequestedUnattachedReference
                TokenReference unAttachedReference = tokenProviderResponse.getUnAttachedReference();
                final RequestedReferenceType requestedUnattachedReferenceType;
                if (unAttachedReference != null) {
                    requestedUnattachedReferenceType =
                        createRequestedReference(unAttachedReference, false);
                } else {
                    requestedUnattachedReferenceType =
                        createRequestedReference(
                            tokenProviderResponse.getTokenId(), tokenRequirements.getTokenType(), false
                        );
                }

                JAXBElement<RequestedReferenceType> requestedUnattachedReference =
                    QNameConstants.WS_TRUST_FACTORY.createRequestedUnattachedReference(
                        requestedUnattachedReferenceType
                    );
                response.getAny().add(requestedUnattachedReference);
            }
        }

        return response;
    }


}
