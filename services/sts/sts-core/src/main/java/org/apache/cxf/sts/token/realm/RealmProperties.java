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

package org.apache.cxf.sts.token.realm;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.sts.SignatureProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;


/**
 * This class defines some properties that are associated with a realm for issuing or validating a particular token.
 */
public class RealmProperties {

    private static final Logger LOG = LogUtils.getL7dLogger(RealmProperties.class);

    private String name;
    private String issuer;
    private String signatureAlias;
    private Crypto signatureCrypto;
    private SignatureProperties signatureProperties;
    private Object signatureCryptoProperties;
    private String callbackHandlerClass;
    private CallbackHandler callbackHandler;

    /**
     * Get the issuer of this realm
     * @return the issuer of this realm
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Get the name of this realm
     * @return realmA
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this realm
     * @param name the name of this realm
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the issuer of this realm
     * @param issuer the issuer of this realm
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Get the signature alias to use for this realm
     * @return the signature alias to use for this realm
     */
    public String getSignatureAlias() {
        return signatureAlias;
    }

    /**
     * Set the signature alias to use for this realm
     * @param signatureAlias the signature alias to use for this realm
     */
    public void setSignatureAlias(String signatureAlias) {
        this.signatureAlias = signatureAlias;
    }

    /**
     * Set the signature Crypto object
     * @param signatureCrypto the signature Crypto object
     */
    public void setSignatureCrypto(Crypto signatureCrypto) {
        this.signatureCrypto = signatureCrypto;
    }

    /**
     * Set the Object corresponding to the signature Properties class. It can be a String
     * corresponding to a filename, a Properties object, or a URL.
     * @param signatureCryptoProperties the object corresponding to the signature properties
     */
    public void setSignatureCryptoProperties(Object signatureCryptoProperties) {
        this.signatureCryptoProperties = signatureCryptoProperties;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting signature crypto properties: " + signatureCryptoProperties);
        }
    }

    /**
     * Set the SignatureProperties to use.
     * @param signatureProperties the SignatureProperties to use.
     */
    public void setSignatureProperties(SignatureProperties signatureProperties) {
        this.signatureProperties = signatureProperties;
    }

    /**
     * Get the SignatureProperties to use.
     * @return the SignatureProperties to use.
     */
    public SignatureProperties getSignatureProperties() {
        return signatureProperties;
    }


    /**
     * Get the signature Crypto object
     * @return the signature Crypto object
     */
    public Crypto getSignatureCrypto() {
        if (signatureCrypto == null && signatureCryptoProperties != null) {
            Properties sigProperties = SecurityUtils.loadProperties(signatureCryptoProperties);
            if (sigProperties == null) {
                LOG.fine("Cannot load signature properties using: " + signatureCryptoProperties);
                throw new STSException("Configuration error: cannot load signature properties");
            }
            try {
                signatureCrypto = CryptoFactory.getInstance(sigProperties);
            } catch (WSSecurityException ex) {
                LOG.fine("Error in loading the signature Crypto object: " + ex.getMessage());
                throw new STSException(ex.getMessage());
            }
        }

        return signatureCrypto;
    }


    /**
     * Set the CallbackHandler object.
     * @param callbackHandler the CallbackHandler object.
     */
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting callbackHandler: " + callbackHandler);
        }
    }

    /**
     * Set the String corresponding to the CallbackHandler class.
     * @param callbackHandlerClass the String corresponding to the CallbackHandler class.
     */
    public void setCallbackHandlerClass(String callbackHandlerClass) {
        this.callbackHandlerClass = callbackHandlerClass;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting callbackHandlerClass: " + callbackHandlerClass);
        }
    }

    /**
     * Get the CallbackHandler object.
     * @return the CallbackHandler object.
     */
    public CallbackHandler getCallbackHandler() {
        if (callbackHandler == null && callbackHandlerClass != null) {
            try {
                callbackHandler = SecurityUtils.getCallbackHandler(callbackHandlerClass);
                if (callbackHandler == null) {
                    LOG.fine("Cannot load CallbackHandler using: " + callbackHandlerClass);
                    throw new STSException("Configuration error: cannot load callback handler");
                }
            } catch (Exception ex) {
                LOG.fine("Error in loading the callback handler object: " + ex.getMessage());
                throw new STSException(ex.getMessage());
            }
        }
        return callbackHandler;
    }

}
