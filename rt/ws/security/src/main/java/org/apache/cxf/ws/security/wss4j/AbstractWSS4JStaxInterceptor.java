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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.JasyptPasswordEncryptor;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.stax.ConfigurationConverter;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;

public abstract class AbstractWSS4JStaxInterceptor implements SoapInterceptor, 
    PhaseInterceptor<SoapMessage> {

    private static final Set<QName> HEADERS = new HashSet<QName>();
    static {
<<<<<<< HEAD
        HEADERS.add(new QName(WSSConstants.NS_WSSE10, "Security"));
        HEADERS.add(new QName(WSSConstants.NS_WSSE11, "Security"));
        HEADERS.add(new QName(WSSConstants.NS_XMLENC, "EncryptedData"));
=======
        HEADERS.add(new QName(WSConstants.WSSE_NS, "Security"));
        HEADERS.add(new QName(WSConstants.ENC_NS, "EncryptedData"));
        HEADERS.add(new QName(WSConstants.WSSE11_NS, "EncryptedHeader"));
>>>>>>> 5b20a3c... [CXF-6343] - EncryptedHeader not properly processed or generated. This closes #66
    }
    
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractWSS4JStaxInterceptor.class);

    private final Map<String, Object> properties;
    private final WSSSecurityProperties userSecurityProperties;
    private Map<String, Crypto> cryptos = new ConcurrentHashMap<String, Crypto>();
    private final Set<String> before = new HashSet<String>();
    private final Set<String> after = new HashSet<String>();
    private String phase;
    private String id;
    
    public AbstractWSS4JStaxInterceptor(WSSSecurityProperties securityProperties) {
        super();
        id = getClass().getName();
        userSecurityProperties = securityProperties;
        properties = null;
    }
    
    public AbstractWSS4JStaxInterceptor(Map<String, Object> properties) {
        super();
        id = getClass().getName();
        this.properties = properties;
        userSecurityProperties = null;
    }
    
    public AbstractWSS4JStaxInterceptor() {
        super();
        id = getClass().getName();
        userSecurityProperties = null;
        properties = null;
    }

    protected WSSSecurityProperties createSecurityProperties() {
        if (userSecurityProperties != null) {
            return new WSSSecurityProperties(userSecurityProperties);
        } else {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            ConfigurationConverter.parseActions(properties, securityProperties);
            ConfigurationConverter.parseUserProperties(properties, securityProperties);
            ConfigurationConverter.parseCallback(properties, securityProperties);
            ConfigurationConverter.parseBooleanProperties(properties, securityProperties);
            ConfigurationConverter.parseNonBooleanProperties(properties, securityProperties);
            return securityProperties;
        }
    }
    
    protected void translateProperties(SoapMessage msg, WSSSecurityProperties securityProperties) {
        String bspCompliant = (String)msg.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
        if (bspCompliant != null) {
            securityProperties.setDisableBSPEnforcement(!Boolean.valueOf(bspCompliant));
        }
        
        String futureTTL = 
            (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_FUTURE_TTL);
        if (futureTTL != null) {
            securityProperties.setTimeStampFutureTTL(Integer.parseInt(futureTTL));
        }
        
        String ttl = 
            (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_TTL);
        if (ttl != null) {
            securityProperties.setTimestampTTL(Integer.parseInt(ttl));
        }
        
        String utFutureTTL = 
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_FUTURE_TTL);
        if (utFutureTTL != null) {
            securityProperties.setUtFutureTTL(Integer.parseInt(utFutureTTL));
        }
        
        String utTTL = 
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_TTL);
        if (utTTL != null) {
            securityProperties.setUtTTL(Integer.parseInt(utTTL));
        }
        
        String certConstraints = 
            (String)msg.getContextualProperty(SecurityConstants.SUBJECT_CERT_CONSTRAINTS);
        if (certConstraints != null && !"".equals(certConstraints)) {
            securityProperties.setSubjectCertConstraints(convertCertConstraints(certConstraints));
        }
        
        // Now set SAML SenderVouches + Holder Of Key requirements
        String validateSAMLSubjectConf = 
            (String)msg.getContextualProperty(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION);
        if (validateSAMLSubjectConf != null) {
            securityProperties.setValidateSamlSubjectConfirmation(Boolean.valueOf(validateSAMLSubjectConf));
        }
        
        String actor = (String)msg.getContextualProperty(SecurityConstants.ACTOR);
        if (actor != null) {
            securityProperties.setActor(actor);
        }
        
        boolean mustUnderstand = 
            MessageUtils.getContextualBoolean(msg, SecurityConstants.MUST_UNDERSTAND, true);
        securityProperties.setMustUnderstand(mustUnderstand);
        
        boolean validateSchemas = 
            MessageUtils.getContextualBoolean(msg, "schema-validation-enabled", false);
        securityProperties.setDisableSchemaValidation(!validateSchemas);
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
    
    protected void configureCallbackHandler(
        SoapMessage soapMessage, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Object o = soapMessage.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
        if (o instanceof String) {
            try {
                o = ClassLoaderUtils.loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
            }
            
            if (o instanceof CallbackHandler) {
                EndpointInfo info = soapMessage.getExchange().get(Endpoint.class).getEndpointInfo();
                synchronized (info) {
                    info.setProperty(SecurityConstants.CALLBACK_HANDLER, o);
                }
                soapMessage.getExchange().get(Endpoint.class).put(SecurityConstants.CALLBACK_HANDLER, o);
                soapMessage.getExchange().put(SecurityConstants.CALLBACK_HANDLER, o);
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
            securityProperties.setCallbackHandler((CallbackHandler)o);
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
        if (properties != null) {
            return properties.get(key);
        }
        return null;
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
        if (properties != null) {
            return properties;
        }
        return Collections.emptyMap();
    }

    public Set<String> getAfter() {
        return after;
    }

    public Set<String> getBefore() {
        return before;
    }

    protected boolean isRequestor(SoapMessage message) {
        return MessageUtils.isRequestor(message);
    }

    /**
     * Load a Crypto instance. Firstly, it tries to use the cryptoPropertyRefId tag to retrieve
     * a Crypto object via a custom reference Id. Failing this, it tries to load the crypto 
     * instance via the cryptoPropertyFile tag.
     */
    protected Crypto loadCrypto(
        SoapMessage soapMessage,
        String cryptoPropertyFile,
        String cryptoPropertyRefId,
        WSSSecurityProperties securityProperties
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
                                                       getPasswordEncryptor(soapMessage, securityProperties));
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
                    crypto = loadCryptoFromPropertiesFile(soapMessage, propFile, securityProperties);
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
        SoapMessage soapMessage, String propFilename, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        PasswordEncryptor passwordEncryptor = getPasswordEncryptor(soapMessage, securityProperties);
        return 
            WSS4JUtils.loadCryptoFromPropertiesFile(
                soapMessage, propFilename, this.getClass(), getClassLoader(), passwordEncryptor
        );
    }
    
    protected PasswordEncryptor getPasswordEncryptor(
        SoapMessage soapMessage, WSSSecurityProperties securityProperties
    ) {
        PasswordEncryptor passwordEncryptor = 
            (PasswordEncryptor)soapMessage.getContextualProperty(
                SecurityConstants.PASSWORD_ENCRYPTOR_INSTANCE
            );
        if (passwordEncryptor != null) {
            return passwordEncryptor;
        }

        CallbackHandler callbackHandler = securityProperties.getCallbackHandler();
        if (callbackHandler == null) {
            callbackHandler = (CallbackHandler)getProperties().get(ConfigurationConstants.PW_CALLBACK_REF);
        }

        if (callbackHandler != null) {
            return new JasyptPasswordEncryptor(callbackHandler);
        }
        
        return null;
    }
    
    protected AssertionInfo getFirstAssertionByLocalname(
        AssertionInfoMap aim, String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        if (sp11Ais != null && !sp11Ais.isEmpty()) {
            return sp11Ais.iterator().next();
        }
        
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));
        if (sp12Ais != null && !sp12Ais.isEmpty()) {
            return sp12Ais.iterator().next();
        }

        return null;
    }
    
    protected Crypto getEncryptionCrypto(
            Object e, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        if (e == null) {
            return null;
        } else if (e instanceof Crypto) {
            return (Crypto)e;
        } else {
            ResourceManager manager = 
                message.getExchange().getBus().getExtension(ResourceManager.class);
            URL propsURL = WSS4JUtils.getPropertiesFileURL(e, manager, this.getClass());
            Properties props = WSS4JUtils.getProps(e, propsURL);
            if (props == null) {
                LOG.fine("Cannot find Crypto Encryption properties: " + e);
                Exception ex = new Exception("Cannot find Crypto Encryption properties: " + e);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
            }

            Crypto encrCrypto = CryptoFactory.getInstance(props,
                    Loader.getClassLoader(CryptoFactory.class),
                    getPasswordEncryptor(message, securityProperties));

            EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.ENCRYPT_CRYPTO, encrCrypto);
            }
            return encrCrypto;
        }
    }
        
    protected Crypto getSignatureCrypto(
        Object s, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        if (s == null) {
            return null;
        } else if (s instanceof Crypto) {
            return (Crypto)s;
        } else {
            ResourceManager manager = 
                message.getExchange().getBus().getExtension(ResourceManager.class);
            URL propsURL = WSS4JUtils.getPropertiesFileURL(s, manager, this.getClass());
            Properties props = WSS4JUtils.getProps(s, propsURL);
            if (props == null) {
                LOG.fine("Cannot find Crypto Signature properties: " + s);
                Exception ex = new Exception("Cannot find Crypto Signature properties: " + s);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
            }

            Crypto signCrypto = CryptoFactory.getInstance(props,
                    Loader.getClassLoader(CryptoFactory.class),
                    getPasswordEncryptor(message, securityProperties));

            EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.SIGNATURE_CRYPTO, signCrypto);
            }
            return signCrypto;
        }
    }

    private ClassLoader getClassLoader() {
        try {
            return Loader.getTCL();
        } catch (Exception ex) {
            return null;
        }
    }
}
