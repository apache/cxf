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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.security.transport.TLSSessionInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicRegistrationServiceTest {

    @Test
    public void testRejectsUnallowedRegisteredScope() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();
        service.setAllowedClientScopes(Collections.singletonList("read"));

        ClientRegistration request = new ClientRegistration();
        request.setScope("admin read");

        Client client = createClient();

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> service.applyClientRegistration(request, client));

        assertNotNull(ex.getResponse());
        OAuthError error = (OAuthError)ex.getResponse().getEntity();
        assertNotNull(error);
        assertEquals("invalid_client_metadata", error.getError());
    }

    @Test
    public void testAcceptsAllowedRegisteredScopes() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();
        service.setAllowedClientScopes(Arrays.asList("read", "write"));

        ClientRegistration request = new ClientRegistration();
        request.setScope("read write");

        Client client = createClient();
        service.applyClientRegistration(request, client);

        assertEquals(Arrays.asList("read", "write"), client.getRegisteredScopes());
    }

    @Test
    public void testAcceptsRegisteredScopesWhenAllowlistNotConfigured() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        ClientRegistration request = new ClientRegistration();
        request.setScope("openid");

        Client client = createClient();
        service.applyClientRegistration(request, client);

        assertEquals(Collections.singletonList("openid"), client.getRegisteredScopes());
    }

    @Test
    public void testAcceptsAllowedRedirectUrlsWebApp() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        ClientRegistration request = new ClientRegistration();
        request.setScope("read write");
        request.setRedirectUris(List.of("https://localhost", "http://localhost"));

        Client client = createClient();
        service.applyClientRegistration(request, client);

        assertEquals(Arrays.asList("https://localhost", "http://localhost"), client.getRedirectUris());
    }

    @Test
    public void testRejectsNotAllowedRedirectUrlsWebApp() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        final List<String> schemes = List.of("http", "https");
        for (String scheme: schemes) {
            ClientRegistration request = new ClientRegistration();
            request.setScope("read write");
            request.setRedirectUris(List.of(scheme + "://localhost"));
    
            Client client = createClient();
            client.setAllowedGrantTypes(Collections.singletonList(OAuthConstants.IMPLICIT_GRANT));
            assertThrows(BadRequestException.class, () -> service.applyClientRegistration(request, client));
        }
    }

    @Test
    public void testAcceptsAllowedRedirectUrlsNativeApp() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        final List<String> hosts = List.of("localhost", "127.0.0.1", "[::1]");
        for (String host: hosts) {
            ClientRegistration request = new ClientRegistration();
            request.setScope("read write");
            request.setRedirectUris(List.of("http://" + host));
            request.setApplicationType("native");

            Client client = createClient();
            service.applyClientRegistration(request, client);

            assertEquals(Arrays.asList("http://" + host), client.getRedirectUris());
        }
    }

    @Test
    public void testRejectsNotAllowedRedirectUrlsNativeApp() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        ClientRegistration request = new ClientRegistration();
        request.setScope("read write");
        request.setRedirectUris(List.of("http://test"));
        request.setApplicationType("native");

        Client client = createClient();
        assertThrows(BadRequestException.class, () -> service.applyClientRegistration(request, client));
    }

    @Test
    public void testRejectsNotAllowedRedirectUrls() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();

        final List<String> uris = List.of("custom://test", "//test", "http:/");
        for (String uri: uris) {
            ClientRegistration request = new ClientRegistration();
            request.setScope("read write");
            request.setRedirectUris(List.of(uri));
    
            Client client = createClient();
            assertThrows(BadRequestException.class, () -> service.applyClientRegistration(request, client));
        }
    }

    @Test
    public void testRejectsTlsClientAuthWithoutTlsCertificate() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();
        service.setEnforceTlsClientAuthCertificateBinding(true);
        service.setMessageContext(createMessageContext("", null));

        ClientRegistration request = new ClientRegistration();
        request.setGrantTypes(Collections.singletonList(OAuthConstants.CLIENT_CREDENTIALS_GRANT));
        request.setTokenEndpointAuthMethod(OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS);
        request.setProperty(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN,
            "CN=client,OU=Test,O=Apache,C=US");

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> service.createClient(request));

        assertInvalidClientMetadata(ex);
    }

    @Test
    public void testRejectsTlsClientAuthWhenSubjectDnDoesNotMatchCertificate() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();
        service.setEnforceTlsClientAuthCertificateBinding(true);
        X509Certificate cert = createCertificate(
            "CN=actual,OU=Test,O=Apache,C=US",
            "CN=issuer,OU=Test,O=Apache,C=US");
        service.setMessageContext(createMessageContext("", cert));

        ClientRegistration request = new ClientRegistration();
        request.setGrantTypes(Collections.singletonList(OAuthConstants.CLIENT_CREDENTIALS_GRANT));
        request.setTokenEndpointAuthMethod(OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS);
        request.setProperty(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN,
            "CN=expected,OU=Test,O=Apache,C=US");

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> service.createClient(request));

        assertInvalidClientMetadata(ex);
    }

    @Test
    public void testAcceptsTlsClientAuthWhenSubjectDnMatchesCertificate() {
        TestDynamicRegistrationService service = new TestDynamicRegistrationService();
        service.setEnforceTlsClientAuthCertificateBinding(true);
        String subjectDn = "CN=client,OU=Test,O=Apache,C=US";
        X509Certificate cert = createCertificate(subjectDn, "CN=issuer,OU=Test,O=Apache,C=US");
        service.setMessageContext(createMessageContext("", cert));

        ClientRegistration request = new ClientRegistration();
        request.setGrantTypes(Collections.singletonList(OAuthConstants.CLIENT_CREDENTIALS_GRANT));
        request.setTokenEndpointAuthMethod(OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS);
        request.setProperty(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN, subjectDn);

        Client client = service.createClient(request);
        assertEquals(subjectDn, client.getProperties().get(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN));
        assertEquals(1, client.getApplicationCertificates().size());
    }

    private static void assertInvalidClientMetadata(BadRequestException ex) {
        assertNotNull(ex.getResponse());
        OAuthError error = (OAuthError)ex.getResponse().getEntity();
        assertNotNull(error);
        assertEquals("invalid_client_metadata", error.getError());
    }

    private static MessageContext createMessageContext(String authScheme, X509Certificate cert) {
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthenticationScheme()).thenReturn(authScheme);

        MessageContext mc = mock(MessageContext.class);
        when(mc.getSecurityContext()).thenReturn(sc);
        if (cert != null) {
            TLSSessionInfo tlsInfo = new TLSSessionInfo("TLS_FAKE", null, new Certificate[] {cert});
            when(mc.get(TLSSessionInfo.class.getName())).thenReturn(tlsInfo);
        }
        return mc;
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

    private static Client createClient() {
        Client client = new Client("client", "secret", true);
        client.setAllowedGrantTypes(Collections.singletonList(OAuthConstants.CLIENT_CREDENTIALS_GRANT));
        return client;
    }

    private static final class TestDynamicRegistrationService extends DynamicRegistrationService {
        void applyClientRegistration(ClientRegistration request, Client client) {
            fromClientRegistrationToClient(request, client);
        }

        Client createClient(ClientRegistration request) {
            return createNewClient(request);
        }
    }
}
