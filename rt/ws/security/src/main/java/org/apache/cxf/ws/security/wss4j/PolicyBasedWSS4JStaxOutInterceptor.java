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
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.policyhandlers.StaxAsymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.StaxSymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.StaxTransportBindingHandler;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.apache.wss4j.policy.model.TransportBinding;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.stax.securityToken.OutboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenProvider;

/**
 * 
 */
public class PolicyBasedWSS4JStaxOutInterceptor extends WSS4JStaxOutInterceptor {
    public static final PolicyBasedWSS4JStaxOutInterceptor INSTANCE 
        = new PolicyBasedWSS4JStaxOutInterceptor();
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyBasedWSS4JStaxOutInterceptor.class);

    public PolicyBasedWSS4JStaxOutInterceptor() {
        super(new HashMap<String, Object>());
    }
    
    public void handleMessage(SoapMessage msg) throws Fault {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        boolean enableStax = 
            MessageUtils.isTrue(msg.getContextualProperty(SecurityConstants.ENABLE_STREAMING_SECURITY));
        if (aim != null && enableStax) {
            getProperties().clear();
            super.handleMessage(msg);
        }
    }
    
    private static Properties getProps(Object o, URL propsURL, SoapMessage message) {
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (propsURL != null) {
            try {
                properties = new Properties();
                InputStream ins = propsURL.openStream();
                properties.load(ins);
                ins.close();
            } catch (IOException e) {
                properties = null;
            }
        }
        
        return properties;
    }
    
    private URL getPropertiesFileURL(Object o, SoapMessage message) {
        if (o instanceof String) {
            URL url = null;
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, AbstractWSS4JInterceptor.class);
                }
                if (url == null) {
                    url = new URL((String)o);
                }
                return url;
            } catch (IOException e) {
                // Do nothing
            }
        } else if (o instanceof URL) {
            return (URL)o;        
        }
        return null;
    }
    
    private Collection<AssertionInfo> getAllAssertionsByLocalname(
        AssertionInfoMap aim,
        String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));
        
        if ((sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty())) {
            Collection<AssertionInfo> ais = new HashSet<AssertionInfo>();
            if (sp11Ais != null) {
                ais.addAll(sp11Ais);
            }
            if (sp12Ais != null) {
                ais.addAll(sp12Ais);
            }
            return ais;
        }
            
        return Collections.emptySet();
    }

    private void checkAsymmetricBinding(
        AssertionInfoMap aim, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_CRYPTO);
        if (s == null) {
            s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        }
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_CRYPTO);
        if (e == null) {
            e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, securityProperties);
        }
        
        if (signCrypto != null) {
            message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), signCrypto);
        }
        
        if (encrCrypto != null) {
            message.put(WSHandlerConstants.ENC_PROP_REF_ID, "RefId-" + encrCrypto.hashCode());
            message.put("RefId-" + encrCrypto.hashCode(), (Crypto)encrCrypto);
        } else if (signCrypto != null) {
            message.put(WSHandlerConstants.ENC_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), (Crypto)signCrypto);
        }
    }
    
    private void checkTransportBinding(
        AssertionInfoMap aim, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_CRYPTO);
        if (s == null) {
            s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        }
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_CRYPTO);
        if (e == null) {
            e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, securityProperties);
        }
        
        if (signCrypto != null) {
            message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), signCrypto);
        }
        
        if (encrCrypto != null) {
            message.put(WSHandlerConstants.ENC_PROP_REF_ID, "RefId-" + encrCrypto.hashCode());
            message.put("RefId-" + encrCrypto.hashCode(), (Crypto)encrCrypto);
        } else if (signCrypto != null) {
            message.put(WSHandlerConstants.ENC_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), (Crypto)signCrypto);
        }
    }
    
    private void checkSymmetricBinding(
        AssertionInfoMap aim, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_CRYPTO);
        if (s == null) {
            s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        }
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_CRYPTO);
        if (e == null) {
            e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, securityProperties);
        }
        
        if (isRequestor(message)) {
            Crypto crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.ENC_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
            
            crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
        } else {
            Crypto crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.ENC_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
            
            crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
        }
    }
    
    private Crypto getEncryptionCrypto(
        Object e, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Crypto encrCrypto = null;
        if (e instanceof Crypto) {
            encrCrypto = (Crypto)e;
        } else if (e != null) {
            URL propsURL = getPropertiesFileURL(e, message);
            Properties props = getProps(e, propsURL, message);
            if (props == null) {
                LOG.fine("Cannot find Crypto Encryption properties: " + e);
                Exception ex = new Exception("Cannot find Crypto Encryption properties: " + e);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
            }
            
            encrCrypto = CryptoFactory.getInstance(props,
                                                   Loader.getClassLoader(CryptoFactory.class),
                                                   getPasswordEncryptor(message, securityProperties));

            EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.ENCRYPT_CRYPTO, encrCrypto);
            }
        }
        return encrCrypto;
    }
    
    private Crypto getSignatureCrypto(
        Object s, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Crypto signCrypto = null;
        if (s instanceof Crypto) {
            signCrypto = (Crypto)s;
        } else if (s != null) {
            URL propsURL = getPropertiesFileURL(s, message);
            Properties props = getProps(s, propsURL, message);
            if (props == null) {
                LOG.fine("Cannot find Crypto Signature properties: " + s);
                Exception ex = new Exception("Cannot find Crypto Signature properties: " + s);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
            }
            
            signCrypto = CryptoFactory.getInstance(props,
                                                   Loader.getClassLoader(CryptoFactory.class),
                                                   getPasswordEncryptor(message, securityProperties));

            EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.SIGNATURE_CRYPTO, signCrypto);
            }
        }
        return signCrypto;
    }
    
    @Override
    protected void configureProperties(
        SoapMessage msg, Map<String, SecurityTokenProvider<OutboundSecurityToken>> outboundTokens,
        WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        
        Collection<AssertionInfo> asymAis = 
            getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (!asymAis.isEmpty()) {
            checkAsymmetricBinding(aim, msg, securityProperties);
        }
        
        Collection<AssertionInfo> symAis = 
            getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (!symAis.isEmpty()) {
            checkSymmetricBinding(aim, msg, securityProperties);
        }
        
        Collection<AssertionInfo> transAis = 
            getAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        if (!transAis.isEmpty()) {
            checkTransportBinding(aim, msg, securityProperties);
        }
        
        super.configureProperties(msg, outboundTokens, securityProperties);
        
        if (!transAis.isEmpty()) {
            TransportBinding binding = (TransportBinding)transAis.iterator().next().getAssertion();
            new StaxTransportBindingHandler(securityProperties, msg, binding, outboundTokens).handleBinding();
        } else if (!asymAis.isEmpty()) {
            AsymmetricBinding binding = (AsymmetricBinding)asymAis.iterator().next().getAssertion();
            new StaxAsymmetricBindingHandler(securityProperties, msg, binding, outboundTokens).handleBinding();
        } else if (!symAis.isEmpty()) {
            SymmetricBinding binding = (SymmetricBinding)symAis.iterator().next().getAssertion();
            new StaxSymmetricBindingHandler(securityProperties, msg, binding, outboundTokens).handleBinding();
        } else {
            // Fall back to Transport Binding
            new StaxTransportBindingHandler(securityProperties, msg, null, outboundTokens).handleBinding();
        }
        
    }
    
}
