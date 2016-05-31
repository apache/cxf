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
package org.apache.cxf.sts;

import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.realm.Relationship;
import org.apache.cxf.sts.token.realm.RelationshipResolver;
import org.apache.cxf.sts.token.realm.SAMLRealmCodec;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.engine.WSSConfig;

/**
 * A static implementation of the STSPropertiesMBean.
 */
public class StaticSTSProperties implements STSPropertiesMBean {
    
    private static final Logger LOG = LogUtils.getL7dLogger(StaticSTSProperties.class);
    
    private CallbackHandler callbackHandler;
    private String callbackHandlerClass;
    private Crypto signatureCrypto;
    private Object signatureCryptoProperties;
    private String signatureUsername;
    private Crypto encryptionCrypto;
    private Object encryptionCryptoProperties;
    private String encryptionUsername;
    private String issuer;
    private SignatureProperties signatureProperties = new SignatureProperties();
    private EncryptionProperties encryptionProperties = new EncryptionProperties();
    private RealmParser realmParser;
    private IdentityMapper identityMapper;
    private List<Relationship> relationships;
    private RelationshipResolver relationshipResolver;
    private SAMLRealmCodec samlRealmCodec;
    private Bus bus;
    private boolean validateUseKey = true;

    /**
     * Load the CallbackHandler, Crypto objects, if necessary.
     */
    public void configureProperties() throws STSException {
        if (signatureCrypto == null && signatureCryptoProperties != null) {
            ResourceManager resourceManager = getResourceManager();
            Properties sigProperties = SecurityUtils.loadProperties(resourceManager, signatureCryptoProperties);
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
        
        if (encryptionCrypto == null && encryptionCryptoProperties != null) {
            ResourceManager resourceManager = getResourceManager();
            Properties encrProperties = SecurityUtils.loadProperties(resourceManager, encryptionCryptoProperties);
            if (encrProperties == null) {
                LOG.fine("Cannot load encryption properties using: " + encryptionCryptoProperties);
                throw new STSException("Configuration error: cannot load encryption properties");
            }
            try {
                encryptionCrypto = CryptoFactory.getInstance(encrProperties);
            } catch (WSSecurityException ex) {
                LOG.fine("Error in loading the encryption Crypto object: " + ex.getMessage());
                throw new STSException(ex.getMessage());
            }
        }
        
        if (callbackHandler == null && callbackHandlerClass != null) {
            try {
                callbackHandler = SecurityUtils.getCallbackHandler(callbackHandlerClass);
                if (callbackHandler == null) {
                    LOG.fine("Cannot load CallbackHandler using: " + callbackHandlerClass);
                    throw new STSException("Configuration error: cannot load callback handler");
                }
            } catch (Exception ex) {
                LOG.fine("Error in loading the callback handler: " + ex.getMessage());
                throw new STSException(ex.getMessage());
            }
        }
        WSSConfig.init();
    }
    
    private ResourceManager getResourceManager() {
        Bus b = bus;
        if (b == null) {
            b = BusFactory.getThreadDefaultBus();
        }
        return b.getExtension(ResourceManager.class);
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
        return callbackHandler;
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
    @Deprecated
    public void setSignaturePropertiesFile(String signaturePropertiesFile) {
        setSignatureCryptoProperties(signaturePropertiesFile);
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
     * Get the signature Crypto object
     * @return the signature Crypto object
     */
    public Crypto getSignatureCrypto() {
        return signatureCrypto;
    }
    
    /**
     * Set the username/alias to use to sign any issued tokens
     * @param signatureUsername the username/alias to use to sign any issued tokens
     */
    public void setSignatureUsername(String signatureUsername) {
        this.signatureUsername = signatureUsername;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting signatureUsername: " + signatureUsername);
        }
    }
    
    /**
     * Get the username/alias to use to sign any issued tokens
     * @return the username/alias to use to sign any issued tokens
     */
    public String getSignatureUsername() {
        return signatureUsername;
    }
    
    /**
     * Set the encryption Crypto object
     * @param encryptionCrypto the encryption Crypto object
     */
    public void setEncryptionCrypto(Crypto encryptionCrypto) {
        this.encryptionCrypto = encryptionCrypto;
    }
    
    /**
     * Set the String corresponding to the encryption Properties class
     * @param signaturePropertiesFile the String corresponding to the encryption properties file
     */
    @Deprecated
    public void setEncryptionPropertiesFile(String encryptionPropertiesFile) {
        setEncryptionCryptoProperties(encryptionPropertiesFile);
    }
    
    /**
     * Set the Object corresponding to the encryption Properties class. It can be a String
     * corresponding to a filename, a Properties object, or a URL.
     * @param encryptionCryptoProperties the object corresponding to the encryption properties
     */
    public void setEncryptionCryptoProperties(Object encryptionCryptoProperties) {
        this.encryptionCryptoProperties = encryptionCryptoProperties;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting encryptionProperties: " + encryptionCryptoProperties);
        }
    }
    
    /**
     * Get the encryption Crypto object
     * @return the encryption Crypto object
     */
    public Crypto getEncryptionCrypto() {
        return encryptionCrypto;
    }
    
    /**
     * Set the username/alias to use to encrypt any issued tokens. This is a default value - it
     * can be configured per Service in the ServiceMBean.
     * @param encryptionUsername the username/alias to use to encrypt any issued tokens
     */
    public void setEncryptionUsername(String encryptionUsername) {
        this.encryptionUsername = encryptionUsername;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting encryptionUsername: " + encryptionUsername);
        }
    }
    
