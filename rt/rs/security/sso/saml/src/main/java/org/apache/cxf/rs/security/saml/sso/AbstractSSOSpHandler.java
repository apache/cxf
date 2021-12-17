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
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import jakarta.annotation.PreDestroy;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.sso.state.SPStateManager;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;

public class AbstractSSOSpHandler {
    private static final Logger LOG =
            LogUtils.getL7dLogger(AbstractSSOSpHandler.class);

    private SPStateManager stateProvider;
    private long stateTimeToLive = SSOConstants.DEFAULT_STATE_TIME;
    private Crypto signatureCrypto;
    private String signaturePropertiesFile;
    private CallbackHandler callbackHandler;
    private String callbackHandlerClass;
    private String signatureUsername;
    private String idpServiceAddress;
    private String issuerId;
    private boolean supportUnsolicited;

    static {
        OpenSAMLUtil.initSamlEngine();
    }

    @PreDestroy
    public void close() {
        if (stateProvider != null) {
            try {
                stateProvider.close();
            } catch (IOException ex) {
                LOG.warning("State provider can not be closed: " + ex.getMessage());
            }
            stateProvider = null;
        }
    }

    public void setSignatureCrypto(Crypto crypto) {
        signatureCrypto = crypto;
    }

    /**
     * Set the String corresponding to the signature Properties class
     * @param signaturePropertiesFile the String corresponding to the signature properties file
     */
    public void setSignaturePropertiesFile(String signaturePropertiesFile) {
        this.signaturePropertiesFile = signaturePropertiesFile;
        LOG.fine("Setting signature properties: " + signaturePropertiesFile);
    }

    /**
     * Set the CallbackHandler object.
     * @param callbackHandler the CallbackHandler object.
     */
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
        LOG.fine("Setting callbackHandler: " + callbackHandler);
    }

    /**
     * Set the String corresponding to the CallbackHandler class.
     * @param callbackHandlerClass the String corresponding to the CallbackHandler class.
     */
    public void setCallbackHandlerClass(String callbackHandlerClass) {
        this.callbackHandlerClass = callbackHandlerClass;
        LOG.fine("Setting callbackHandlerClass: " + callbackHandlerClass);
    }

    //TODO: support attaching a signature to the cookie value
    protected String createCookie(String name,
                                  String value,
                                  String path,
                                  String domain) {

        String contextCookie = name + "=" + value;
        // Setting a specific path restricts the browsers
        // to return a cookie only to the web applications
        // listening on that specific context path
        if (path != null) {
            contextCookie += ";Path=" + path;
        }

        // Setting a specific domain further restricts the browsers
        // to return a cookie only to the web applications
        // listening on the specific context path within a particular domain
        if (domain != null) {
            contextCookie += ";Domain=" + domain;
        }

        if (stateTimeToLive > 0) {
            // Keep the cookie across the browser restarts until it actually expires.
            // Note that the Expires property has been deprecated but apparently is
            // supported better than 'max-age' property by different browsers
            // (Firefox, IE, etc)
            Instant expires = Instant.ofEpochMilli(System.currentTimeMillis() + stateTimeToLive);
            String cookieExpires =
                HttpUtils.getHttpDateFormat().format(Date.from(expires.atZone(ZoneOffset.UTC).toInstant()));
            contextCookie += ";Expires=" + cookieExpires;
        }

        //TODO: Consider adding an 'HttpOnly' attribute

        return contextCookie;
    }

    public void setStateProvider(SPStateManager stateProvider) {
        this.stateProvider = stateProvider;
    }

    public SPStateManager getStateProvider() {
        return stateProvider;
    }

    public void setStateTimeToLive(long stateTimeToLive) {
        this.stateTimeToLive = stateTimeToLive;
    }

    public long getStateTimeToLive() {
        return stateTimeToLive;
    }

    protected Crypto getSignatureCrypto() {
        if (signatureCrypto == null && signaturePropertiesFile != null) {
            Properties sigProperties = SecurityUtils.loadProperties(signaturePropertiesFile);
            if (sigProperties == null) {
                LOG.warning("Cannot load signature properties using: " + signaturePropertiesFile);
                return null;
            }
            try {
                signatureCrypto = CryptoFactory.getInstance(sigProperties);
            } catch (WSSecurityException ex) {
                LOG.warning("Error in loading the signature Crypto object: " + ex.getMessage());
                return null;
            }
        }
        return signatureCrypto;
    }

    protected CallbackHandler getCallbackHandler() {
        if (callbackHandler == null && callbackHandlerClass != null) {
            try {
                callbackHandler = SecurityUtils.getCallbackHandler(callbackHandlerClass);
                if (callbackHandler == null) {
                    LOG.warning("Cannot load CallbackHandler using: " + callbackHandlerClass);
                    return null;
                }
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Error in loading callback handler", ex);
                return null;
            }
        }
        return callbackHandler;
    }

    /**
     * Set the username/alias to use to sign any request
     * @param signatureUsername the username/alias to use to sign any request
     */
    public void setSignatureUsername(String signatureUsername) {
        this.signatureUsername = signatureUsername;
        LOG.fine("Setting signatureUsername: " + signatureUsername);
    }

    /**
     * Get the username/alias to use to sign any request
     * @return the username/alias to use to sign any request
     */
    public String getSignatureUsername() {
        return signatureUsername;
    }

    public void setIdpServiceAddress(String idpServiceAddress) {
        this.idpServiceAddress = idpServiceAddress;
    }

    public String getIdpServiceAddress() {
        return idpServiceAddress;
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }

    protected String getIssuerId(Message m) {
        if (issuerId == null) {
            return new UriInfoImpl(m).getBaseUri().toString();
        }
        return issuerId;
    }

    public boolean isSupportUnsolicited() {
        return supportUnsolicited;
    }

    /**
     * Whether to support unsolicited IdP initiated login or not. The default
     * is false.
     */
    public void setSupportUnsolicited(boolean supportUnsolicited) {
        this.supportUnsolicited = supportUnsolicited;
    }
}
