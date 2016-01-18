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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

import javax.annotation.PreDestroy;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.w3c.dom.Document;
import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.rs.security.saml.sso.state.RequestState;
import org.apache.cxf.rs.security.saml.sso.state.ResponseState;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.opensaml.core.xml.XMLObject;

public abstract class AbstractRequestAssertionConsumerHandler extends AbstractSSOSpHandler {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractRequestAssertionConsumerHandler.class);
    private static final ResourceBundle BUNDLE = 
        BundleUtils.getBundle(AbstractRequestAssertionConsumerHandler.class);
    
    private boolean supportDeflateEncoding = true;
    private boolean supportBase64Encoding = true;
    private boolean enforceAssertionsSigned = true;
    private boolean enforceKnownIssuer = true;
    private boolean keyInfoMustBeAvailable = true;
    private boolean enforceResponseSigned;
    private TokenReplayCache<String> replayCache;

    private MessageContext messageContext;
    
    @Context 
    public void setMessageContext(MessageContext mc) {
        this.messageContext = mc;
    }
    
    public void setSupportDeflateEncoding(boolean deflate) {
        supportDeflateEncoding = deflate;
    }
    public boolean isSupportDeflateEncoding() {
        return supportDeflateEncoding;
    }
    
    public void setReplayCache(TokenReplayCache<String> replayCache) {
        this.replayCache = replayCache;
    }
    
    public TokenReplayCache<String> getReplayCache() {
        if (replayCache == null) {
            Bus bus = (Bus)messageContext.getContextualProperty(Bus.class.getName());
            replayCache = new EHCacheTokenReplayCache(bus);
        }
        return replayCache;
    }
    
    /**
     * Enforce that Assertions must be signed if the POST binding was used. The default is true.
     */
    public void setEnforceAssertionsSigned(boolean enforceAssertionsSigned) {
        this.enforceAssertionsSigned = enforceAssertionsSigned;
    }
    
    /**
     * Enforce that the Issuer of the received Response/Assertion is known to this RACS. The
     * default is true.
     */
    public void setEnforceKnownIssuer(boolean enforceKnownIssuer) {
        this.enforceKnownIssuer = enforceKnownIssuer;
    }
    
    public void setSupportBase64Encoding(boolean supportBase64Encoding) {
        this.supportBase64Encoding = supportBase64Encoding;
    }
    public boolean isSupportBase64Encoding() {
        return supportBase64Encoding;
    }
    
    @PreDestroy
    @Override
    public void close() {
        if (replayCache != null) {
            try {
                replayCache.close();
            } catch (IOException ex) {
                LOG.warning("Replay cache can not be closed: " + ex.getMessage());
            }
        }
        super.close();
    }
    
    protected Response doProcessSamlResponse(String encodedSamlResponse,
                                             String relayState,
                                             boolean postBinding) {
        RequestState requestState = processRelayState(relayState);
       
        String contextCookie = createSecurityContext(requestState,
                                                    encodedSamlResponse,
                                                   relayState,
                                                   postBinding);
       
        // Finally, redirect to the service provider endpoint
        URI targetURI = getTargetURI(requestState.getTargetAddress());
        return Response.seeOther(targetURI).header("Set-Cookie", contextCookie).build();
    }
    
    private URI getTargetURI(String targetAddress) {
        if (targetAddress != null) {
            try {
                return URI.create(targetAddress);
            } catch (IllegalArgumentException ex) {
                reportError("INVALID_TARGET_URI");
            }
        } else {
            reportError("MISSING_TARGET_URI");
        }
        throw ExceptionUtils.toBadRequestException(null, null);
    }
    
    protected String createSecurityContext(RequestState requestState,
                                           String encodedSamlResponse,
                                           String relayState,
                                           boolean postBinding) {
           
        org.opensaml.saml.saml2.core.Response samlResponse = 
               readSAMLResponse(postBinding, encodedSamlResponse);

        // Validate the Response
        validateSamlResponseProtocol(samlResponse);
        SSOValidatorResponse validatorResponse = 
            validateSamlSSOResponse(postBinding, samlResponse, requestState);
           
        // Set the security context
        String securityContextKey = UUID.randomUUID().toString();
           
        long currentTime = System.currentTimeMillis();
        Date notOnOrAfter = validatorResponse.getSessionNotOnOrAfter();
        long expiresAt = 0;
        if (notOnOrAfter != null) {
            expiresAt = notOnOrAfter.getTime();
        } else {
            expiresAt = currentTime + getStateTimeToLive(); 
        }
           
        ResponseState responseState = 
            new ResponseState(validatorResponse.getAssertion(),
                              relayState, 
                              requestState.getWebAppContext(),
                              requestState.getWebAppDomain(),
                              currentTime, 
                              expiresAt);
        getStateProvider().setResponseState(securityContextKey, responseState);
           
        String contextCookie = createCookie(SSOConstants.SECURITY_CONTEXT_TOKEN,
                                            securityContextKey,
                                            requestState.getWebAppContext(),
                                            requestState.getWebAppDomain());
           
        return contextCookie;
           
    }
    
    protected RequestState processRelayState(String relayState) {
        if (relayState == null) {
            reportError("MISSING_RELAY_STATE");
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        if (relayState.getBytes().length == 0 || relayState.getBytes().length > 80) {
            reportError("INVALID_RELAY_STATE");
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        RequestState requestState = getStateProvider().removeRequestState(relayState);
        if (requestState == null) {
            reportError("MISSING_REQUEST_STATE");
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        if (isStateExpired(requestState.getCreatedAt(), 0)) {
            reportError("EXPIRED_REQUEST_STATE");
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        return requestState;
    }
    
    private org.opensaml.saml.saml2.core.Response readSAMLResponse(
        boolean postBinding,
        String samlResponse
    ) {
        if (StringUtils.isEmpty(samlResponse)) {
            reportError("MISSING_SAML_RESPONSE");
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        
        String samlResponseDecoded = samlResponse;
        /*
        // URL Decoding only applies for the re-direct binding
        if (!postBinding) {
            try {
                samlResponseDecoded = URLDecoder.decode(samlResponse, StandardCharsets.UTF_8);
            } catch (UnsupportedEncodingException e) {
                throw ExceptionUtils.toBadRequestException(null, null);
            }
        }
        */
        InputStream tokenStream = null;
        if (isSupportBase64Encoding()) {
            try {
                byte[] deflatedToken = Base64Utility.decode(samlResponseDecoded);
                tokenStream = !postBinding && isSupportDeflateEncoding() 
                    ? new DeflateEncoderDecoder().inflateToken(deflatedToken)
                    : new ByteArrayInputStream(deflatedToken); 
            } catch (Base64Exception ex) {
                throw ExceptionUtils.toBadRequestException(ex, null);
            } catch (DataFormatException ex) {
                throw ExceptionUtils.toBadRequestException(ex, null);
            }
        } else {
            tokenStream = new ByteArrayInputStream(samlResponseDecoded.getBytes(StandardCharsets.UTF_8));
        }
        
        Document responseDoc = null;
        try {
            responseDoc = StaxUtils.read(new InputStreamReader(tokenStream, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new WebApplicationException(400);
        }
        
        LOG.fine("Received response: " + DOM2Writer.nodeToString(responseDoc.getDocumentElement()));
        
        XMLObject responseObject = null;
        try {
            responseObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        } catch (WSSecurityException ex) {
            throw ExceptionUtils.toBadRequestException(ex, null);
        }
        if (!(responseObject instanceof org.opensaml.saml.saml2.core.Response)) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        return (org.opensaml.saml.saml2.core.Response)responseObject;
    }
    
    /**
     * Validate the received SAML Response as per the protocol
     */
    protected void validateSamlResponseProtocol(
        org.opensaml.saml.saml2.core.Response samlResponse
    ) {
        try {
            SAMLProtocolResponseValidator protocolValidator = new SAMLProtocolResponseValidator();
            protocolValidator.setKeyInfoMustBeAvailable(keyInfoMustBeAvailable);
            protocolValidator.validateSamlResponse(samlResponse, getSignatureCrypto(), getCallbackHandler());
        } catch (WSSecurityException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            reportError("INVALID_SAML_RESPONSE");
            throw ExceptionUtils.toBadRequestException(null, null);
        }
    }
    
    /**
     * Validate the received SAML Response as per the Web SSO profile
     */
    protected SSOValidatorResponse validateSamlSSOResponse(
        boolean postBinding,
        org.opensaml.saml.saml2.core.Response samlResponse,
        RequestState requestState
    ) {
        try {
            SAMLSSOResponseValidator ssoResponseValidator = new SAMLSSOResponseValidator();
            ssoResponseValidator.setAssertionConsumerURL(
                messageContext.getUriInfo().getAbsolutePath().toString());

            ssoResponseValidator.setClientAddress(
                 messageContext.getHttpServletRequest().getRemoteAddr());

            ssoResponseValidator.setIssuerIDP(requestState.getIdpServiceAddress());
            ssoResponseValidator.setRequestId(requestState.getSamlRequestId());
            ssoResponseValidator.setSpIdentifier(requestState.getIssuerId());
            ssoResponseValidator.setEnforceAssertionsSigned(enforceAssertionsSigned);
            ssoResponseValidator.setEnforceResponseSigned(enforceResponseSigned);
            ssoResponseValidator.setEnforceKnownIssuer(enforceKnownIssuer);
            if (postBinding) {
                ssoResponseValidator.setReplayCache(getReplayCache());
            }

            return ssoResponseValidator.validateSamlResponse(samlResponse, postBinding);
        } catch (WSSecurityException ex) {
            reportError("INVALID_SAML_RESPONSE");
            throw ExceptionUtils.toBadRequestException(ex, null);
        }
    }
    
    protected void reportError(String code) {
        org.apache.cxf.common.i18n.Message errorMsg = 
            new org.apache.cxf.common.i18n.Message(code, BUNDLE);
        LOG.warning(errorMsg.toString());
    }
    
    public void setKeyInfoMustBeAvailable(boolean keyInfoMustBeAvailable) {
        this.keyInfoMustBeAvailable = keyInfoMustBeAvailable;
    }

    public boolean isEnforceResponseSigned() {
        return enforceResponseSigned;
    }

    /**
     * Enforce that a SAML Response must be signed.
     */
    public void setEnforceResponseSigned(boolean enforceResponseSigned) {
        this.enforceResponseSigned = enforceResponseSigned;
    }
}
