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

package org.apache.cxf.sts.rest;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceImpl;
import org.apache.cxf.ws.security.sts.provider.model.ClaimsType;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.dom.WSConstants;

public class RESTSecurityTokenServiceImpl extends SecurityTokenServiceImpl implements RESTSecurityTokenService {

    public static final Map<String, String> DEFAULT_CLAIM_TYPE_MAP;

    public static final Map<String, String> DEFAULT_TOKEN_TYPE_MAP;

    private static final String CLAIM_TYPE = "ClaimType";
    private static final String CLAIM_TYPE_NS = "http://schemas.xmlsoap.org/ws/2005/05/identity";

    static {
        DEFAULT_CLAIM_TYPE_MAP = new HashMap<String, String>();
        DEFAULT_CLAIM_TYPE_MAP.put("emailaddress", CLAIM_TYPE_NS + "/claims/emailaddress");
        DEFAULT_CLAIM_TYPE_MAP.put("role", CLAIM_TYPE_NS + "/claims/role");
        DEFAULT_CLAIM_TYPE_MAP.put("surname", CLAIM_TYPE_NS + "/claims/surname");
        DEFAULT_CLAIM_TYPE_MAP.put("givenname", CLAIM_TYPE_NS + "/claims/givenname");
        DEFAULT_CLAIM_TYPE_MAP.put("name", CLAIM_TYPE_NS + "/claims/name");
        DEFAULT_CLAIM_TYPE_MAP.put("upn", CLAIM_TYPE_NS + "/claims/upn");
        DEFAULT_CLAIM_TYPE_MAP.put("nameidentifier", CLAIM_TYPE_NS + "/claims/nameidentifier");

        DEFAULT_TOKEN_TYPE_MAP = new HashMap<String, String>();
        DEFAULT_TOKEN_TYPE_MAP.put("saml", WSConstants.WSS_SAML2_TOKEN_TYPE);
        DEFAULT_TOKEN_TYPE_MAP.put("saml2.0", WSConstants.WSS_SAML2_TOKEN_TYPE);
        DEFAULT_TOKEN_TYPE_MAP.put("saml1.1", WSConstants.WSS_SAML_TOKEN_TYPE);
        DEFAULT_TOKEN_TYPE_MAP.put("jwt", JWTTokenProvider.JWT_TOKEN_TYPE);
        DEFAULT_TOKEN_TYPE_MAP.put("sct", STSUtils.TOKEN_TYPE_SCT_05_12);
    }

    @Context
    private MessageContext messageContext;

    private Map<String, String> claimTypeMap = DEFAULT_CLAIM_TYPE_MAP;

    private Map<String, String> tokenTypeMap = DEFAULT_TOKEN_TYPE_MAP;

    private String defaultKeyType = STSConstants.BEARER_KEY_KEYTYPE;

    private List<String> defaultClaims;

    private boolean requestClaimsOptional = true;

    @Override
    public Response getToken(String tokenType, String keyType, List<String> requestedClaims) {

        if (tokenTypeMap != null && tokenTypeMap.containsKey(tokenType)) {
            tokenType = tokenTypeMap.get(tokenType);
        }
        ObjectFactory of = new ObjectFactory();
        RequestSecurityTokenType request = of.createRequestSecurityTokenType();

        request.getAny().add(of.createTokenType(tokenType));

        request.getAny().add(of.createRequestType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue"));

        request.getAny().add(of.createKeyType(keyType != null
            ? keyType
            : defaultKeyType));

        // Claims
        if (requestedClaims == null) {
            requestedClaims = defaultClaims;
        }

        if (requestedClaims != null) {
            ClaimsType claimsType = of.createClaimsType();
            claimsType.setDialect(CLAIM_TYPE_NS);
            JAXBElement<ClaimsType> claims = of.createClaims(claimsType);
            for (String claim : requestedClaims) {

                if (claimTypeMap != null && claimTypeMap.containsKey(claim)) {
                    claim = claimTypeMap.get(claim);
                }

                Document doc = DOMUtils.createDocument();
                Element claimElement = doc.createElementNS(CLAIM_TYPE_NS, CLAIM_TYPE);
                claimElement.setAttributeNS(null, "Uri", claim);
                claimElement.setAttributeNS(null, "Optional", Boolean.toString(requestClaimsOptional));
                claimsType.getAny().add(claimElement);
            }
            request.getAny().add(claims);
        }

        // OnBehalfOf
        // User Authentication done with JWT or SAML?
        //if (securityContext != null && securityContext.getUserPrincipal() != null) {
            //TODO
//            if (onBehalfOfToken != null) {
//                OnBehalfOfType onBehalfOfType = of.createOnBehalfOfType();
//                onBehalfOfType.setAny(onBehalfOfToken);
//                JAXBElement<OnBehalfOfType> onBehalfOfElement = of.createOnBehalfOf(onBehalfOfType);
//                request.getAny().add(onBehalfOfElement);
//            }
      //  }

        // request.setContext(null);
        return getToken(Action.ISSUE, request);
    }

    @Override
    public Response getToken(Action action, RequestSecurityTokenType request) {
        RequestSecurityTokenResponseType response;
        switch (action) {
        case VALIDATE:
            response = validate(request);
            break;
        case RENEW:
            response = renew(request);
            break;
        case CANCEL:
            response = cancel(request);
            break;
        case ISSUE:
        default:
            response = issueSingle(request);
            break;
        }
        
        JAXBElement<RequestSecurityTokenResponseType> jaxbResponse = 
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponse(response);

        return Response.ok(jaxbResponse).build();
    }

    @Override
    public Response removeToken(RequestSecurityTokenType request) {
        RequestSecurityTokenResponseType response = cancel(request);
        return Response.ok(response).build();
    }

    @Override
    public Response getKeyExchangeToken(RequestSecurityTokenType request) {
        RequestSecurityTokenResponseType response = keyExchangeToken(request);
        return Response.ok(response).build();
    }

    public Map<String, String> getTokenTypeMap() {
        return tokenTypeMap;
    }

    public void setTokenTypeMap(Map<String, String> tokenTypeMap) {
        this.tokenTypeMap = tokenTypeMap;
    }

    public String getDefaultKeyType() {
        return defaultKeyType;
    }

    public void setDefaultKeyType(String defaultKeyType) {
        this.defaultKeyType = defaultKeyType;
    }

    public boolean isRequestClaimsOptional() {
        return requestClaimsOptional;
    }

    public void setRequestClaimsOptional(boolean requestClaimsOptional) {
        this.requestClaimsOptional = requestClaimsOptional;
    }

    public Map<String, String> getClaimTypeMap() {
        return claimTypeMap;
    }

    public void setClaimTypeMap(Map<String, String> claimTypeMap) {
        this.claimTypeMap = claimTypeMap;
    }
    
    @Override
    protected Principal getPrincipal() {
        return messageContext.getSecurityContext().getUserPrincipal();
    }
    
    @Override
    protected Map<String, Object> getMessageContext() {
        return PhaseInterceptorChain.getCurrentMessage();
    }

}
