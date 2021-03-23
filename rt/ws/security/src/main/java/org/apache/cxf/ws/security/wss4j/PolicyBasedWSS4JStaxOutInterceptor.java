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

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
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

    public void handleMessage(SoapMessage msg) throws Fault {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        boolean enableStax =
            MessageUtils.getContextualBoolean(msg, SecurityConstants.ENABLE_STREAMING_SECURITY);
        if (aim != null && enableStax) {
            super.handleMessage(msg);
        }
    }

    @Override
    protected WSSSecurityProperties createSecurityProperties() {
        return new WSSSecurityProperties();
    }

    private void checkAsymmetricBinding(
        SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }

        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        final Crypto signCrypto;
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
        SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }

        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        final Crypto signCrypto;
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
        SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }

        Crypto encrCrypto = getEncryptionCrypto(e, message, securityProperties);
        final Crypto signCrypto;
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

        AssertionInfo asymAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (asymAis != null) {
            checkAsymmetricBinding(msg, securityProperties);
            asymAis.setAsserted(true);
        }

        AssertionInfo symAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (symAis != null) {
            checkSymmetricBinding(msg, securityProperties);
            symAis.setAsserted(true);
        }

        AssertionInfo transAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        if (transAis != null) {
            checkTransportBinding(msg, securityProperties);
            transAis.setAsserted(true);
        }

        super.configureProperties(msg, outboundSecurityContext, securityProperties);

        if (transAis != null) {
            TransportBinding binding = (TransportBinding)transAis.getAssertion();
            new StaxTransportBindingHandler(
                securityProperties, msg, binding, outboundSecurityContext).handleBinding();
        } else if (asymAis != null) {
            AsymmetricBinding binding = (AsymmetricBinding)asymAis.getAssertion();
            new StaxAsymmetricBindingHandler(
                securityProperties, msg, binding, outboundSecurityContext).handleBinding();
        } else if (symAis != null) {
            SymmetricBinding binding = (SymmetricBinding)symAis.getAssertion();
            new StaxSymmetricBindingHandler(
                securityProperties, msg, binding, outboundSecurityContext).handleBinding();
        } else {
            // Fall back to Transport Binding
            new StaxTransportBindingHandler(
                securityProperties, msg, null, outboundSecurityContext).handleBinding();
        }

    }

}
