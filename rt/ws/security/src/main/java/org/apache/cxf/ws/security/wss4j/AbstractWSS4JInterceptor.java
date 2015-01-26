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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

public abstract class AbstractWSS4JInterceptor extends WSHandler implements SoapInterceptor, 
    PhaseInterceptor<SoapMessage> {

    private static final Set<QName> HEADERS = new HashSet<QName>();
    static {
        HEADERS.add(new QName(WSConstants.WSSE_NS, "Security"));
        HEADERS.add(new QName(WSConstants.WSSE11_NS, "Security"));
        HEADERS.add(new QName(WSConstants.ENC_NS, "EncryptedData"));
    }

    private Map<String, Object> properties = new ConcurrentHashMap<String, Object>();
    private final Set<String> before = new HashSet<String>();
    private final Set<String> after = new HashSet<String>();
    private String phase;
    private String id;
    
    public AbstractWSS4JInterceptor() {
        super();
        id = getClass().getName();
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

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
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
    
    protected void translateProperties(SoapMessage msg) {
        String bspCompliant = (String)msg.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
        if (bspCompliant != null) {
            msg.put(WSHandlerConstants.IS_BSP_COMPLIANT, bspCompliant);
        }
        String futureTTL = 
            (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_FUTURE_TTL);
        if (futureTTL != null) {
            msg.put(WSHandlerConstants.TTL_FUTURE_TIMESTAMP, futureTTL);
        }
        String ttl = 
                (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_TTL);
        if (ttl != null) {
            msg.put(WSHandlerConstants.TTL_TIMESTAMP, ttl);
        }
        
        String utFutureTTL = 
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_FUTURE_TTL);
        if (utFutureTTL != null) {
            msg.put(WSHandlerConstants.TTL_FUTURE_USERNAMETOKEN, utFutureTTL);
        }
        String utTTL = 
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_TTL);
        if (utTTL != null) {
            msg.put(WSHandlerConstants.TTL_USERNAMETOKEN, utTTL);
        }
        
        String certConstraints = 
            (String)msg.getContextualProperty(SecurityConstants.SUBJECT_CERT_CONSTRAINTS);
        if (certConstraints != null) {
            msg.put(WSHandlerConstants.SIG_SUBJECT_CERT_CONSTRAINTS, certConstraints);
        }
        
        // Now set SAML SenderVouches + Holder Of Key requirements
        boolean validateSAMLSubjectConf = 
            MessageUtils.getContextualBoolean(
                msg, SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, true
            );
        msg.put(
            WSHandlerConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, 
            Boolean.toString(validateSAMLSubjectConf)
        );
        
        PasswordEncryptor passwordEncryptor = 
            (PasswordEncryptor)msg.getContextualProperty(SecurityConstants.PASSWORD_ENCRYPTOR_INSTANCE);
        if (passwordEncryptor != null) {
            msg.put(ConfigurationConstants.PASSWORD_ENCRYPTOR_INSTANCE, passwordEncryptor);
        }
    }

    @Override
    protected Crypto loadCryptoFromPropertiesFile(
        String propFilename, 
        RequestData reqData
    ) throws WSSecurityException {
        ClassLoaderHolder orig = null;
        try {
            try {
                URL url = ClassLoaderUtils.getResource(propFilename, this.getClass());
                if (url == null) {
                    ResourceManager manager = ((Message)reqData.getMsgContext()).getExchange()
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
                    return CryptoFactory.getInstance(props,
                                                     this.getClassLoader(reqData.getMsgContext()),
                                                     getPasswordEncryptor(reqData));
                }
            } catch (Exception e) {
                //ignore
            } 
            return CryptoFactory.getInstance(propFilename, this.getClassLoader(reqData.getMsgContext()));
        } finally {
            if (orig != null) {
                orig.reset();
            }
        }
    }
    
}
