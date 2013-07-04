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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;

/**
 * This interceptor marks the CXF AssertionInfos as asserted. WSS4J 2.0 (StAX) takes care of all
 * policy validation, so we are just asserting the appropriate AssertionInfo objects in CXF to 
 * make sure that policy validation passes.
 */
public class PolicyStaxActionInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    
    public PolicyStaxActionInInterceptor() {
        super(Phase.PRE_PROTOCOL);
        this.getBefore().add(StaxSecurityContextInInterceptor.class.getName());
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {
        
        AssertionInfoMap aim = soapMessage.get(AssertionInfoMap.class);
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingSecurityEventList = 
            (List<SecurityEvent>)soapMessage.get(SecurityEvent.class.getName() + ".in");
        if (aim == null || incomingSecurityEventList == null) {
            return;
        }
        
        verifyTokens(aim, incomingSecurityEventList);
        verifyPartsAndElements(aim, incomingSecurityEventList, soapMessage);
        verifyBindings(aim);
    }
    
    private void verifyPartsAndElements(
        AssertionInfoMap aim, List<SecurityEvent> incomingSecurityEventList,
        SoapMessage soapMessage
    ) {
        assertAllAssertionsByLocalname(aim, SPConstants.SIGNED_PARTS);
        assertAllAssertionsByLocalname(aim, SPConstants.SIGNED_ELEMENTS);
        assertAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_PARTS);
        assertAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_ELEMENTS);
        assertAllAssertionsByLocalname(aim, SPConstants.CONTENT_ENCRYPTED_ELEMENTS);

        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRED_PARTS);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRED_ELEMENTS);
    }

    private void verifyTokens(
        AssertionInfoMap aim, List<SecurityEvent> incomingSecurityEventList
    ) {
        // UsernameToken
        assertAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN10);
        assertAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN11);
        assertAllAssertionsByLocalname(aim, SPConstants.HASH_PASSWORD);
        assertAllAssertionsByLocalname(aim, SPConstants.NO_PASSWORD);
        Collection<AssertionInfo> sp13Ais = aim.get(SP13Constants.NONCE);
        if (sp13Ais != null) {
            for (AssertionInfo ai : sp13Ais) {
                ai.setAsserted(true);
            }
        }
        sp13Ais = aim.get(SP13Constants.CREATED);
        if (sp13Ais != null) {
            for (AssertionInfo ai : sp13Ais) {
                ai.setAsserted(true);
            }
        }
        
        // X509
        assertAllAssertionsByLocalname(aim, SPConstants.X509_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS_X509_PKCS7_TOKEN10);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS_X509_PKCS7_TOKEN11);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS_X509_PKI_PATH_V1_TOKEN10);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS_X509_PKI_PATH_V1_TOKEN11);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS_X509_V1_TOKEN10);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS_X509_V1_TOKEN11);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS_X509_V3_TOKEN10);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS_X509_V3_TOKEN11);
        
        // SAML
        assertAllAssertionsByLocalname(aim, SPConstants.SAML_TOKEN);
        assertAllAssertionsByLocalname(aim, "WssSamlV11Token10");
        assertAllAssertionsByLocalname(aim, "WssSamlV11Token11");
        assertAllAssertionsByLocalname(aim, "WssSamlV20Token11");
        
        // SCT
        assertAllAssertionsByLocalname(aim, SPConstants.SECURITY_CONTEXT_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_EXTERNAL_URI_REFERENCE);
        
        for (SecurityEvent event : incomingSecurityEventList) {
            if (WSSecurityEventConstants.Timestamp == event.getSecurityEventType()) {
                assertAllAssertionsByLocalname(aim, "Timestamp");
            }
        }
        
        assertAllAssertionsByLocalname(aim, SPConstants.SUPPORTING_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.SIGNED_SUPPORTING_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_SUPPORTING_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.ENDORSING_SUPPORTING_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
    }
    
    private void verifyBindings(AssertionInfoMap aim) {
        assertAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        assertAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        assertAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        assertAllAssertionsByLocalname(aim, SPConstants.PROTECTION_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.INITIATOR_ENCRYPTION_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.INITIATOR_SIGNATURE_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.INITIATOR_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.RECIPIENT_ENCRYPTION_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.RECIPIENT_SIGNATURE_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.RECIPIENT_TOKEN);
        
        assertAllAssertionsByLocalname(aim, SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY);
        assertAllAssertionsByLocalname(aim, SPConstants.PROTECT_TOKENS);
        assertAllAssertionsByLocalname(aim, SPConstants.INCLUDE_TIMESTAMP);
        assertAllAssertionsByLocalname(aim, SPConstants.ENCRYPT_SIGNATURE);
        assertAllAssertionsByLocalname(aim, SPConstants.SIGN_BEFORE_ENCRYPTING);
        assertAllAssertionsByLocalname(aim, SPConstants.ENCRYPT_BEFORE_SIGNING);
        assertAllAssertionsByLocalname(aim, SPConstants.LAYOUT);
        assertAllAssertionsByLocalname(aim, SPConstants.LAYOUT_LAX);
        assertAllAssertionsByLocalname(aim, SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST);
        assertAllAssertionsByLocalname(aim, SPConstants.LAYOUT_LAX_TIMESTAMP_LAST);
        assertAllAssertionsByLocalname(aim, SPConstants.LAYOUT_STRICT);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_DERIVED_KEYS);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_SIGNATURE_CONFIRMATION);
        
        assertAllAssertionsByLocalname(aim, SPConstants.ALGORITHM_SUITE);
        assertAllAlgorithmSuites(SP11Constants.SP_NS, aim);
        assertAllAlgorithmSuites(SP12Constants.SP_NS, aim);
        
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_INTERNAL_REFERENCE);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_EXTERNAL_REFERENCE);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_THUMBPRINT_REFERENCE);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_EMBEDDED_TOKEN_REFERENCE);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_ISSUER_SERIAL_REFERENCE);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE);
        
        assertAllAssertionsByLocalname(aim, SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER);
        assertAllAssertionsByLocalname(aim, SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL);
        assertAllAssertionsByLocalname(aim, SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI);
        assertAllAssertionsByLocalname(aim, SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.MUST_SUPPORT_ISSUED_TOKENS);

        assertAllAssertionsByLocalname(aim, SPConstants.MUST_SUPPORT_REF_THUMBPRINT);
        assertAllAssertionsByLocalname(aim, SPConstants.MUST_SUPPORT_REF_ENCRYPTED_KEY);
        
        assertAllAssertionsByLocalname(aim, SPConstants.KEY_VALUE_TOKEN);
        assertAllAssertionsByLocalname(aim, SPConstants.RSA_KEY_VALUE);
        
        assertAllAssertionsByLocalname(aim, SPConstants.WSS10);
        assertAllAssertionsByLocalname(aim, SPConstants.WSS11);
        
        assertAllAssertionsByLocalname(aim, SPConstants.TRUST_10);
        assertAllAssertionsByLocalname(aim, SPConstants.TRUST_13);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_CLIENT_ENTROPY);
        assertAllAssertionsByLocalname(aim, SPConstants.REQUIRE_SERVER_ENTROPY);
    }
    
    private void assertAllAssertionsByLocalname(AssertionInfoMap aim, String localname) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        if (sp11Ais != null) {
            for (AssertionInfo ai : sp11Ais) {
                ai.setAsserted(true);
            }
        }
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));
        if (sp12Ais != null) {
            for (AssertionInfo ai : sp12Ais) {
                ai.setAsserted(true);
            }
        }
    }
    
    private void assertAllAlgorithmSuites(String spNamespace, AssertionInfoMap aim) {
        Collection<AssertionInfo> sp11Ais = 
            aim.get(new QName(spNamespace, SPConstants.ALGORITHM_SUITE));
        if (sp11Ais != null) {
            for (AssertionInfo ai : sp11Ais) {
                ai.setAsserted(true);
                AlgorithmSuite algorithmSuite = (AlgorithmSuite)ai.getAssertion();
                AlgorithmSuiteType algorithmSuiteType = algorithmSuite.getAlgorithmSuiteType();
                String namespace = algorithmSuiteType.getNamespace();
                if (namespace == null) {
                    namespace = spNamespace;
                }
                Collection<AssertionInfo> algAis = 
                    aim.get(new QName(namespace, algorithmSuiteType.getName()));
                if (algAis != null) {
                    for (AssertionInfo algAi : algAis) {
                        algAi.setAsserted(true);
                    }
                }
            }
        }
    }


}
