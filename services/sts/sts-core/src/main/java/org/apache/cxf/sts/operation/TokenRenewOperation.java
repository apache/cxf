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
import org.apache.cxf.sts.event.STSRenewFailureEvent;
import org.apache.cxf.sts.event.STSRenewSuccessEvent;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.RequestRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenReference;
import org.apache.cxf.sts.token.renewer.TokenRenewer;
import org.apache.cxf.sts.token.renewer.TokenRenewerParameters;
import org.apache.cxf.sts.token.renewer.TokenRenewerResponse;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * An implementation of the IssueOperation interface to renew tokens.
 */
public class TokenRenewOperation extends AbstractOperation implements RenewOperation {

    private static final Logger LOG = LogUtils.getL7dLogger(TokenRenewOperation.class);

    private List<TokenRenewer> tokenRenewers = new ArrayList<TokenRenewer>();

    public void setTokenRenewers(List<TokenRenewer> tokenRenewerList) {
        this.tokenRenewers = tokenRenewerList;
    }
    
    public List<TokenRenewer> getTokenRenewers() {
        return tokenRenewers;
    }

    public RequestSecurityTokenResponseType renew(
        RequestSecurityTokenType request, WebServiceContext context
    ) {
        long start = System.currentTimeMillis();
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        
        try {
            RequestRequirements requestRequirements = parseRequest(request, context);
    
            KeyRequirements keyRequirements = requestRequirements.getKeyRequirements();
            TokenRequirements tokenRequirements = requestRequirements.getTokenRequirements();
            
            renewerParameters.setStsProperties(stsProperties);
            renewerParameters.setPrincipal(context.getUserPrincipal());
            renewerParameters.setMessageContext(context.getMessageContext());
            renewerParameters.setTokenStore(getTokenStore());
            
            renewerParameters.setKeyRequirements(keyRequirements);
            renewerParameters.setTokenRequirements(tokenRequirements);  
            
            ReceivedToken renewTarget = tokenRequirements.getRenewTarget();
            if (renewTarget == null || renewTarget.getToken() == null) {
                throw new STSException("No element presented for renewal", STSException.INVALID_REQUEST);
            }
            renewerParameters.setToken(renewTarget);
            
            if (tokenRequirements.getTokenType() == null) {
                LOG.fine("Received TokenType is null");
            }
            
            // Get the realm of the request
            String realm = null;
            if (stsProperties.getRealmParser() != null) {
                RealmParser realmParser = stsProperties.getRealmParser();
                realm = realmParser.parseRealm(context.getMessageContext());
            }
            renewerParameters.setRealm(realm);
            
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
            
            // Reject an invalid token
            if (tokenResponse.getToken().getState() != STATE.EXPIRED
                && tokenResponse.getToken().getState() != STATE.VALID) {
                LOG.fine("The token is not valid or expired, and so it cannot be renewed");
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
            renewerParameters = createTokenRenewerParameters(requestRequirements, context);
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
                STSRenewSuccessEvent event = new STSRenewSuccessEvent(renewerParameters,
                        System.currentTimeMillis() - start);
                publishEvent(event);
                return response;
            } catch (Throwable ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new STSException("Error in creating the response", ex, STSException.REQUEST_FAILED);
            }
        } catch (RuntimeException ex) {
            STSRenewFailureEvent event = new STSRenewFailureEvent(renewerParameters,
                                                              System.currentTimeMillis() - start, ex);
            publishEvent(event);
            throw ex;
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
        requestedTokenType.setAny(tokenRenewerResponse.getToken());
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

        // Lifetime
        LifetimeType lifetime = 
            createLifetime(tokenRenewerResponse.getCreated(), tokenRenewerResponse.getExpires());
        JAXBElement<LifetimeType> lifetimeType = QNameConstants.WS_TRUST_FACTORY.createLifetime(lifetime);
        response.getAny().add(lifetimeType);

        return response;
    }

    private TokenRenewerParameters createTokenRenewerParameters(
        RequestRequirements requestRequirements, WebServiceContext context
    ) {
        TokenProviderParameters providerParameters = 
            createTokenProviderParameters(requestRequirements, context);
        
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress(providerParameters.getAppliesToAddress());
        renewerParameters.setEncryptionProperties(providerParameters.getEncryptionProperties());
        renewerParameters.setKeyRequirements(providerParameters.getKeyRequirements());
        renewerParameters.setPrincipal(providerParameters.getPrincipal());
        renewerParameters.setStsProperties(providerParameters.getStsProperties());
        renewerParameters.setTokenRequirements(providerParameters.getTokenRequirements());
        renewerParameters.setTokenStore(providerParameters.getTokenStore());
        renewerParameters.setMessageContext(providerParameters.getMessageContext());
        
        return renewerParameters;
    }

}
