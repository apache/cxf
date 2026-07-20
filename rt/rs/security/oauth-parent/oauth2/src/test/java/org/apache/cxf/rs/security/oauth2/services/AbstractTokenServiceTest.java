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
package org.apache.cxf.rs.security.oauth2.services;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.ext.MessageContext;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.security.transport.TLSSessionInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractTokenServiceTest {

    @Test
    public void testGetClientReportsInvalidClientWhenClientNotFound() {
        TestTokenService service = new TestTokenService();

        InvalidClientException ex = assertThrows(InvalidClientException.class,
            () -> service.callGetClient("missing-client", "secret", new MetadataMap<>()));

        assertNotNull(ex.getError());
        assertEquals(OAuthConstants.INVALID_CLIENT, ex.getError().getError());
    }

    @Test
    public void testInvalidClientInvalidCharactersHandled() {
        TestTokenService service = new TestTokenService();

        String clientId = "alice\r\n\r\n\r\n[FAKE]+Admin+login+successful";
        InvalidClientException ex = assertThrows(InvalidClientException.class,
            () -> service.callGetClient(clientId, "secret", new MetadataMap<>()));

        assertNotNull(ex.getError());
        assertEquals(OAuthConstants.INVALID_CLIENT, ex.getError().getError());
    }

    @Test
    public void testGetClientReportsCustomErrorWhenProviderThrowsIt() {
        TestTokenService service = new TestTokenService();
        OAuthError customError = new OAuthError(OAuthConstants.UNAUTHORIZED_CLIENT);
        service.setException(new OAuthServiceException(customError));

        InvalidClientException ex = assertThrows(InvalidClientException.class,
            () -> service.callGetClient("client", "secret", new MetadataMap<>()));

        assertSame(customError, ex.getError());
    }

    @Test
    public void testGetClientReturnsClientWhenFound() {
        TestTokenService service = new TestTokenService();
        Client expected = new Client("client", "secret", true);
        service.setClient(expected);

        Client actual = service.callGetClient("client", "secret", new MetadataMap<>());
        assertSame(expected, actual);
    }

    @Test
    public void testCheckCertificateBindingAcceptsDnOnlyClient() {
        TestTokenService service = new TestTokenService();
        Client client = new Client("c1", null, true);
        client.getProperties().put(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN, "CN=peer,O=Test,C=US");

        service.callCheckCertificateBinding(client,
            makeTlsSession("CN=peer,O=Test,C=US", "CN=issuer,O=Test,C=US"));
    }

    @Test
    public void testCheckCertificateBindingRejectsDnOnlyClientMismatch() {
        TestTokenService service = new TestTokenService();
        Client client = new Client("c1", null, true);
        client.getProperties().put(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN, "CN=peer,O=Test,C=US");

        InvalidClientException ex = assertThrows(InvalidClientException.class,
            () -> service.callCheckCertificateBinding(client,
                makeTlsSession("CN=other,O=Test,C=US", "CN=issuer,O=Test,C=US")));

        assertNotNull(ex.getError());
        assertEquals(OAuthConstants.INVALID_CLIENT, ex.getError().getError());
    }

    @Test
    public void testCheckCertificateBindingRejectsCertificateMismatch() {
        TestTokenService service = new TestTokenService();
        Client client = new Client("c2", null, true);
        client.getApplicationCertificates().add(Base64Utility.encode(new byte[] {9, 9, 9}));

        InvalidClientException ex = assertThrows(InvalidClientException.class,
            () -> service.callCheckCertificateBinding(client,
                makeTlsSession("CN=peer,O=Test,C=US", "CN=issuer,O=Test,C=US")));

        assertNotNull(ex.getError());
        assertEquals(OAuthConstants.INVALID_CLIENT, ex.getError().getError());
    }

    @Test
    public void testCheckCertificateBindingAcceptsCertificateMatch() throws Exception {
        TestTokenService service = new TestTokenService();
        X509Certificate cert = createCertificate("CN=peer,O=Test,C=US", "CN=issuer,O=Test,C=US");

        Client client = new Client("c3", null, true);
        client.getApplicationCertificates().add(Base64Utility.encode(cert.getEncoded()));

        service.callCheckCertificateBinding(client, makeTlsSession(cert));
    }

    private static TLSSessionInfo makeTlsSession(String subjectDn, String issuerDn) {
        X509Certificate cert = createCertificate(subjectDn, issuerDn);
        return makeTlsSession(cert);
    }

    private static TLSSessionInfo makeTlsSession(X509Certificate cert) {
        return new TLSSessionInfo("TLS_FAKE", null, new Certificate[] {cert});
    }

    private static X509Certificate createCertificate(String subjectDn, String issuerDn) {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getSubjectX500Principal()).thenReturn(new X500Principal(subjectDn));
        when(cert.getIssuerX500Principal()).thenReturn(new X500Principal(issuerDn));
        try {
            when(cert.getEncoded()).thenReturn(new byte[] {1, 2, 3});
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return cert;
    }

    private static final class TestTokenService extends AbstractTokenService {
        private Client client;
        private OAuthServiceException exception;

        Client callGetClient(String clientId, String clientSecret, MultivaluedMap<String, String> params) {
            return getClient(clientId, clientSecret, params);
        }

        void callCheckCertificateBinding(Client boundClient, TLSSessionInfo tlsSessionInfo) {
            MessageContext mc = mock(MessageContext.class);
            SecurityContext sc = mock(SecurityContext.class);
            when(sc.getAuthenticationScheme()).thenReturn("");
            when(mc.getSecurityContext()).thenReturn(sc);
            setMessageContext(mc);
            checkCertificateBinding(boundClient, tlsSessionInfo);
        }

        void setClient(Client client) {
            this.client = client;
        }

        void setException(OAuthServiceException exception) {
            this.exception = exception;
        }

        @Override
        protected Client getValidClient(String clientId, String clientSecret, MultivaluedMap<String, String> params)
            throws OAuthServiceException {
            if (exception != null) {
                throw exception;
            }
            return client;
        }

        @Override
        protected void reportInvalidClient() {
            throw new InvalidClientException(new OAuthError(OAuthConstants.INVALID_CLIENT));
        }

        @Override
        protected void reportInvalidClient(OAuthError error) {
            throw new InvalidClientException(error);
        }
    }

    private static class InvalidClientException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final OAuthError error;

        InvalidClientException(OAuthError error) {
            this.error = error;
        }

        OAuthError getError() {
            return error;
        }
    }
}
