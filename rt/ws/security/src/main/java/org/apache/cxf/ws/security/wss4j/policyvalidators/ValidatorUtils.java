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
package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;

/**
 * Configure the Validators
 */
public final class ValidatorUtils {

    // The default security policy validators
    private static final Map<QName, SecurityPolicyValidator> DEFAULT_SECURITY_POLICY_VALIDATORS =
        new HashMap<>();

    static {
        configureTokenValidators();
        configureBindingValidators();
        configureSupportingTokenValidators();
        configurePartsValidators();
    }

    private ValidatorUtils() {
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
