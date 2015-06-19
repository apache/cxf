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
package org.apache.cxf.ws.security.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.cxf.ws.security.wss4j.policyvalidators.AlgorithmSuitePolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.AsymmetricBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.ConcreteSupportingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EndorsingEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EndorsingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.IssuedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.KerberosTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.LayoutPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.RequiredElementsPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.RequiredPartsPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SamlTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecuredElementsPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecuredPartsPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecurityContextTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecurityPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEndorsingEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEndorsingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SymmetricBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.TransportBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.UsernameTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.WSS11PolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.X509TokenPolicyValidator;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractBinding;

/**
 * Some common functionality that can be shared for working with policies
 */
public final class PolicyUtils {
    
    // The default security policy validators
    private static final Map<QName, SecurityPolicyValidator> DEFAULT_SECURITY_POLICY_VALIDATORS =
        new HashMap<>();
    
    static {
        configureTokenValidators();
        configureBindingValidators();
        configureSupportingTokenValidators();
        configurePartsValidators();
    }
    
    private PolicyUtils() {
        // complete
    }
    
    private static void configureTokenValidators() {
        SecurityPolicyValidator validator = new X509TokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.X509_TOKEN, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.X509_TOKEN, validator);
        validator = new UsernameTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.USERNAME_TOKEN, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.USERNAME_TOKEN, validator);
        validator = new SamlTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SAML_TOKEN, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.SAML_TOKEN, validator);
        validator = new SecurityContextTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SECURITY_CONTEXT_TOKEN, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.SECURITY_CONTEXT_TOKEN, validator);
        validator = new WSS11PolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.WSS11, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.WSS11, validator);
        validator = new IssuedTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.ISSUED_TOKEN, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.ISSUED_TOKEN, validator);
        validator = new KerberosTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.KERBEROS_TOKEN, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.KERBEROS_TOKEN, validator);
    }
    
    private static void configureBindingValidators() {
        SecurityPolicyValidator validator = new TransportBindingPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.TRANSPORT_BINDING, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.TRANSPORT_BINDING, validator);
        validator = new SymmetricBindingPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SYMMETRIC_BINDING, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.SYMMETRIC_BINDING, validator);
        validator = new AsymmetricBindingPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.ASYMMETRIC_BINDING, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.ASYMMETRIC_BINDING, validator);
        validator = new AlgorithmSuitePolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.ALGORITHM_SUITE, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.ALGORITHM_SUITE, validator);
        validator = new LayoutPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.LAYOUT, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.LAYOUT, validator);
    }
    
    private static void configureSupportingTokenValidators() {
        SecurityPolicyValidator validator = new ConcreteSupportingTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SUPPORTING_TOKENS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.SUPPORTING_TOKENS, validator);
        validator = new SignedTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SIGNED_SUPPORTING_TOKENS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.SIGNED_SUPPORTING_TOKENS, validator);
        validator = new EndorsingTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.ENDORSING_SUPPORTING_TOKENS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.ENDORSING_SUPPORTING_TOKENS, validator);
        validator = new SignedEndorsingTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS, validator);
        validator = new EncryptedTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.ENCRYPTED_SUPPORTING_TOKENS, validator);
        validator = new SignedEncryptedTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS, validator);
        validator = new EndorsingEncryptedTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS, validator);
        validator = new SignedEndorsingEncryptedTokenPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS, validator);
    }
    
    private static void configurePartsValidators() {
        SecurityPolicyValidator validator = new SecuredPartsPolicyValidator();
        ((SecuredPartsPolicyValidator)validator).setCoverageType(CoverageType.SIGNED);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SIGNED_PARTS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.SIGNED_PARTS, validator);
        validator = new SecuredPartsPolicyValidator();
        ((SecuredPartsPolicyValidator)validator).setCoverageType(CoverageType.ENCRYPTED);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.ENCRYPTED_PARTS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.ENCRYPTED_PARTS, validator);
        validator = new SecuredElementsPolicyValidator();
        ((SecuredElementsPolicyValidator)validator).setCoverageType(CoverageType.SIGNED);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.SIGNED_ELEMENTS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.SIGNED_ELEMENTS, validator);
        validator = new SecuredElementsPolicyValidator();
        ((SecuredElementsPolicyValidator)validator).setCoverageType(CoverageType.ENCRYPTED);
        ((SecuredElementsPolicyValidator)validator).setCoverageScope(CoverageScope.ELEMENT);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.ENCRYPTED_ELEMENTS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.ENCRYPTED_ELEMENTS, validator);
        validator = new SecuredElementsPolicyValidator();
        ((SecuredElementsPolicyValidator)validator).setCoverageType(CoverageType.ENCRYPTED);
        ((SecuredElementsPolicyValidator)validator).setCoverageScope(CoverageScope.CONTENT);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.CONTENT_ENCRYPTED_ELEMENTS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.CONTENT_ENCRYPTED_ELEMENTS, validator);
        validator = new RequiredPartsPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.REQUIRED_PARTS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.REQUIRED_PARTS, validator);
        validator = new RequiredElementsPolicyValidator();
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP12Constants.REQUIRED_ELEMENTS, validator);
        DEFAULT_SECURITY_POLICY_VALIDATORS.put(SP11Constants.REQUIRED_ELEMENTS, validator);
    }
    
    public static Collection<AssertionInfo> getAllAssertionsByLocalname(
        AssertionInfoMap aim, String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));

        if ((sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty())) {
            Collection<AssertionInfo> ais = new HashSet<>();
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

    public static boolean assertPolicy(AssertionInfoMap aim, QName name) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(name);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }    
            return true;
        }
        return false;
    }
    
    public static boolean assertPolicy(AssertionInfoMap aim, String localname) {
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, localname);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }    
            return true;
        }
        return false;
    }
    
    public static AssertionInfo getFirstAssertionByLocalname(
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

    public static boolean isThereAnAssertionByLocalname(
        AssertionInfoMap aim, String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));

        return (sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty());
    }
    
    public static AbstractBinding getSecurityBinding(AssertionInfoMap aim) {
        
        AssertionInfo asymAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (asymAis != null) {
            asymAis.setAsserted(true);
            return (AbstractBinding)asymAis.getAssertion();
        }

        AssertionInfo symAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (symAis != null) {
            symAis.setAsserted(true);
            return (AbstractBinding)symAis.getAssertion();
        }
        
        AssertionInfo transAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        if (transAis != null) {
            transAis.setAsserted(true);
            return (AbstractBinding)transAis.getAssertion();
        }
        
        return null;
    }

    public static Map<QName, SecurityPolicyValidator> getSecurityPolicyValidators(Message message) {
        Map<QName, SecurityPolicyValidator> mapToReturn = new HashMap<>(DEFAULT_SECURITY_POLICY_VALIDATORS); 
        Map<QName, SecurityPolicyValidator> policyMap = 
            CastUtils.cast((Map<?, ?>)message.getContextualProperty(SecurityConstants.POLICY_VALIDATOR_MAP));
        
        // Allow overriding the default policies
        if (policyMap != null && !policyMap.isEmpty()) {
            mapToReturn.putAll(policyMap);
        }
        
        return mapToReturn;
    }
}
