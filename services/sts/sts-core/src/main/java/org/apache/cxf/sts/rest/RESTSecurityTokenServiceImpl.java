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
import java.security.cert.X509Certificate;
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
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceImpl;
import org.apache.cxf.ws.security.sts.provider.model.ClaimsType;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.UseKeyType;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.X509Data;

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
    public Response getToken(String tokenType, String keyType, 
                             List<String> requestedClaims, String appliesTo,
                             boolean wstrustResponse) {
        RequestSecurityTokenResponseType response = 
            issueToken(tokenType, keyType, requestedClaims, appliesTo);
        
        if (wstrustResponse) {
            JAXBElement<RequestSecurityTokenResponseType> jaxbResponse = 
                QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponse(response);
            
            return Response.ok(jaxbResponse).build();
        }
        
        RequestedSecurityTokenType requestedToken = getRequestedSecurityToken(response);
        
        if ("jwt".equals(tokenType)) {
            // Discard the wrapper here
            return Response.ok(((Element)requestedToken.getAny()).getTextContent()).build();
        } else {
            return Response.ok(requestedToken.getAny()).build();
        }
    }
    
    private RequestedSecurityTokenType getRequestedSecurityToken(RequestSecurityTokenResponseType response) {
        for (Object obj : response.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("RequestedSecurityToken".equals(jaxbElement.getName().getLocalPart())) {
                    return (RequestedSecurityTokenType)jaxbElement.getValue();
                }
            }
        }
        return null;
    }
    
    private RequestSecurityTokenResponseType issueToken(
        String tokenType,
        String keyType,
        List<String> requestedClaims,
        String appliesTo
    ) {
        if (tokenTypeMap != null && tokenTypeMap.containsKey(tokenType)) {
            tokenType = tokenTypeMap.get(tokenType);
        }
        ObjectFactory of = new ObjectFactory();
        RequestSecurityTokenType request = of.createRequestSecurityTokenType();

        request.getAny().add(of.createTokenType(tokenType));

        request.getAny().add(of.createRequestType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue"));

        String desiredKeyType = keyType != null ? keyType : defaultKeyType;
        request.getAny().add(of.createKeyType(desiredKeyType));
        
        // Add the TLS client Certificate as the UseKey Element if the KeyType is PublicKey
        if (STSConstants.PUBLIC_KEY_KEYTYPE.equals(desiredKeyType)) {
            X509Certificate clientCert = getTLSClientCertificate();
            if (clientCert != null) {
                Document doc = DOMUtils.createDocument();
                Element keyInfoElement = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "KeyInfo");
                
                try {
                    X509Data certElem = new X509Data(doc);
                    certElem.addCertificate(clientCert);
                    keyInfoElement.appendChild(certElem.getElement());
                    
                    UseKeyType useKeyType = of.createUseKeyType();
                    useKeyType.setAny(keyInfoElement);
                    
                    JAXBElement<UseKeyType> useKey = of.createUseKey(useKeyType);
                    request.getAny().add(useKey);
                } catch (XMLSecurityException ex) {
                    // TODO
                }
            }
        }

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
        
        if (appliesTo != null) {
            String wspNamespace = "http://www.w3.org/ns/ws-policy";
            Document doc = DOMUtils.createDocument();
            Element appliesToElement = doc.createElementNS(wspNamespace, "AppliesTo");
            
            String addressingNamespace = "http://www.w3.org/2005/08/addressing";
            Element eprElement = doc.createElementNS(addressingNamespace, "EndpointReference");
            Element addressElement = doc.createElementNS(addressingNamespace, "Address");
            addressElement.setTextContent(appliesTo);

            eprElement.appendChild(addressElement);
            appliesToElement.appendChild(eprElement);
            
            request.getAny().add(appliesToElement);
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
        return processRequest(Action.issue, request);
    }

    @Override
    public Response getToken(Action action, RequestSecurityTokenType request) {
        RequestSecurityTokenResponseType response = processRequest(action, request);
        
        JAXBElement<RequestSecurityTokenResponseType> jaxbResponse = 
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponse(response);

        return Response.ok(jaxbResponse).build();
    }
    
    private RequestSecurityTokenResponseType processRequest(Action action, 
                                                            RequestSecurityTokenType request) {
        switch (action) {
        case validate:
            return validate(request);
        case renew:
            return renew(request);
        case cancel:
            return cancel(request);
        case issue:
        default:
            return issueSingle(request);
        }
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
        SecurityContext sc = (SecurityContext)messageContext.get(SecurityContext.class);
        if (sc == null || sc.getUserPrincipal() == null) {
            // Get the TLS client principal if no security context is set up
            return getTLSClientCertificate().getSubjectX500Principal();
        }
        return messageContext.getSecurityContext().getUserPrincipal();
    }
    
    private X509Certificate getTLSClientCertificate() {
        TLSSessionInfo tlsInfo = 
            (TLSSessionInfo)PhaseInterceptorChain.getCurrentMessage().get(TLSSessionInfo.class);
        if (tlsInfo != null && tlsInfo.getPeerCertificates() != null 
                && tlsInfo.getPeerCertificates().length > 0
                && (tlsInfo.getPeerCertificates()[0] instanceof X509Certificate)
        ) {
            return (X509Certificate)tlsInfo.getPeerCertificates()[0];
        }
        return null;
    }
    
    @Override
    protected Map<String, Object> getMessageContext() {
        return PhaseInterceptorChain.getCurrentMessage();
    }

}
