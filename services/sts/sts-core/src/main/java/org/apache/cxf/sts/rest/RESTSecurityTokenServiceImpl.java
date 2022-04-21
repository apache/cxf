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

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.Deflater;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.message.Message;
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
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.X509Data;

public class RESTSecurityTokenServiceImpl extends SecurityTokenServiceImpl implements RESTSecurityTokenService {

    public static final Map<String, String> DEFAULT_CLAIM_TYPE_MAP;
    public static final Map<String, String> DEFAULT_TOKEN_TYPE_MAP;

    private static final Map<String, String> DEFAULT_KEY_TYPE_MAP = new HashMap<>();

    private static final String CLAIM_TYPE = "ClaimType";
    private static final String CLAIM_TYPE_NS = "http://schemas.xmlsoap.org/ws/2005/05/identity";
    private static final Logger LOG = LogUtils.getL7dLogger(RESTSecurityTokenServiceImpl.class);

    static {
        Map<String, String> tmpClaimTypeMap = new HashMap<>();
        tmpClaimTypeMap.put("emailaddress", CLAIM_TYPE_NS + "/claims/emailaddress");
        tmpClaimTypeMap.put("role", CLAIM_TYPE_NS + "/claims/role");
        tmpClaimTypeMap.put("roles", CLAIM_TYPE_NS + "/claims/role");
        tmpClaimTypeMap.put("surname", CLAIM_TYPE_NS + "/claims/surname");
        tmpClaimTypeMap.put("givenname", CLAIM_TYPE_NS + "/claims/givenname");
        tmpClaimTypeMap.put("name", CLAIM_TYPE_NS + "/claims/name");
        tmpClaimTypeMap.put("upn", CLAIM_TYPE_NS + "/claims/upn");
        tmpClaimTypeMap.put("nameidentifier", CLAIM_TYPE_NS + "/claims/nameidentifier");
        DEFAULT_CLAIM_TYPE_MAP = Collections.unmodifiableMap(tmpClaimTypeMap);

        Map<String, String> tmpTokenTypeMap = new HashMap<>();
        tmpTokenTypeMap.put("saml", WSS4JConstants.WSS_SAML2_TOKEN_TYPE);
        tmpTokenTypeMap.put("saml2.0", WSS4JConstants.WSS_SAML2_TOKEN_TYPE);
        tmpTokenTypeMap.put("saml1.1", WSS4JConstants.WSS_SAML_TOKEN_TYPE);
        tmpTokenTypeMap.put("jwt", JWTTokenProvider.JWT_TOKEN_TYPE);
        tmpTokenTypeMap.put("sct", STSUtils.TOKEN_TYPE_SCT_05_12);
        DEFAULT_TOKEN_TYPE_MAP = Collections.unmodifiableMap(tmpTokenTypeMap);

        DEFAULT_KEY_TYPE_MAP.put("SymmetricKey", STSConstants.SYMMETRIC_KEY_KEYTYPE);
        DEFAULT_KEY_TYPE_MAP.put("PublicKey", STSConstants.PUBLIC_KEY_KEYTYPE);
        DEFAULT_KEY_TYPE_MAP.put("Bearer", STSConstants.BEARER_KEY_KEYTYPE);
    }

    @Context
    private MessageContext messageContext;

    @Context
    private jakarta.ws.rs.core.SecurityContext securityContext;

    private Map<String, String> claimTypeMap = DEFAULT_CLAIM_TYPE_MAP;
    private Map<String, String> tokenTypeMap = DEFAULT_TOKEN_TYPE_MAP;

    private String defaultKeyType = STSConstants.BEARER_KEY_KEYTYPE;

    private List<String> defaultClaims;

    private boolean requestClaimsOptional = true;
    private boolean useDeflateEncoding = true;

    @Override
    public Response getXMLToken(String tokenType, String keyType,
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
        return Response.ok(requestedToken.getAny()).build();
    }

