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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.RequestParser;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.TokenReference;
import org.apache.cxf.sts.token.renewer.TokenRenewer;
import org.apache.cxf.sts.token.renewer.TokenRenewerParameters;
import org.apache.cxf.sts.token.renewer.TokenRenewerResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
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
        
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setStsProperties(stsProperties);
        renewerParameters.setPrincipal(context.getUserPrincipal());
        renewerParameters.setWebServiceContext(context);
        renewerParameters.setTokenStore(getTokenStore());
        
        renewerParameters.setKeyRequirements(keyRequirements);
        renewerParameters.setTokenRequirements(tokenRequirements);   
        
        //
        // Renew token
        //
        TokenRenewerResponse tokenResponse = null;
        for (TokenRenewer tokenRenewer : tokenRenewers) {
            if (tokenRenewer.canHandleToken(renewTarget)) {
                try {
                    tokenResponse = tokenRenewer.renewToken(renewerParameters);
                } catch (RuntimeException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw new STSException(
                        "Error while renewing a token", ex, STSException.REQUEST_FAILED
                    );
                }
                break;
            }
        }
        if (tokenResponse == null) {
            LOG.fine("No Token Renewer has been found that can handle this token");
            throw new STSException(
                "No token Renewer found for requested token type: " 
                + tokenRequirements.getTokenType(), 
                STSException.REQUEST_FAILED
            );
        }
        
        if (!tokenResponse.isTokenRenewed()) {
            LOG.log(Level.WARNING, "Token renewal failed.");
            throw new STSException("Token renewal failed.");
        }
        
        // prepare response
        try {
            return createResponse(tokenResponse, tokenRequirements, keyRequirements, context);
        } catch (Throwable ex) {
            LOG.log(Level.WARNING, "", ex);
            throw new STSException("Error in creating the response", ex, STSException.REQUEST_FAILED);
        }
    }
    
    private RequestSecurityTokenResponseType createResponse(
        TokenRenewerResponse tokenResponse,
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
        requestedTokenType.setAny(tokenResponse.getRenewedToken());
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
        
        // Lifetime
        LifetimeType lifetime = createLifetime(tokenResponse.getLifetime());
        JAXBElement<LifetimeType> lifetimeType = QNameConstants.WS_TRUST_FACTORY.createLifetime(lifetime);
        response.getAny().add(lifetimeType);
        
        return response;
    }


}