    /**
     * Get the username/alias to use to encrypt any issued tokens. This is a default value - it
     * can be configured per Service in the ServiceMBean
     * @return the username/alias to use to encrypt any issued tokens
     */
    public String getEncryptionUsername() {
        return encryptionUsername;
    }
    
    /**
     * Set the EncryptionProperties to use.
     * @param encryptionProperties the EncryptionProperties to use.
     */
    public void setEncryptionProperties(EncryptionProperties encryptionProperties) {
        this.encryptionProperties = encryptionProperties;
    }
    
    /**
     * Get the EncryptionProperties to use.
     * @return the EncryptionProperties to use.
     */
    public EncryptionProperties getEncryptionProperties() {
        return encryptionProperties;
    }
    
    /**
     * Set the STS issuer name
     * @param issuer the STS issuer name
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting issuer: " + issuer);
        }
    }
    
    /**
     * Get the STS issuer name
     * @return the STS issuer name
     */
    public String getIssuer() {
        return issuer;
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
     * Set the RealmParser object to use.
     * @param realmParser the RealmParser object to use.
     */
    public void setRealmParser(RealmParser realmParser) {
        this.realmParser = realmParser;
    }
    
    /**
     * Get the RealmParser object to use.
     * @return the RealmParser object to use.
     */
    public RealmParser getRealmParser() {
        return realmParser;
    }
    
    /**
     * Set the IdentityMapper object to use.
     * @param identityMapper the IdentityMapper object to use.
     */
    public void setIdentityMapper(IdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }
    
    /**
     * Get the IdentityMapper object to use.
     * @return the IdentityMapper object to use.
     */
    public IdentityMapper getIdentityMapper() {
        return identityMapper;
    }
    
    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
        this.relationshipResolver = new RelationshipResolver(this.relationships);
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }
    
    public RelationshipResolver getRelationshipResolver() {
        return relationshipResolver;      
    }

    public SAMLRealmCodec getSamlRealmCodec() {
        return samlRealmCodec;
    }

    public void setSamlRealmCodec(SAMLRealmCodec samlRealmCodec) {
        this.samlRealmCodec = samlRealmCodec;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    /**
     * Get whether to validate a client Public Key or Certificate presented as part of a 
     * UseKey element. This is true by default.
     */
    public boolean isValidateUseKey() {
        return validateUseKey;
    }
    
    /**
     * Set whether to validate a client Public Key or Certificate presented as part of a 
     * UseKey element. If this is set to true (the default), the public key must be trusted
     * by the Signature Crypto of the STS.
     * 
     * @param validateUseKey whether to validate a client UseKey or not.
     */
    public void setValidateUseKey(boolean validateUseKey) {
        this.validateUseKey = validateUseKey;
    }
}
