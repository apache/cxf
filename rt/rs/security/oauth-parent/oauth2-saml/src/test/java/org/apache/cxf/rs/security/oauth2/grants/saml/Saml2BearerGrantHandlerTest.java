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
package org.apache.cxf.rs.security.oauth2.grants.saml;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Saml2BearerGrantHandler} Holder-of-Key binding validation.
 * Tests the extraction and validation logic for X.509 certificate subjects and SAML assertion subjects.
 */
public class Saml2BearerGrantHandlerTest {
    private final Saml2BearerGrantHandler handler = new Saml2BearerGrantHandler();

    /**
     * Test extracting CN from a standard X.500 DN
     */
    @Test
    public void testExtractCNValueStandard() {
        String dn = "CN=alice@example.com,O=Acme,C=US";
        String cn = handler.extractCNValue(dn);
        assertEquals("alice@example.com", cn);
    }

    /**
     * Test extracting CN when it's the only component
     */
    @Test
    public void testExtractCNValueOnly() {
        String dn = "CN=bob@example.com";
        String cn = handler.extractCNValue(dn);
        assertEquals("bob@example.com", cn);
    }

    /**
     * Test extracting CN when DN has spaces around values
     */
    @Test
    public void testExtractCNValueWithSpaces() {
        String dn = "CN = charlie@example.com , O = Acme , C = US";
        String cn = handler.extractCNValue(dn);
        assertEquals("charlie@example.com", cn);
    }

    /**
     * Test extracting CN when CN doesn't exist in DN
     */
    @Test
    public void testExtractCNValueNotFound() {
        String cn = handler.extractCNValue("O=Acme,C=US");
        assertNull("CN should not be found in DN without CN component", cn);
    }

    /**
     * Test extracting CN from null DN
     */
    @Test
    public void testExtractCNValueNullDN() {
        String result = handler.extractCNValue(null);
        assertNull("Should return null for null DN", result);
    }

    /**
     * Test extracting CN with empty DN
     */
    @Test
    public void testExtractCNValueEmptyDN() {
        String result = handler.extractCNValue("");
        assertNull("Should return null for empty DN", result);
    }

    /**
     * Test extracting CN with complex DN that has multiple levels
     */
    @Test
    public void testExtractCNValueComplexDN() {
        String dn = "CN=alice@example.com,OU=Engineering,O=Acme,C=US";
        String cn = handler.extractCNValue(dn);
        assertEquals("alice@example.com", cn);
    }

    /**
     * Test extracting from DN with equals sign in value (edge case)
     */
    @Test
    public void testExtractCNValueWithEqualsInValue() {
        // This is an edge case - the regex should extract up to the first comma
        String dn = "CN=user=alice@example.com,O=Acme,C=US";
        String cn = handler.extractCNValue(dn);
        // The regex will match up to first comma
        assertEquals("user=alice@example.com", cn);
    }

    /**
     * Test extracting assertion subject from null wrapper
     */
    @Test
    public void testExtractAssertionSubjectNullWrapper() {
        String subject = handler.extractAssertionSubject(null);
        assertNull("Should return null for null assertion", subject);
    }

    /**
     * Test extracting certificate subject identifier from null certificate
     */
    @Test
    public void testExtractCertificateSubjectIdentifierNullCertificate() {
        String subject = handler.extractCertificateSubjectIdentifier(null);
        assertNull("Should return null for null certificate", subject);
    }

    /**
     * Test extracting certificate subject identifier with non-X509 certificate
     */
    @Test
    public void testExtractCertificateSubjectIdentifierNonX509Certificate() {
        Certificate mockCert = new Certificate("MockType") {
            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public void verify(java.security.PublicKey key) {
            }

            @Override
            public void verify(java.security.PublicKey key, String sigProvider) {
            }

            @Override
            public String toString() {
                return "MockCertificate";
            }

            @Override
            public java.security.PublicKey getPublicKey() {
                return null;
            }
        };

        String subject = handler.extractCertificateSubjectIdentifier(mockCert);
        assertNull("Should return null for non-X509 certificate", subject);
    }

    @Test
    public void testValidateUnsignedAssertionBindingMatchesTLSCertificate() {
        Saml2BearerGrantHandler bindingHandler = createHandlerWithAssertionSubject("alice@example.com");
        Message message = createMessageWithPeerCertificate("CN=alice@example.com,O=Acme,C=US");
        SamlAssertionWrapper assertion = mock(SamlAssertionWrapper.class);

        bindingHandler.validateUnsignedAssertionBinding(message, assertion);
    }

    @Test
    public void testValidateUnsignedAssertionBindingRejectsSubjectMismatch() {
        Saml2BearerGrantHandler bindingHandler = createHandlerWithAssertionSubject("mallory@example.com");
        Message message = createMessageWithPeerCertificate("CN=alice@example.com,O=Acme,C=US");
        SamlAssertionWrapper assertion = mock(SamlAssertionWrapper.class);

        try {
            bindingHandler.validateUnsignedAssertionBinding(message, assertion);
            fail("Expected OAuthServiceException for mismatched SAML subject and TLS certificate CN");
        } catch (OAuthServiceException ex) {
            // expected
        }
    }

    private Saml2BearerGrantHandler createHandlerWithAssertionSubject(String assertionSubject) {
        return new Saml2BearerGrantHandler() {
            @Override
            protected String extractAssertionSubject(SamlAssertionWrapper assertion) {
                return assertionSubject;
            }
        };
    }

    private Message createMessageWithPeerCertificate(String subjectDn) {
        Message message = new MessageImpl();
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getSubjectX500Principal()).thenReturn(new X500Principal(subjectDn));
        TLSSessionInfo tlsInfo = new TLSSessionInfo("TLS_FAKE", null, new Certificate[] {cert});
        message.put(TLSSessionInfo.class, tlsInfo);
        return message;
    }
}
