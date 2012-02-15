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

import java.util.Arrays;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.junit.Test;

public class PolicyBasedWss4JInOutTest extends AbstractPolicySecurityTest {

    @Test
    public void testSignedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                "signed_elements_policy.xml",
                null,
                SP12Constants.SIGNED_ELEMENTS,
                CoverageType.SIGNED);
    }
    
    @Test
    public void testSignedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_elements_policy.xml",
                SP12Constants.SIGNED_ELEMENTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_elements_policy.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_ELEMENTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
    }
    
    @Test
    public void testAsymmetricBindingAlgorithmSuitePolicy() throws Exception {
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_elements_policy.xml",
                Arrays.asList(SP12Constants.ASYMMETRIC_BINDING),
                null,
                Arrays.asList(SP12Constants.ASYMMETRIC_BINDING),
                null,
                Arrays.asList(CoverageType.SIGNED));
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_elements_Basic256Sha256_policy.xml",
                Arrays.asList(SP12Constants.ASYMMETRIC_BINDING),
                null,
                Arrays.asList(SP12Constants.ASYMMETRIC_BINDING),
                null,
                Arrays.asList(CoverageType.SIGNED));
    }
    
    // TODO this test does not follow the traditional pattern as no server-side enforcement
    // of algorithm suites yet exists.  This support is blocked on WSS4J patches.  In the interim
    // the outbound side is tested ONLY.
    @Test
    public void testSignedElementsWithIssuedSAMLToken() throws Exception {
        this.runOutInterceptorAndValidateSamlTokenAttached(
                "signed_elements_with_sst_issued_token_policy.xml");
    }

    @Test
    public void testSignedPartsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_body.xml",
                "signed_parts_policy_body.xml",
                null,
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                "signed_parts_policy_header_namespace_only.xml",
                null,
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                "signed_parts_policy_header.xml",
                null,
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
    }
    
    @Test
    public void testSignedPartsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_parts_policy_body.xml",
                SP12Constants.SIGNED_PARTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_body.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_parts_policy_header_namespace_only.xml",
                SP12Constants.SIGNED_PARTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_header_namespace_only.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_parts_policy_header.xml",
                SP12Constants.SIGNED_PARTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_header.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_parts_policy_header_and_body.xml",
                SP12Constants.SIGNED_PARTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_header_and_body.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
    }
    
    @Test
    public void testEncryptedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_missing_enc_header.xml",
                "encrypted_elements_policy.xml",
                null,
                SP12Constants.ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
        
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_elements_policy2.xml",
                null,
                SP12Constants.ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testEncryptedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_elements_policy.xml",
                SP12Constants.ENCRYPTED_ELEMENTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_elements_policy.xml",
                null,
                null,
                Arrays.asList(new QName[] {SP12Constants.ENCRYPTED_ELEMENTS}),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
        
        this.runInInterceptorAndValidate(
                "encrypted_body_element.xml",
                "encrypted_elements_policy2.xml",
                SP12Constants.ENCRYPTED_ELEMENTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_elements_policy2.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_ELEMENTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
    }
    
    @Test
    public void testContentEncryptedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_element.xml",
                "content_encrypted_elements_policy.xml",
                null,
                SP12Constants.CONTENT_ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testContentEncryptedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "content_encrypted_elements_policy.xml",
                SP12Constants.CONTENT_ENCRYPTED_ELEMENTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "content_encrypted_elements_policy.xml",
                null,
                null,
                Arrays.asList(SP12Constants.CONTENT_ENCRYPTED_ELEMENTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
    }
    
    @Test
    public void testEncryptedPartsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_missing_enc_body.xml",
                "encrypted_parts_policy_body.xml",
                null,
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runInInterceptorAndValidate(
                "encrypted_body_element.xml",
                "encrypted_parts_policy_body.xml",
                null,
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runInInterceptorAndValidate(
                "encrypted_missing_enc_header.xml",
                "encrypted_parts_policy_header_namespace_only.xml",
                null,
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runInInterceptorAndValidate(
                "encrypted_missing_enc_header.xml",
                "encrypted_parts_policy_header.xml",
                null,
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testEncryptedPartsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_body.xml",
                SP12Constants.ENCRYPTED_PARTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_body.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
        
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header_namespace_only.xml",
                SP12Constants.ENCRYPTED_PARTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_header_namespace_only.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
        
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header.xml",
                SP12Constants.ENCRYPTED_PARTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_header.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
        
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header_and_body.xml",
                SP12Constants.ENCRYPTED_PARTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_header_and_body.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
    }
    
    @Test
    public void testSignedEncryptedPartsWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_encrypted_missing_enc_header.xml",
                "signed_parts_policy_header_and_body_encrypted.xml",
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                Arrays.asList(CoverageType.ENCRYPTED,
                        CoverageType.SIGNED));
    }
    
    @Test
    public void testSignedEncryptedPartsWithCompleteCoverage() throws Exception {
        if (!checkUnrestrictedPoliciesInstalled()) {
            return;
        }
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_encrypted.xml",
                "signed_parts_policy_header_and_body_encrypted.xml",
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS, 
                        SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED,
                        CoverageType.SIGNED));
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_header_and_body_encrypted.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS, 
                        SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED,
                        CoverageType.SIGNED));
    }
    
    @Test
    public void testEncryptedSignedPartsWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content_signed_missing_signed_header.xml",
                "encrypted_parts_policy_header_and_body_signed.xml",
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                Arrays.asList(CoverageType.ENCRYPTED, CoverageType.SIGNED));
    }
    
    @Test
    public void testEncryptedSignedPartsWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content_signed.xml",
                "encrypted_parts_policy_header_and_body_signed.xml",
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS, 
                        SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED, CoverageType.SIGNED));
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_header_and_body_signed.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS,
                        SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED,
                        CoverageType.SIGNED));
    }
    
    @Test
    public void testProtectTokenAssertion() throws Exception {
        
        // ////////////////////////////////////////////////////
        // x509 Direct Ref Tests
        
        /* REVISIT
        No inbound validation is available for the PROTECT_TOKENS assertion.
        We cannot yet test inbound in the standard manner.  Since we can't
        test inbound, we can't test reound trip either and thus must take
        a different approach for now.
         
        this.runInInterceptorAndValidate(
                "signed_x509_direct_ref_token_prot.xml",
                "protect_token_policy_asym_x509_direct_ref.xml",
                SP12Constants.PROTECT_TOKENS,
                null,
                CoverageType.SIGNED);

        this.runInInterceptorAndValidate(
                "signed_x509_direct_ref.xml",
                "protect_token_policy_asym_x509_direct_ref.xml",
                null,
                SP12Constants.PROTECT_TOKENS,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "protect_token_policy_asym_x509_direct_ref.xml",
                null,
                null,
                Arrays.asList(new QName[] {SP12Constants.PROTECT_TOKENS }),
                null,
                Arrays.asList(new CoverageType[] {CoverageType.SIGNED }));
        */
        
        // REVISIT
        // We test using a policy with ProtectTokens enabled on
        // the outbound but with a policy using a SignedElements policy
        // on the inbound to validate that the correct thing got signed.
        this.runAndValidate(
                "wsse-request-clean.xml",
                "protect_token_policy_asym_x509_direct_ref.xml",
                "protect_token_policy_asym_x509_direct_ref_complement.xml",
                new AssertionsHolder(
                        Arrays.asList(new QName[] {SP12Constants.ASYMMETRIC_BINDING}),
                        null),
                new AssertionsHolder(
                        Arrays.asList(new QName[] {SP12Constants.SIGNED_ELEMENTS}),
                        null),
                Arrays.asList(new CoverageType[] {CoverageType.SIGNED }));
        
        // ////////////////////////////////////////////////////
        // x509 Issuer Serial Tests
        
        /* REVISIT
        No inbound validation is available for the PROTECT_TOKENS assertion.
        We cannot yet test inbound in the standard manner.  Since we can't
        test inbound, we can't test reound trip either and thus must take
        a different approach for now.
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_token_prot.xml",
                "protect_token_policy_asym_x509_issuer_serial.xml",
                SP12Constants.PROTECT_TOKENS,
                null,
                CoverageType.SIGNED);

        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "protect_token_policy_asym_x509_issuer_serial.xml",
                null,
                SP12Constants.PROTECT_TOKENS,
                CoverageType.SIGNED);

        this.runAndValidate(
                "wsse-request-clean.xml",
                "protect_token_policy_asym_x509_issuer_serial.xml",
                null,
                null,
                Arrays.asList(new QName[] { SP12Constants.PROTECT_TOKENS }),
                null,
                Arrays.asList(new CoverageType[] { CoverageType.SIGNED }));
        */
        
        // REVISIT
        // We test using a policy with ProtectTokens enabled on
        // the outbound but with a policy using a SignedElements policy
        // on the inbound to validate that the correct thing got signed.
        this.runAndValidate(
                "wsse-request-clean.xml",
                "protect_token_policy_asym_x509_issuer_serial.xml",
                "protect_token_policy_asym_x509_issuer_serial_complement.xml",
                new AssertionsHolder(
                        Arrays.asList(new QName[] {SP12Constants.ASYMMETRIC_BINDING}),
                        null),
                new AssertionsHolder(
                        Arrays.asList(new QName[] {SP12Constants.SIGNED_ELEMENTS}),
                        null),
                Arrays.asList(new CoverageType[] {CoverageType.SIGNED }));

        // ////////////////////////////////////////////////////
        // x509 Key Identifier Tests

        // TODO: Tests for Key Identifier are needed but require that the
        // certificates used in the test cases be updated to version 3
        // according to WSS4J.
        
        // TODO: Tests for derived keys.
    }
  
}