    @Override
    public Response getJSONToken(String tokenType, String keyType,
                             List<String> requestedClaims, String appliesTo) {
        if (!"jwt".equals(tokenType)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        RequestSecurityTokenResponseType response =
            issueToken(tokenType, keyType, requestedClaims, appliesTo);

        RequestedSecurityTokenType requestedToken = getRequestedSecurityToken(response);

        // Discard the XML Wrapper + create a new JSON Wrapper
        String token = ((Element)requestedToken.getAny()).getTextContent();
        return Response.ok(new JSONWrapper(token)).build();
    }

    @Override
    public Response getPlainToken(String tokenType, String keyType,
                             List<String> requestedClaims, String appliesTo) {
        RequestSecurityTokenResponseType response =
            issueToken(tokenType, keyType, requestedClaims, appliesTo);

        RequestedSecurityTokenType requestedToken = getRequestedSecurityToken(response);

        if ("jwt".equals(tokenType)) {
            // Discard the wrapper here
            return Response.ok(((Element)requestedToken.getAny()).getTextContent()).build();
        }
        // Base-64 encode the token + return it
        try {
            String encodedToken =
                encodeToken(DOM2Writer.nodeToString((Element)requestedToken.getAny()));
            return Response.ok(encodedToken).build();
        } catch (Exception ex) {
            LOG.warning(ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
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
        String tokenTypeToUse = tokenType;
        if (tokenTypeMap != null && tokenTypeMap.containsKey(tokenTypeToUse)) {
            tokenTypeToUse = tokenTypeMap.get(tokenTypeToUse);
        }

        String keyTypeToUse = keyType;
        if (DEFAULT_KEY_TYPE_MAP.containsKey(keyTypeToUse)) {
            keyTypeToUse = DEFAULT_KEY_TYPE_MAP.get(keyTypeToUse);
        }

        ObjectFactory of = new ObjectFactory();
        RequestSecurityTokenType request = of.createRequestSecurityTokenType();

        request.getAny().add(of.createTokenType(tokenTypeToUse));

        request.getAny().add(of.createRequestType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue"));

        String desiredKeyType = keyTypeToUse != null ? keyTypeToUse : defaultKeyType;
        request.getAny().add(of.createKeyType(desiredKeyType));

        // Add the TLS client Certificate as the UseKey Element if the KeyType is PublicKey
        if (STSConstants.PUBLIC_KEY_KEYTYPE.equals(desiredKeyType)) {
            X509Certificate clientCert = getTLSClientCertificate();
            if (clientCert != null) {
                Document doc = DOMUtils.getEmptyDocument();
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
                    LOG.warning(ex.getMessage());
                }
            }
        }

        // Claims
        if (requestedClaims == null || requestedClaims.isEmpty()) {
            requestedClaims = defaultClaims;
        }

        if (requestedClaims != null && !requestedClaims.isEmpty()) {
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
        // Try JAX-RS SecurityContext first
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal();
        }

        // Then try the CXF SecurityContext
        SecurityContext sc = (SecurityContext)messageContext.get(SecurityContext.class);
        if (sc != null && sc.getUserPrincipal() != null) {
            return sc.getUserPrincipal();
        }

        // Get the TLS client principal if no security context is set up
        X509Certificate clientCert = getTLSClientCertificate();
        if (clientCert != null) {
            return clientCert.getSubjectX500Principal();
        }

        return null;
    }

    private X509Certificate getTLSClientCertificate() {
        TLSSessionInfo tlsInfo =
            PhaseInterceptorChain.getCurrentMessage().get(TLSSessionInfo.class);
        if (tlsInfo != null && tlsInfo.getPeerCertificates() != null
                && tlsInfo.getPeerCertificates().length > 0
                && tlsInfo.getPeerCertificates()[0] instanceof X509Certificate
        ) {
            return (X509Certificate)tlsInfo.getPeerCertificates()[0];
        }
        return null;
    }

    @Override
    protected Map<String, Object> getMessageContext() {
        return PhaseInterceptorChain.getCurrentMessage();
    }

    public void setUseDeflateEncoding(boolean deflate) {
        useDeflateEncoding = deflate;
    }

    protected String encodeToken(String assertion) throws Base64Exception {
        byte[] tokenBytes = assertion.getBytes(StandardCharsets.UTF_8);

        if (useDeflateEncoding) {
            tokenBytes = CompressionUtils.deflate(tokenBytes, getDeflateLevel(), true);
        }
        StringWriter writer = new StringWriter();
        Base64Utility.encode(tokenBytes, 0, tokenBytes.length, writer);
        return writer.toString();
    }

    private static int getDeflateLevel() {
        Integer level = null;

        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (m != null) {
            level = PropertyUtils.getInteger(m, "deflate.level");
        }
        if (level == null) {
            level = Deflater.DEFLATED;
        }
        return level;
    }

    private static class JSONWrapper {
        private String token;

        JSONWrapper(String token) {
            this.token = token;
        }

        @SuppressWarnings("unused")
        public String getToken() {
            return token;
        }
    }
}
