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
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

import org.w3c.dom.Document;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
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
    private boolean checkClientAddress = true;
    private boolean enforceResponseSigned;
    private TokenReplayCache<String> replayCache;

    private MessageContext messageContext;
    private String applicationURL;
    private boolean parseApplicationURLFromRelayState;
    private String assertionConsumerServiceAddress;

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

    public TokenReplayCache<String> getReplayCache() throws Exception {
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
        Instant notOnOrAfter = validatorResponse.getSessionNotOnOrAfter();
        final long expiresAt;
        if (notOnOrAfter != null) {
            expiresAt = notOnOrAfter.toEpochMilli();
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

        return createCookie(SSOConstants.SECURITY_CONTEXT_TOKEN,
                            securityContextKey,
                            requestState.getWebAppContext(),
                            requestState.getWebAppDomain());
    }

    protected RequestState processRelayState(String relayState) {
        if (isSupportUnsolicited()) {
            String urlToForwardTo = applicationURL;
            if (relayState != null && relayState.getBytes().length > 0 && relayState.getBytes().length < 80) {
                // First see if we have a valid RequestState
                RequestState requestState = getStateProvider().removeRequestState(relayState);
                if (requestState != null
                    && !isStateExpired(requestState.getCreatedAt(), requestState.getTimeToLive())) {
                    return requestState;
                }

                // Otherwise get the application URL from the RelayState if supported
                if (parseApplicationURLFromRelayState) {
                    urlToForwardTo = relayState;
                }
            }

            // Otherwise create a new one for the IdP initiated case
            Instant now = Instant.now();
            return new RequestState(urlToForwardTo,
                                    getIdpServiceAddress(),
                                    null,
                                    getIssuerId(JAXRSUtils.getCurrentMessage()),
                                    "/",
                                    null,
                                    now.toEpochMilli(),
                                    getStateTimeToLive());
        }

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
        if (isStateExpired(requestState.getCreatedAt(), requestState.getTimeToLive())) {
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
        final Reader reader;
        if (isSupportBase64Encoding()) {
            try {
                byte[] deflatedToken = Base64Utility.decode(samlResponseDecoded);
                final InputStream tokenStream = !postBinding && isSupportDeflateEncoding()
                    ? new DeflateEncoderDecoder().inflateToken(deflatedToken)
                    : new ByteArrayInputStream(deflatedToken);
                reader = new InputStreamReader(tokenStream, StandardCharsets.UTF_8);
            } catch (Base64Exception | DataFormatException ex) {
                throw ExceptionUtils.toBadRequestException(ex, null);
            }
        } else {
            reader = new StringReader(samlResponseDecoded);
        }

        final Document responseDoc;
        try {
            responseDoc = StaxUtils.read(reader);
        } catch (Exception ex) {
            throw new WebApplicationException(400);
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Received response: " + DOM2Writer.nodeToString(responseDoc.getDocumentElement()));
        }

        final XMLObject responseObject;
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
            String racsAddress = assertionConsumerServiceAddress;
            if (racsAddress == null) {
                racsAddress = messageContext.getUriInfo().getAbsolutePath().toString();
            }
            ssoResponseValidator.setAssertionConsumerURL(racsAddress);

            if (checkClientAddress) {
                ssoResponseValidator.setClientAddress(
                    messageContext.getHttpServletRequest().getRemoteAddr());
            }

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
        } catch (Exception ex) {
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

    public String getApplicationURL() {
        return applicationURL;
    }

    /**
     * Set the Application URL to forward to, for the unsolicited IdP case.
     * @param applicationURL
     */
    public void setApplicationURL(String applicationURL) {
        this.applicationURL = applicationURL;
    }

    public boolean isParseApplicationURLFromRelayState() {
        return parseApplicationURLFromRelayState;
    }

    /**
     * Whether to parse the application URL to forward to from the RelayState, for the unsolicted IdP case.
     * @param parseApplicationURLFromRelayState
     */
    public void setParseApplicationURLFromRelayState(boolean parseApplicationURLFromRelayState) {
        this.parseApplicationURLFromRelayState = parseApplicationURLFromRelayState;
    }

    public String getAssertionConsumerServiceAddress() {
        return assertionConsumerServiceAddress;
    }

    public void setAssertionConsumerServiceAddress(String assertionConsumerServiceAddress) {
        this.assertionConsumerServiceAddress = assertionConsumerServiceAddress;
    }

    public boolean isCheckClientAddress() {
        return checkClientAddress;
    }

    public void setCheckClientAddress(boolean checkClientAddress) {
        this.checkClientAddress = checkClientAddress;
    }

    protected boolean isStateExpired(long stateCreatedAt, long expiresAt) {
        Instant currentTime = Instant.now();
        return expiresAt > 0 && currentTime.isAfter(Instant.ofEpochMilli(stateCreatedAt + expiresAt));
    }

}
