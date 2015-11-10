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
package org.apache.cxf.rs.security.saml.sso;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.SAMLUtils;
import org.apache.cxf.rs.security.saml.assertion.Subject;
import org.apache.cxf.rs.security.saml.sso.state.RequestState;
import org.apache.cxf.rs.security.saml.sso.state.ResponseState;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.opensaml.saml.saml2.core.AuthnRequest;

@PreMatching
@Priority(Priorities.AUTHENTICATION + 1)
public abstract class AbstractServiceProviderFilter extends AbstractSSOSpHandler 
    implements ContainerRequestFilter {
    
    protected static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractServiceProviderFilter.class);
    protected static final ResourceBundle BUNDLE = 
        BundleUtils.getBundle(AbstractServiceProviderFilter.class);
    
    private String idpServiceAddress;
    private String issuerId;
    private String assertionConsumerServiceAddress;
    private AuthnRequestBuilder authnRequestBuilder = new DefaultAuthnRequestBuilder();
    private boolean signRequest;
    
    private String webAppDomain;
    private boolean addWebAppContext = true;
    private boolean addEndpointAddressToContext;
    
    public void setAddEndpointAddressToContext(boolean add) {
        addEndpointAddressToContext = add;
    }
    
    public void setSignRequest(boolean signRequest) {
        this.signRequest = signRequest;
    }
    
    public boolean isSignRequest() {
        return signRequest;
    }
    
    public void setAuthnRequestBuilder(AuthnRequestBuilder authnRequestBuilder) {
        this.authnRequestBuilder = authnRequestBuilder;
    }
    
    public void setAssertionConsumerServiceAddress(
            String assertionConsumerServiceAddress) {
        this.assertionConsumerServiceAddress = assertionConsumerServiceAddress;
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }
    
    public void setIdpServiceAddress(String idpServiceAddress) {
        this.idpServiceAddress = idpServiceAddress;
    }

    public String getIdpServiceAddress() {
        return idpServiceAddress;
    }
    
    @PreDestroy
    public void close() {
        super.close();
    }
    
    private String getIssuerId(Message m) {
        if (issuerId == null) {
            return new UriInfoImpl(m).getBaseUri().toString();
        } else {
            return issuerId;
        }
    }
    
    protected boolean checkSecurityContext(Message m) {
        HttpHeaders headers = new HttpHeadersImpl(m);
        Map<String, Cookie> cookies = headers.getCookies();
        
        Cookie securityContextCookie = cookies.get(SSOConstants.SECURITY_CONTEXT_TOKEN);
        
        ResponseState responseState = getValidResponseState(securityContextCookie, m);
        if (responseState == null) {
            return false;    
        }
        
        Cookie relayStateCookie = cookies.get(SSOConstants.RELAY_STATE);
        if (relayStateCookie == null) {
            reportError("MISSING_RELAY_COOKIE");
            return false;
        }
        String originalRelayState = responseState.getRelayState();
        if (!originalRelayState.equals(relayStateCookie.getValue())) {
            // perhaps the response state should also be removed
            reportError("INVALID_RELAY_STATE");
            return false;
        }
        try {
            String assertion = responseState.getAssertion();
            SamlAssertionWrapper assertionWrapper = 
                new SamlAssertionWrapper(
                    StaxUtils.read(new StringReader(assertion)).getDocumentElement());
            setSecurityContext(m, assertionWrapper);
        } catch (Exception ex) {
            reportError("INVALID_RESPONSE_STATE");
            return false;
        }
        return true;
    }
    
    protected void setSecurityContext(Message m, SamlAssertionWrapper assertionWrapper) {
        Subject subject = SAMLUtils.getSubject(m, assertionWrapper);
        final String name = subject.getName();
        
        if (name != null) {
            String roleAttributeName = 
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SAML_ROLE_ATTRIBUTENAME, m);
            if (roleAttributeName == null || roleAttributeName.length() == 0) {
                roleAttributeName = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
            }
            ClaimCollection claims = 
                org.apache.cxf.rt.security.saml.utils.SAMLUtils.getClaims(assertionWrapper);
            Set<Principal> roles = 
                org.apache.cxf.rt.security.saml.utils.SAMLUtils.parseRolesFromClaims(
                    claims, roleAttributeName, null);

            SAMLSecurityContext context = 
                new SAMLSecurityContext(new SimplePrincipal(name), roles, claims);
            context.setIssuer(org.apache.cxf.rt.security.saml.utils.SAMLUtils.getIssuer(assertionWrapper));
            context.setAssertionElement(
                org.apache.cxf.rt.security.saml.utils.SAMLUtils.getAssertionElement(assertionWrapper));
            m.put(SecurityContext.class, context);
        }
    }
    
    protected ResponseState getValidResponseState(Cookie securityContextCookie, 
                                                  Message m) {
        if (securityContextCookie == null) {
            // most likely it means that the user has not been offered
            // a chance to get logged on yet, though it might be that the browser
            // has removed an expired cookie from its cache; warning is too noisy in the
            // former case
            reportTrace("MISSING_RESPONSE_STATE");
            return null;
        }
        String contextKey = securityContextCookie.getValue();
        
        ResponseState responseState = getStateProvider().getResponseState(contextKey);
        
        if (responseState == null) {
            reportError("MISSING_RESPONSE_STATE");
            return null;
        }
        if (isStateExpired(responseState.getCreatedAt(), responseState.getExpiresAt())) {
            reportError("EXPIRED_RESPONSE_STATE");
            getStateProvider().removeResponseState(contextKey);
            return null;
        }
        String webAppContext = getWebAppContext(m);
        if (webAppDomain != null 
            && (responseState.getWebAppDomain() == null 
                || !webAppDomain.equals(responseState.getWebAppDomain()))
            || responseState.getWebAppContext() == null
            || !webAppContext.equals(responseState.getWebAppContext())) {
            getStateProvider().removeResponseState(contextKey);
            reportError("INVALID_RESPONSE_STATE");
            return null;
        }
        if (responseState.getAssertion() == null) {
            reportError("INVALID_RESPONSE_STATE");
            return null;
        }
        return responseState;
    }
    
    protected SamlRequestInfo createSamlRequestInfo(Message m) throws Exception {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
 
        // Create the AuthnRequest
        AuthnRequest authnRequest = 
            authnRequestBuilder.createAuthnRequest(
                m, getIssuerId(m), getAbsoluteAssertionServiceAddress(m)
            );
        if (isSignRequest()) {
            authnRequest.setDestination(idpServiceAddress);
            signAuthnRequest(authnRequest);
        }
        Element authnRequestElement = OpenSAMLUtil.toDom(authnRequest, doc);
        String authnRequestEncoded = encodeAuthnRequest(authnRequestElement);
        
        SamlRequestInfo info = new SamlRequestInfo();
        info.setSamlRequest(authnRequestEncoded);
        
        String webAppContext = getWebAppContext(m);
        String originalRequestURI = new UriInfoImpl(m).getRequestUri().toString();
        
        RequestState requestState = new RequestState(originalRequestURI,
                                                     getIdpServiceAddress(),
                                                     authnRequest.getID(),
                                                     getIssuerId(m),
                                                     webAppContext,
                                                     getWebAppDomain(),
                                                     System.currentTimeMillis());
        
        String relayState = URLEncoder.encode(UUID.randomUUID().toString(), StandardCharsets.UTF_8.name());
        getStateProvider().setRequestState(relayState, requestState);
        info.setRelayState(relayState);
        info.setWebAppContext(webAppContext);
        info.setWebAppDomain(getWebAppDomain());
        
        return info;
    }
    
    protected abstract String encodeAuthnRequest(Element authnRequest) throws IOException;
    
    protected abstract void signAuthnRequest(AuthnRequest authnRequest) throws Exception;
    
    private String getAbsoluteAssertionServiceAddress(Message m) {
        if (assertionConsumerServiceAddress == null) {
            if (Boolean.TRUE.equals(JAXRSUtils.getCurrentMessage().get(SSOConstants.RACS_IS_COLLOCATED))) {
                assertionConsumerServiceAddress = new UriInfoImpl(m).getAbsolutePath().toString();    
            } else {
                reportError("MISSING_ASSERTION_SERVICE_URL");
                throw ExceptionUtils.toInternalServerErrorException(null, null);
            }
        }
        if (!assertionConsumerServiceAddress.startsWith("http")) {
            String httpBasePath = (String)m.get("http.base.path");
            return UriBuilder.fromUri(httpBasePath)
                             .path(assertionConsumerServiceAddress)
                             .build()
                             .toString();
        } else {
            return assertionConsumerServiceAddress;
        }
    }
    
    protected void reportError(String code) {
        org.apache.cxf.common.i18n.Message errorMsg = 
            new org.apache.cxf.common.i18n.Message(code, BUNDLE);
        LOG.warning(errorMsg.toString());
    }
    
    protected void reportTrace(String code) {
        if (LOG.isLoggable(Level.FINE)) {
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message(code, BUNDLE);
            LOG.fine(errorMsg.toString());
        }
    }

    private String getWebAppContext(Message m) {
        if (addWebAppContext) {
            if (addEndpointAddressToContext) {
                return new UriInfoImpl(m).getBaseUri().getRawPath();
            } else {
                String httpBasePath = (String)m.get("http.base.path");
                return URI.create(httpBasePath).getRawPath();
            }
        } else {
            return "/";
        }
    }
    
    public String getWebAppDomain() {
        return webAppDomain;
    }

    public void setWebAppDomain(String webAppDomain) {
        this.webAppDomain = webAppDomain;
    }

    public void setAddWebAppContext(boolean addWebAppContext) {
        this.addWebAppContext = addWebAppContext;
    }
        
}
