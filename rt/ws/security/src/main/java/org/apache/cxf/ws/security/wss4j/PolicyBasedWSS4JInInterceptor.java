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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.wss4j.policyvalidators.PolicyValidatorParameters;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecurityPolicyValidator;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.token.Timestamp;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.UsernameToken.PasswordType;
import org.apache.wss4j.policy.model.Wss11;

/**
 * 
 */
public class PolicyBasedWSS4JInInterceptor extends WSS4JInInterceptor {
    /**
     * 
     */
    public PolicyBasedWSS4JInInterceptor() {
        super(true);
    }
    
    public void handleMessage(SoapMessage msg) throws Fault {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        boolean enableStax = 
            MessageUtils.isTrue(msg.getContextualProperty(SecurityConstants.ENABLE_STREAMING_SECURITY));
        if (aim != null && !enableStax) {
            super.handleMessage(msg);
        }
    }
    
    private void handleWSS11(AssertionInfoMap aim, SoapMessage message) {
        if (isRequestor(message)) {
            message.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, "false");
            Collection<AssertionInfo> ais = 
                PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.WSS11);
            if (!ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    Wss11 wss11 = (Wss11)ai.getAssertion();
                    if (wss11.isRequireSignatureConfirmation()) {
                        message.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
                        break;
                    }
                }
            }
        }
    }

    private String addToAction(String action, String val, boolean pre) {
        if (action.contains(val)) {
            return action;
        }
        if (pre) {
            return val + " " + action; 
        } 
        return action + " " + val;
    }
    
    private String checkAsymmetricBinding(
        AssertionInfoMap aim, String action, SoapMessage message, RequestData data
    ) throws WSSecurityException {
        AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (ai == null) {
            return action;
        }
        
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message, data);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, data);
        }
        
        final String signCryptoRefId = signCrypto != null ? "RefId-" + signCrypto.hashCode() : null;
        
        if (signCrypto != null) {
            message.put(WSHandlerConstants.DEC_PROP_REF_ID, signCryptoRefId);
            message.put(signCryptoRefId, signCrypto);
        }
        
        if (encrCrypto != null) {
            final String encCryptoRefId = "RefId-" + encrCrypto.hashCode();
            message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, encCryptoRefId);
            message.put(encCryptoRefId, (Crypto)encrCrypto);
        } else if (signCrypto != null) {
            message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, signCryptoRefId);
            message.put(signCryptoRefId, (Crypto)signCrypto);
        }
     
        return action;
    }
    
    private String checkDefaultBinding(
        AssertionInfoMap aim, String action, SoapMessage message, RequestData data
    ) throws WSSecurityException {
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message, data);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, data);
        }
        
        final String signCryptoRefId = signCrypto != null ? "RefId-" + signCrypto.hashCode() : null;
        if (signCrypto != null) {
            message.put(WSHandlerConstants.DEC_PROP_REF_ID, signCryptoRefId);
            message.put(signCryptoRefId, signCrypto);
        }
        
        if (encrCrypto != null) {
            final String encCryptoRefId = "RefId-" + encrCrypto.hashCode();
            message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, encCryptoRefId);
            message.put(encCryptoRefId, (Crypto)encrCrypto);
        } else if (signCrypto != null) {
            message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, signCryptoRefId);
            message.put(signCryptoRefId, (Crypto)signCrypto);
        }

        return action;
    }
    
    /**
     * Is a Nonce Cache required, i.e. are we expecting a UsernameToken
     */
    @Override
    protected boolean isNonceCacheRequired(List<Integer> actions, SoapMessage msg) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.USERNAME_TOKEN);
            if (ai != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Is a Timestamp cache required, i.e. are we expecting a Timestamp 
     */
    @Override
    protected boolean isTimestampCacheRequired(List<Integer> actions, SoapMessage msg) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.INCLUDE_TIMESTAMP);
            if (ai != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Is a SAML Cache required, i.e. are we expecting a SAML Token 
     */
    @Override
    protected boolean isSamlCacheRequired(List<Integer> actions, SoapMessage msg) {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        if (aim != null) {
            AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SAML_TOKEN);
            if (ai != null) {
                return true;
            }
        }
        
        return false;
    }
    
    private void checkUsernameToken(
        AssertionInfoMap aim, SoapMessage message
    ) throws WSSecurityException {
        Collection<AssertionInfo> ais = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);
        
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                UsernameToken policy = (UsernameToken)ai.getAssertion();
                if (policy.getPasswordType() == PasswordType.NoPassword) {
                    message.put(WSHandlerConstants.ALLOW_USERNAMETOKEN_NOPASSWORD, "true");
                }
            }
        }
    }
    
    private String checkSymmetricBinding(
        AssertionInfoMap aim, String action, SoapMessage message, RequestData data
    ) throws WSSecurityException {
        AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (ai == null) {
            return action;
        }
        
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (s == null) {
            s = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PROPERTIES, message);
        }
        Object e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, message);
        if (e == null) {
            e = SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_PROPERTIES, message);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message, data);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message, data);
        }
        
        if (isRequestor(message)) {
            Crypto crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                final String refId = "RefId-" + crypto.hashCode();
                message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, refId);
                message.put(refId, crypto);
            }
            
            crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                final String refId = "RefId-" + crypto.hashCode();
                message.put(WSHandlerConstants.DEC_PROP_REF_ID, refId);
                message.put(refId, crypto);
            }
        } else {
            Crypto crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                final String refId = "RefId-" + crypto.hashCode();
                message.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, refId);
                message.put(refId, crypto);
            }
            
            crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                final String refId = "RefId-" + crypto.hashCode();
                message.put(WSHandlerConstants.DEC_PROP_REF_ID, refId);
                message.put(refId, crypto);
            }
        }
        
        return action;
    }
    
    private Crypto getEncryptionCrypto(Object e, 
                                       SoapMessage message, 
                                       RequestData requestData) throws WSSecurityException {
        PasswordEncryptor passwordEncryptor = getPasswordEncryptor(message, requestData);
        return WSS4JUtils.getEncryptionCrypto(e, message, passwordEncryptor);
    }
    
    private PasswordEncryptor getPasswordEncryptor(SoapMessage soapMessage, RequestData requestData) {
        PasswordEncryptor passwordEncryptor = 
            (PasswordEncryptor)soapMessage.getContextualProperty(
                SecurityConstants.PASSWORD_ENCRYPTOR_INSTANCE
            );
        if (passwordEncryptor != null) {
            return passwordEncryptor;
        }
        
        return super.getPasswordEncryptor(requestData);
    }
    
    private Crypto getSignatureCrypto(Object s, SoapMessage message, 
                                      RequestData requestData) throws WSSecurityException {
        PasswordEncryptor passwordEncryptor = getPasswordEncryptor(message, requestData);
        return WSS4JUtils.getSignatureCrypto(s, message, passwordEncryptor);
    }
    
    /**
     * Set a WSS4J AlgorithmSuite object on the RequestData context, to restrict the
     * algorithms that are allowed for encryption, signature, etc.
     */
    protected void setAlgorithmSuites(SoapMessage message, RequestData data) throws WSSecurityException {
        AlgorithmSuiteTranslater translater = new AlgorithmSuiteTranslater();
        translater.translateAlgorithmSuites(message.get(AssertionInfoMap.class), data);
        
        // Allow for setting non-standard asymmetric signature algorithms
        String asymSignatureAlgorithm = 
            (String)message.getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
        if (asymSignatureAlgorithm != null && data.getAlgorithmSuite() != null) {
            data.getAlgorithmSuite().getSignatureMethods().clear();
            data.getAlgorithmSuite().getSignatureMethods().add(asymSignatureAlgorithm);
        }
    }

    protected void computeAction(SoapMessage message, RequestData data) throws WSSecurityException {
        String action = getString(WSHandlerConstants.ACTION, message);
        if (action == null) {
            action = "";
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (aim != null) {
            //things that DO impact setup
            handleWSS11(aim, message);
            action = checkAsymmetricBinding(aim, action, message, data);
            action = checkSymmetricBinding(aim, action, message, data);
            Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
            if ("".equals(action) || (ais != null && !ais.isEmpty())) {
                action = checkDefaultBinding(aim, action, message, data);
            }
            
            // Allow for setting non-standard asymmetric signature algorithms
            String asymSignatureAlgorithm = 
                (String)message.getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
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
            
            checkUsernameToken(aim, message);
            
            // stuff we can default to asserted and un-assert if a condition isn't met
            PolicyUtils.assertPolicy(aim, SPConstants.KEY_VALUE_TOKEN);
            PolicyUtils.assertPolicy(aim, SPConstants.RSA_KEY_VALUE);
            
            // WSS10
            ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.WSS10);
            if (!ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }
                PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER);
                PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL);
                PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI);
                PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN);
            }
            
            // Trust 1.0
            ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.TRUST_10);
            boolean trust10Asserted = false;
            if (!ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }
                PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_CLIENT_CHALLENGE);
                PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_SERVER_CHALLENGE);
                PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_CLIENT_ENTROPY);
                PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_SERVER_ENTROPY);
                PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_ISSUED_TOKENS);
                trust10Asserted = true;
            }
            
            // Trust 1.3
            ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.TRUST_13);
            if (!ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }
                PolicyUtils.assertPolicy(aim, SP12Constants.REQUIRE_REQUEST_SECURITY_TOKEN_COLLECTION);
                PolicyUtils.assertPolicy(aim, SP12Constants.REQUIRE_APPLIES_TO);
                PolicyUtils.assertPolicy(aim, SP13Constants.SCOPE_POLICY_15);
                PolicyUtils.assertPolicy(aim, SP13Constants.MUST_SUPPORT_INTERACTIVE_CHALLENGE);
                
                if (!trust10Asserted) {
                    PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_CLIENT_CHALLENGE);
                    PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_SERVER_CHALLENGE);
                    PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_CLIENT_ENTROPY);
                    PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_SERVER_ENTROPY);
                    PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_ISSUED_TOKENS);
                }
            }
            
            message.put(WSHandlerConstants.ACTION, action.trim());
        }
    }
    
    @Override
    protected void doResults(
        SoapMessage msg, 
        String actor,
        Element soapHeader,
        Element soapBody,
        WSHandlerResult results, 
        boolean utWithCallbacks
    ) throws SOAPException, XMLStreamException, WSSecurityException {
        //
        // Pre-fetch various results
        //
        List<WSSecurityEngineResult> signedResults = new ArrayList<>();
        if (results.getActionResults().containsKey(WSConstants.SIGN)) {
            signedResults.addAll(results.getActionResults().get(WSConstants.SIGN));
        }
        if (results.getActionResults().containsKey(WSConstants.UT_SIGN)) {
            signedResults.addAll(results.getActionResults().get(WSConstants.UT_SIGN));
        }
        if (results.getActionResults().containsKey(WSConstants.ST_SIGNED)) {
            signedResults.addAll(results.getActionResults().get(WSConstants.ST_SIGNED));
        }
        Collection<WSDataRef> signed = new HashSet<>();
        for (WSSecurityEngineResult result : signedResults) {
            List<WSDataRef> sl = 
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            if (sl != null) {
                for (WSDataRef r : sl) {
                    signed.add(r);
                }
            }
        }
        
        List<WSSecurityEngineResult> encryptResults = results.getActionResults().get(WSConstants.ENCR);
        Collection<WSDataRef> encrypted = new HashSet<>();
        if (encryptResults != null) {
            for (WSSecurityEngineResult result : encryptResults) {
                List<WSDataRef> sl = 
                    CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                if (sl != null) {
                    for (WSDataRef r : sl) {
                        encrypted.add(r);
                    }
                }
            }
        }
        
        CryptoCoverageUtil.reconcileEncryptedSignedRefs(signed, encrypted);
        
        //
        // Check policies
        //
        PolicyValidatorParameters parameters = new PolicyValidatorParameters();
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        parameters.setAssertionInfoMap(aim);
        parameters.setMessage(msg);
        parameters.setSoapBody(soapBody);
        parameters.setSoapHeader(soapHeader);
        parameters.setResults(results);
        parameters.setSignedResults(signedResults);
        parameters.setEncryptedResults(encryptResults);
        parameters.setUtWithCallbacks(utWithCallbacks);
        parameters.setSigned(signed);
        parameters.setEncrypted(encrypted);
        
        List<WSSecurityEngineResult> utResults = new ArrayList<>();
        if (results.getActionResults().containsKey(WSConstants.UT)) {
            utResults.addAll(results.getActionResults().get(WSConstants.UT));
        }
        if (results.getActionResults().containsKey(WSConstants.UT_NOPASSWORD)) {
            utResults.addAll(results.getActionResults().get(WSConstants.UT_NOPASSWORD));
        }
        parameters.setUsernameTokenResults(utResults);
        
        List<WSSecurityEngineResult> samlResults = new ArrayList<>();
        if (results.getActionResults().containsKey(WSConstants.ST_SIGNED)) {
            samlResults.addAll(results.getActionResults().get(WSConstants.ST_SIGNED));
        }
        if (results.getActionResults().containsKey(WSConstants.ST_UNSIGNED)) {
            samlResults.addAll(results.getActionResults().get(WSConstants.ST_UNSIGNED));
        }
        parameters.setSamlResults(samlResults);
        
        // Store the timestamp element
        WSSecurityEngineResult tsResult = null;
        if (results.getActionResults().containsKey(WSConstants.TS)) {
            tsResult = results.getActionResults().get(WSConstants.TS).get(0);
        }
        Element timestamp = null;
        if (tsResult != null) {
            Timestamp ts = (Timestamp)tsResult.get(WSSecurityEngineResult.TAG_TIMESTAMP);
            timestamp = ts.getElement();
        }
        parameters.setTimestampElement(timestamp);
        
        // Validate security policies
        Map<QName, SecurityPolicyValidator> validators = PolicyUtils.getSecurityPolicyValidators(msg);
        for (Map.Entry<QName, Collection<AssertionInfo>> entry : aim.entrySet()) {
            // Check to see if we have a security policy + if we can validate it
            if (validators.containsKey(entry.getKey())) {
                validators.get(entry.getKey()).validatePolicies(parameters, entry.getValue());
            }
        }
        
        super.doResults(msg, actor, soapHeader, soapBody, results, utWithCallbacks);
    }
    
}
