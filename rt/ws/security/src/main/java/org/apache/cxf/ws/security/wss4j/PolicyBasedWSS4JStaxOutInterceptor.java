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

import java.util.Collection;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.policyhandlers.StaxAsymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.StaxSymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.StaxTransportBindingHandler;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.apache.wss4j.policy.model.TransportBinding;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.stax.ext.OutboundSecurityContext;

/**
 * 
 */
public class PolicyBasedWSS4JStaxOutInterceptor extends WSS4JStaxOutInterceptor {
    public static final PolicyBasedWSS4JStaxOutInterceptor INSTANCE 
        = new PolicyBasedWSS4JStaxOutInterceptor();

    public void handleMessage(SoapMessage msg) throws Fault {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        boolean enableStax = 
            MessageUtils.isTrue(msg.getContextualProperty(SecurityConstants.ENABLE_STREAMING_SECURITY));
        if (aim != null && enableStax) {
            getProperties().clear();
            super.handleMessage(msg);
        }
    }
    
    @Override
    protected WSSSecurityProperties createSecurityProperties() {
        return new WSSSecurityProperties();
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
            securityProperties.setSignatureCrypto(signCrypto);
        }
        
        if (encrCrypto != null) {
            securityProperties.setEncryptionCrypto(encrCrypto);
        } else if (signCrypto != null) {
            securityProperties.setEncryptionCrypto(signCrypto);
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
            securityProperties.setSignatureCrypto(signCrypto);
        }
        
        if (encrCrypto != null) {
            securityProperties.setEncryptionCrypto(encrCrypto);
        } else if (signCrypto != null) {
            securityProperties.setEncryptionCrypto(signCrypto);
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
                securityProperties.setEncryptionCrypto(crypto);
            }
            
            crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                securityProperties.setSignatureCrypto(crypto);
            }
        } else {
            Crypto crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                securityProperties.setEncryptionCrypto(crypto);
            }
            
            crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                securityProperties.setSignatureCrypto(crypto);
            }
        }
    }
    
    @Override
    protected void configureProperties(
        SoapMessage msg, OutboundSecurityContext outboundSecurityContext,
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
        
        super.configureProperties(msg, outboundSecurityContext, securityProperties);
        
        if (!transAis.isEmpty()) {
            TransportBinding binding = (TransportBinding)transAis.iterator().next().getAssertion();
            new StaxTransportBindingHandler(
                securityProperties, msg, binding, outboundSecurityContext).handleBinding();
        } else if (!asymAis.isEmpty()) {
            AsymmetricBinding binding = (AsymmetricBinding)asymAis.iterator().next().getAssertion();
            new StaxAsymmetricBindingHandler(
                securityProperties, msg, binding, outboundSecurityContext).handleBinding();
        } else if (!symAis.isEmpty()) {
            SymmetricBinding binding = (SymmetricBinding)symAis.iterator().next().getAssertion();
            new StaxSymmetricBindingHandler(
                securityProperties, msg, binding, outboundSecurityContext).handleBinding();
        } else {
            // Fall back to Transport Binding
            new StaxTransportBindingHandler(
                securityProperties, msg, null, outboundSecurityContext).handleBinding();
        }
        
    }
    
}
