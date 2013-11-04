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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.realm.Relationship;
import org.apache.cxf.sts.token.realm.RelationshipResolver;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;

/**
 * A static implementation of the STSPropertiesMBean.
 */
public class StaticSTSProperties implements STSPropertiesMBean {
    
    private static final Logger LOG = LogUtils.getL7dLogger(StaticSTSProperties.class);
    
    private CallbackHandler callbackHandler;
    private String callbackHandlerClass;
    private Crypto signatureCrypto;
    private String signaturePropertiesFile;
    private String signatureUsername;
    private Crypto encryptionCrypto;
    private String encryptionPropertiesFile;
    private String encryptionUsername;
    private String issuer;
    private SignatureProperties signatureProperties = new SignatureProperties();
    private EncryptionProperties encryptionProperties = new EncryptionProperties();
    private RealmParser realmParser;
    private IdentityMapper identityMapper;
    private List<Relationship> relationships;
    private RelationshipResolver relationshipResolver;
    private Bus bus;

    /**
     * Load the CallbackHandler, Crypto objects, if necessary.
     */
    public void configureProperties() throws STSException {
        if (signatureCrypto == null && signaturePropertiesFile != null) {
            Properties sigProperties = getProps(signaturePropertiesFile, bus);
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
        
        if (encryptionCrypto == null && encryptionPropertiesFile != null) {
            Properties encrProperties = getProps(encryptionPropertiesFile, bus);
            if (encrProperties == null) {
                LOG.fine("Cannot load encryption properties using: " + encryptionPropertiesFile);
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
            callbackHandler = getCallbackHandler(callbackHandlerClass);
            if (callbackHandler == null) {
                LOG.fine("Cannot load CallbackHandler using: " + callbackHandlerClass);
                throw new STSException("Configuration error: cannot load callback handler");
            }
        }
        WSSConfig.init();
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
    public void setSignaturePropertiesFile(String signaturePropertiesFile) {
        this.signaturePropertiesFile = signaturePropertiesFile;
        LOG.fine("Setting signature properties: " + signaturePropertiesFile);
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
        LOG.fine("Setting signatureUsername: " + signatureUsername);
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
    public void setEncryptionPropertiesFile(String encryptionPropertiesFile) {
        this.encryptionPropertiesFile = encryptionPropertiesFile;
        LOG.fine("Setting encryptionProperties: " + encryptionPropertiesFile);
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
        LOG.fine("Setting encryptionUsername: " + encryptionUsername);
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
        LOG.fine("Setting issuer: " + issuer);
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
    
    private static Properties getProps(Object o, Bus bus) {
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            URL url = null;
            Bus b = bus;
            if (b == null) {
                b = BusFactory.getThreadDefaultBus();
            }
            ResourceManager rm = b.getExtension(ResourceManager.class);
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
    
    private CallbackHandler getCallbackHandler(Object o) {
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = 
                    (CallbackHandler)ClassLoaderUtils.loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                LOG.fine(e.getMessage());
                handler = null;
            }
        }
        return handler;
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

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
}
