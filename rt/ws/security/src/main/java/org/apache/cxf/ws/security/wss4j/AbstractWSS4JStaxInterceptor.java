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
package org.apache.cxf.ws.security.wss4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.JasyptPasswordEncryptor;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;

public abstract class AbstractWSS4JStaxInterceptor implements SoapInterceptor, 
    PhaseInterceptor<SoapMessage> {

    private static final Set<QName> HEADERS = new HashSet<QName>();
    static {
        HEADERS.add(new QName(WSSConstants.NS_WSSE10, "Security"));
        HEADERS.add(new QName(WSSConstants.NS_WSSE11, "Security"));
        HEADERS.add(new QName(WSSConstants.NS_XMLENC, "EncryptedData"));
    }
    
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractWSS4JStaxInterceptor.class);

    private Map<String, Object> properties = new ConcurrentHashMap<String, Object>();
    private Map<String, Crypto> cryptos = new ConcurrentHashMap<String, Crypto>();
    private WSSSecurityProperties securityProperties;
    private Set<String> before = new HashSet<String>();
    private Set<String> after = new HashSet<String>();
    private String phase;
    private String id;
    
    public AbstractWSS4JStaxInterceptor() {
        super();
        id = getClass().getName();
    }
    
    public AbstractWSS4JStaxInterceptor(Map<String, Object> properties) {
        this();
        this.properties.putAll(properties);
    }
    
    protected void translateProperties(SoapMessage msg) {
        String bspCompliant = (String)msg.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
        if (bspCompliant != null) {
            if (securityProperties != null) {
                securityProperties.setDisableBSPEnforcement(Boolean.valueOf(bspCompliant));
            } else {
                properties.put(ConfigurationConstants.IS_BSP_COMPLIANT, bspCompliant);
            }
        }
        String futureTTL = 
            (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_FUTURE_TTL);
        if (futureTTL != null) {
            if (securityProperties != null) {
                securityProperties.setTimeStampFutureTTL(Integer.parseInt(futureTTL));
            } else {
                properties.put(ConfigurationConstants.TTL_FUTURE_TIMESTAMP, futureTTL);
            }
        }
        String ttl = 
            (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_TTL);
        if (ttl != null) {
            if (securityProperties != null) {
                securityProperties.setTimestampTTL(Integer.parseInt(ttl));
            } else {
                properties.put(ConfigurationConstants.TTL_TIMESTAMP, ttl);
            }
        }
        
        String utFutureTTL = 
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_FUTURE_TTL);
        if (utFutureTTL != null) {
            if (securityProperties != null) {
                securityProperties.setUtFutureTTL(Integer.parseInt(utFutureTTL));
            } else {
                properties.put(ConfigurationConstants.TTL_FUTURE_USERNAMETOKEN, utFutureTTL);
            }
        }
        String utTTL = 
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_TTL);
        if (utTTL != null) {
            if (securityProperties != null) {
                securityProperties.setUtTTL(Integer.parseInt(utTTL));
            } else {
                properties.put(ConfigurationConstants.TTL_USERNAMETOKEN, utTTL);
            }
        }
        
        String certConstraints = 
            (String)msg.getContextualProperty(SecurityConstants.SUBJECT_CERT_CONSTRAINTS);
        if (certConstraints != null) {
            if (securityProperties != null) {
                securityProperties.setSubjectCertConstraints(convertCertConstraints(certConstraints));
            } else {
                properties.put(ConfigurationConstants.SIG_SUBJECT_CERT_CONSTRAINTS, certConstraints);
            }
        }
        
        // Now set SAML SenderVouches + Holder Of Key requirements
        String validateSAMLSubjectConf = 
            (String)msg.getContextualProperty(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION);
        if (validateSAMLSubjectConf != null) {
            if (securityProperties != null) {
                securityProperties.setValidateSamlSubjectConfirmation(Boolean.valueOf(validateSAMLSubjectConf));
            } else {
                properties.put(ConfigurationConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, 
                               validateSAMLSubjectConf);
            }
        }
        
        String actor = (String)msg.getContextualProperty(SecurityConstants.ACTOR);
        if (actor != null) {
            if (securityProperties != null) {
                securityProperties.setActor(actor);
            } else {
                properties.put(ConfigurationConstants.ACTOR, actor);
            }
        }
        
        boolean mustUnderstand = 
            MessageUtils.getContextualBoolean(msg, SecurityConstants.MUST_UNDERSTAND, true);
        if (properties != null) {
            properties.put(ConfigurationConstants.MUST_UNDERSTAND, Boolean.toString(mustUnderstand));
        }
        
        PasswordEncryptor passwordEncryptor = 
            (PasswordEncryptor)msg.getContextualProperty(SecurityConstants.PASSWORD_ENCRYPTOR_INSTANCE);
        if (passwordEncryptor != null && securityProperties == null) {
            properties.put(ConfigurationConstants.PASSWORD_ENCRYPTOR_INSTANCE, passwordEncryptor);
        }
    }
    
    private  Collection<Pattern> convertCertConstraints(String certConstraints) {
        String[] certConstraintsList = certConstraints.split(",");
        if (certConstraintsList != null) {
            Collection<Pattern> subjectCertConstraints = 
                new ArrayList<Pattern>(certConstraintsList.length);
            for (String certConstraint : certConstraintsList) {
                try {
                    subjectCertConstraints.add(Pattern.compile(certConstraint.trim()));
                } catch (PatternSyntaxException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            return subjectCertConstraints;
        }
        
        return null;
    }
    
    protected void configureCallbackHandler(SoapMessage soapMessage) throws WSSecurityException {
        Object o = soapMessage.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
        if (o instanceof String) {
            try {
                o = ClassLoaderUtils.loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
            }
        }            
        
        // If we have a "password" but no CallbackHandler then construct one
        if (o == null && getPassword(soapMessage) != null) {
            final String password = getPassword(soapMessage);
            o = new CallbackHandler() {

                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback callback : callbacks) {
                        if (callback instanceof WSPasswordCallback) {
                            WSPasswordCallback wsPasswordCallback = (WSPasswordCallback)callback;
                            wsPasswordCallback.setPassword(password);
                        }
                    }
                }
            };
        }
        
        if (o instanceof CallbackHandler) {
            Map<String, Object> config = getProperties();
            
            if (securityProperties != null) {
                securityProperties.setCallbackHandler((CallbackHandler)o);
            } else {
                config.put(ConfigurationConstants.PW_CALLBACK_REF, (CallbackHandler)o);
            }
        }
    }
    
    protected final TokenStore getTokenStore(Message message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore = 
                (TokenStore)message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            if (tokenStore == null) {
                TokenStoreFactory tokenStoreFactory = TokenStoreFactory.newInstance();
                String cacheKey = SecurityConstants.TOKEN_STORE_CACHE_INSTANCE;
                if (info.getName() != null) {
                    cacheKey += "-" + info.getName().toString().hashCode();
                }
                tokenStore = tokenStoreFactory.newTokenStore(cacheKey, message);
                info.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);
            }
            return tokenStore;
        }
    }

    public Set<URI> getRoles() {
        return null;
    }

    public void handleFault(SoapMessage message) {
    }

    public void postHandleMessage(SoapMessage message) throws Fault {
    }
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }
    
    public Object getOption(String key) {
        return properties.get(key);
    }
    
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getPassword(Object msgContext) {
        return (String)((Message)msgContext).getContextualProperty("password");
    }

    public Object getProperty(Object msgContext, String key) {
        Object obj = ((Message)msgContext).getContextualProperty(key);
        if (obj == null) {
            obj = getOption(key);
        }
        return obj;
    }

    public void setPassword(Object msgContext, String password) {
        ((Message)msgContext).put("password", password);
    }

    public void setProperty(Object msgContext, String key, Object value) {
        ((Message)msgContext).put(key, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }

    public Set<String> getAfter() {
        return after;
    }

    public void setAfter(Set<String> after) {
        this.after = after;
    }

    public Set<String> getBefore() {
        return before;
    }

    public void setBefore(Set<String> before) {
        this.before = before;
    }
    
    protected boolean isRequestor(SoapMessage message) {
        return MessageUtils.isRequestor(message);
    }

    public WSSSecurityProperties getSecurityProperties() {
        return securityProperties;
    }

    public void setSecurityProperties(WSSSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }  
    
    /**
     * Load a Crypto instance. Firstly, it tries to use the cryptoPropertyRefId tag to retrieve
     * a Crypto object via a custom reference Id. Failing this, it tries to load the crypto 
     * instance via the cryptoPropertyFile tag.
     */
    protected Crypto loadCrypto(
        SoapMessage soapMessage,
        String cryptoPropertyFile,
        String cryptoPropertyRefId
    ) throws WSSecurityException {
        Crypto crypto = null;
        
        //
        // Try the Property Ref Id first
        //
        String refId = (String)getProperty(soapMessage, cryptoPropertyRefId);
        if (refId != null) {
            crypto = cryptos.get(refId);
            if (crypto == null) {
                Object obj = getProperty(soapMessage, refId);
                if (obj instanceof Properties) {
                    crypto = CryptoFactory.getInstance((Properties)obj, 
                                                       getClassLoader(),
                                                       getPasswordEncryptor(soapMessage));
                    cryptos.put(refId, crypto);
                } else if (obj instanceof Crypto) {
                    crypto = (Crypto)obj;
                    cryptos.put(refId, crypto);
                }
            }
            if (crypto == null) {
                LOG.info("The Crypto reference " + refId + " specified by "
                    + cryptoPropertyRefId + " could not be loaded"
                );
            }
        }
        
        //
        // Now try loading the properties file
        //
        if (crypto == null) {
            String propFile = (String)getProperty(soapMessage, cryptoPropertyFile);
            if (propFile != null) {
                crypto = cryptos.get(propFile);
                if (crypto == null) {
                    crypto = loadCryptoFromPropertiesFile(soapMessage, propFile);
                    cryptos.put(propFile, crypto);
                }
                if (crypto == null) {
                    LOG.info(
                         "The Crypto properties file " + propFile + " specified by "
                         + cryptoPropertyFile + " could not be loaded or found"
                    );
                }
            } 
        }
        
        return crypto;
    }
    
    protected Crypto loadCryptoFromPropertiesFile(
        SoapMessage soapMessage, String propFilename
    ) throws WSSecurityException {
        ClassLoaderHolder orig = null;
        try {
            try {
                URL url = ClassLoaderUtils.getResource(propFilename, this.getClass());
                if (url == null) {
                    ResourceManager manager = soapMessage.getExchange()
                            .getBus().getExtension(ResourceManager.class);
                    ClassLoader loader = manager.resolveResource("", ClassLoader.class);
                    if (loader != null) {
                        orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                    }
                    url = manager.resolveResource(propFilename, URL.class);
                }
                if (url != null) {
                    Properties props = new Properties();
                    InputStream in = url.openStream(); 
                    props.load(in);
                    in.close();
                    return CryptoFactory.getInstance(props, getClassLoader(), 
                                                     getPasswordEncryptor(soapMessage));
                }
            } catch (Exception e) {
                //ignore
            } 
            return CryptoFactory.getInstance(propFilename, getClassLoader());
        } finally {
            if (orig != null) {
                orig.reset();
            }
        }
    }
    
    protected PasswordEncryptor getPasswordEncryptor(SoapMessage soapMessage) {
        PasswordEncryptor passwordEncryptor = 
            (PasswordEncryptor)soapMessage.getContextualProperty(
                SecurityConstants.PASSWORD_ENCRYPTOR_INSTANCE
            );
        if (passwordEncryptor != null) {
            return passwordEncryptor;
        }
        
        CallbackHandler callbackHandler = null;
        if (securityProperties != null) {
            callbackHandler = securityProperties.getCallbackHandler();
        } else {
            callbackHandler = (CallbackHandler)getProperties().get(ConfigurationConstants.PW_CALLBACK_REF);
        }
        if (callbackHandler != null) {
            return new JasyptPasswordEncryptor(callbackHandler);
        }
        
        return null;
    }
    
    private ClassLoader getClassLoader() {
        try {
            return Loader.getTCL();
        } catch (Exception ex) {
            return null;
        }
    }
}
