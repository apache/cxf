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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.WSSPolicyException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.stax.OperationPolicy;
import org.apache.wss4j.policy.stax.PolicyEnforcer;
import org.apache.wss4j.policy.stax.PolicyInputProcessor;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.impl.securityToken.HttpsSecurityTokenImpl;
import org.apache.wss4j.stax.securityEvent.HttpsTokenSecurityEvent;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;

/**
 * 
 */
public class PolicyBasedWSS4JStaxInInterceptor extends WSS4JStaxInInterceptor {
    public static final PolicyBasedWSS4JStaxInInterceptor INSTANCE 
        = new PolicyBasedWSS4JStaxInInterceptor();
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyBasedWSS4JStaxInInterceptor.class);

    public void handleMessage(SoapMessage msg) throws Fault {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        boolean enableStax = 
            MessageUtils.isTrue(msg.getContextualProperty(SecurityConstants.ENABLE_STREAMING_SECURITY));
        if (aim != null && enableStax) {
            super.handleMessage(msg);
            msg.getInterceptorChain().add(new PolicyStaxActionInInterceptor());
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
        Collection<AssertionInfo> ais = 
            getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (ais.isEmpty()) {
            return;
        }
        
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
            message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), signCrypto);
        }
        
        if (encrCrypto != null) {
            message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, "RefId-" + encrCrypto.hashCode());
            message.put("RefId-" + encrCrypto.hashCode(), (Crypto)encrCrypto);
        } else if (signCrypto != null) {
            message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), (Crypto)signCrypto);
        }
    }
    
    private void checkTransportBinding(
        AssertionInfoMap aim, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws XMLSecurityException {
        boolean transportPolicyInEffect = 
            !getAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING).isEmpty();
        if (!transportPolicyInEffect && !(getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING).isEmpty()
            && getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING).isEmpty())) {
            return;
        }
        
        // Add a HttpsSecurityEvent so the policy verification code knows TLS is in use
        if (isRequestor(message)) {
            HttpsTokenSecurityEvent httpsTokenSecurityEvent = new HttpsTokenSecurityEvent();
            httpsTokenSecurityEvent.setAuthenticationType(
                HttpsTokenSecurityEvent.AuthenticationType.HttpsNoAuthentication
            );
            HttpsSecurityTokenImpl httpsSecurityToken = new HttpsSecurityTokenImpl();
            try {
                httpsSecurityToken.addTokenUsage(WSSecurityTokenConstants.TokenUsage_MainSignature);
            } catch (XMLSecurityException e) {
                LOG.fine(e.getMessage());
            }
            httpsTokenSecurityEvent.setSecurityToken(httpsSecurityToken);

            List<SecurityEvent> securityEvents = getSecurityEventList(message);
            securityEvents.add(httpsTokenSecurityEvent);
        }
        
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
            message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), signCrypto);
        }

        if (encrCrypto != null) {
            message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, "RefId-" + encrCrypto.hashCode());
            message.put("RefId-" + encrCrypto.hashCode(), (Crypto)encrCrypto);
        } else if (signCrypto != null) {
            message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), (Crypto)signCrypto);
        }
    }

    private List<SecurityEvent> getSecurityEventList(Message message) {
        @SuppressWarnings("unchecked")
        List<SecurityEvent> securityEvents = 
            (List<SecurityEvent>) message.getExchange().get(SecurityEvent.class.getName() + ".out");
        if (securityEvents == null) {
            securityEvents = new ArrayList<SecurityEvent>();
            message.getExchange().put(SecurityEvent.class.getName() + ".out", securityEvents);
        }
        
        return securityEvents;
    }
    
    private void checkSymmetricBinding(
        AssertionInfoMap aim, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Collection<AssertionInfo> ais = 
            getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (ais.isEmpty()) {
            return;
        }
        
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
                message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
            
            crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
        } else {
            Crypto crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
            
            crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + crypto.hashCode());
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
        SoapMessage msg, WSSSecurityProperties securityProperties
    ) throws XMLSecurityException {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        checkAsymmetricBinding(aim, msg, securityProperties);
        checkSymmetricBinding(aim, msg, securityProperties);
        checkTransportBinding(aim, msg, securityProperties);
        
        // Allow for setting non-standard asymmetric signature algorithms
        String asymSignatureAlgorithm = 
            (String)msg.getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
        if (asymSignatureAlgorithm != null) {
            Collection<AssertionInfo> algorithmSuites = 
                aim.get(SP12Constants.ALGORITHM_SUITE);
            if (algorithmSuites != null && !algorithmSuites.isEmpty()) {
                for (AssertionInfo algorithmSuite : algorithmSuites) {
                    AlgorithmSuite algSuite = (AlgorithmSuite)algorithmSuite.getAssertion();
                    algSuite.setAsymmetricSignature(asymSignatureAlgorithm);
                }
            }
        }
        
        super.configureProperties(msg, securityProperties);
    }
    
    /**
     * Is a Nonce Cache required, i.e. are we expecting a UsernameToken 
     */
    @Override
    protected boolean isNonceCacheRequired(SoapMessage msg, WSSSecurityProperties securityProperties) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            Collection<AssertionInfo> ais = 
                getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);
            
            if (!ais.isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Is a Timestamp cache required, i.e. are we expecting a Timestamp 
     */
    @Override
    protected boolean isTimestampCacheRequired(SoapMessage msg, WSSSecurityProperties securityProperties) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            Collection<AssertionInfo> ais = 
                getAllAssertionsByLocalname(aim, SPConstants.INCLUDE_TIMESTAMP);
            
            if (!ais.isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Is a SAML Cache required, i.e. are we expecting a SAML Token 
     */
    @Override
    protected boolean isSamlCacheRequired(SoapMessage msg, WSSSecurityProperties securityProperties) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            Collection<AssertionInfo> ais = 
                getAllAssertionsByLocalname(aim, SPConstants.SAML_TOKEN);
            
            if (!ais.isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    protected List<SecurityEventListener> configureSecurityEventListeners(
        SoapMessage msg, WSSSecurityProperties securityProperties
    ) throws WSSPolicyException {
        List<SecurityEventListener> securityEventListeners = new ArrayList<SecurityEventListener>(2);
        securityEventListeners.addAll(super.configureSecurityEventListeners(msg, securityProperties));
        
        Endpoint endoint = msg.getExchange().get(Endpoint.class);
        
        PolicyEnforcer policyEnforcer = createPolicyEnforcer(endoint.getEndpointInfo(), msg);
        securityProperties.addInputProcessor(new PolicyInputProcessor(policyEnforcer, securityProperties));
        securityEventListeners.add(policyEnforcer);
        
        return securityEventListeners;
    }
    
    private PolicyEnforcer createPolicyEnforcer(
        EndpointInfo endpointInfo, SoapMessage msg
    ) throws WSSPolicyException {

        List<OperationPolicy> operationPolicies = new ArrayList<OperationPolicy>();
        Collection<BindingOperationInfo> bindingOperationInfos = endpointInfo.getBinding().getOperations();
        for (Iterator<BindingOperationInfo> bindingOperationInfoIterator =
                     bindingOperationInfos.iterator(); bindingOperationInfoIterator.hasNext();) {
            BindingOperationInfo bindingOperationInfo = bindingOperationInfoIterator.next();
            QName operationName = bindingOperationInfo.getName();
            
            // todo: I'm not sure what the effectivePolicy exactly contains,
            // a) only the operation policy,
            // or b) all policies for the service,
            // or c) all policies which applies for the current operation.
            // c) is that what we need for stax.
            EffectivePolicy policy = 
                (EffectivePolicy)bindingOperationInfo.getProperty("policy-engine-info-serve-request");
            //PolicyEngineImpl.POLICY_INFO_REQUEST_SERVER);
            String localName = operationName.getLocalPart();
            if (MessageUtils.isRequestor(msg)) {
                policy = 
                    (EffectivePolicy)bindingOperationInfo.getProperty("policy-engine-info-client-response");
                MessageInfo messageInfo = bindingOperationInfo.getOutput().getMessageInfo();
                localName = messageInfo.getName().getLocalPart();
                if (!messageInfo.getMessageParts().isEmpty()) {
                    localName = messageInfo.getMessagePart(0).getConcreteName().getLocalPart();
                }
            }
            SoapOperationInfo soapOperationInfo = bindingOperationInfo.getExtensor(SoapOperationInfo.class);
            if (policy != null && soapOperationInfo != null) {
                String soapNS;
                BindingInfo bindingInfo = bindingOperationInfo.getBinding();
                if (bindingInfo instanceof SoapBindingInfo) {
                    soapNS = ((SoapBindingInfo)bindingInfo).getSoapVersion().getNamespace();
                } else {
                    //no idea what todo here...
                    //most probably throw an exception:
                    throw new IllegalArgumentException("BindingInfo is not an instance of SoapBindingInfo");
                }
                
                //todo: I think its a bug that we handover only the localPart of the operation. 
                // Needs to be fixed in ws-security-policy-stax
                OperationPolicy operationPolicy = new OperationPolicy(localName);
                operationPolicy.setPolicy(policy.getPolicy());
                operationPolicy.setOperationAction(soapOperationInfo.getAction());
                operationPolicy.setSoapMessageVersionNamespace(soapNS);
                
                operationPolicies.add(operationPolicy);
            }
        }
        
        String soapAction = SoapActionInInterceptor.getSoapAction(msg);
        if (soapAction == null) {
            soapAction = "";
        }
        
        String actor = (String)msg.getContextualProperty(SecurityConstants.ACTOR);
        final Collection<org.apache.cxf.message.Attachment> attachments = 
            msg.getAttachments();
        int attachmentCount = 0;
        if (attachments != null && !attachments.isEmpty()) {
            attachmentCount = attachments.size();
        }
        return new PolicyEnforcer(operationPolicies, soapAction, isRequestor(msg), actor, attachmentCount);
    }
    
}
