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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.sts.SignatureProperties;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;


/**
 * This class defines some properties that are associated with a realm for the SAMLTokenProvider and
 * SAMLTokenValidator.
 */
public class SAMLRealm {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SAMLRealm.class);
    
    private String issuer;
    private String signatureAlias;
    private Crypto signatureCrypto;
    private SignatureProperties signatureProperties;
    private String signaturePropertiesFile;
    private String callbackHandlerClass;
    private CallbackHandler callbackHandler;
    
    /**
     * Get the issuer of this SAML realm
     * @return the issuer of this SAML realm
     */
    public String getIssuer() {
        return issuer;
    }
    
    /**
     * Set the issuer of this SAML realm
     * @param issuer the issuer of this SAML realm
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    /**
     * Get the signature alias to use for this SAML realm
     * @return the signature alias to use for this SAML realm
     */
    public String getSignatureAlias() {
        return signatureAlias;
    }
    
    /**
     * Set the signature alias to use for this SAML realm
     * @param signatureAlias the signature alias to use for this SAML realm
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
     * Set the String corresponding to the signature Properties class
     * @param signaturePropertiesFile the String corresponding to the signature properties file
     */
    public void setSignaturePropertiesFile(String signaturePropertiesFile) {
        this.signaturePropertiesFile = signaturePropertiesFile;
        LOG.fine("Setting signature properties: " + signaturePropertiesFile);
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
        if (signatureCrypto == null && signaturePropertiesFile != null) {
            Properties sigProperties = getProps(signaturePropertiesFile);
            if (sigProperties == null) {
                LOG.fine("Cannot load signature properties using: " + signaturePropertiesFile);
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
            } catch (WSSecurityException ex) {
                LOG.fine("Error in loading the callback handler object: " + ex.getMessage());
                throw new STSException(ex.getMessage());
            }
        }
        return callbackHandler;
    }
    
    private static Properties getProps(Object o) {
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            URL url = null;
            Bus bus = PhaseInterceptorChain.getCurrentMessage().getExchange().getBus();
            ResourceManager rm = bus.getExtension(ResourceManager.class);
            url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, StaticSTSProperties.class);
                }
                if (url == null) {
                    url = new URL((String)o);
                }
                if (url != null) {
                    properties = new Properties();
                    InputStream ins = url.openStream();
                    properties.load(ins);
                    ins.close();
                }
            } catch (IOException e) {
                LOG.fine(e.getMessage());
                properties = null;
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            try {
                InputStream ins = ((URL)o).openStream();
                properties.load(ins);
                ins.close();
            } catch (IOException e) {
                LOG.fine(e.getMessage());
                properties = null;
            }            
        }
        return properties;
    }
    
}
