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

package org.apache.cxf.transport.https;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.transport.https.SSLUtils.SSLEngineWrapper;
import org.apache.cxf.transport.https.SSLUtils.X509TrustManagerWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SSLUtilsTest {

    private SSLEngine engine;

    @Test
    public void testProtocolConstraints() throws Exception {
        String[] protocols = SSLUtils.getProtocolsToInclude(
            Arrays.asList("TLSv1.3", "TLSv1"),
            Collections.singletonList("TLSv1.3"),
            new String[] {"TLSv1.3", "TLSv1.2"},
            new String[] {"TLSv1", "TLSv1.2", "TLSv1.3"});

        assertThat(protocols, is(new String[] {"TLSv1"}));
    }

    @Test(expected = GeneralSecurityException.class)
    public void testProtocolConstraintsRejectEmptyResult() throws Exception {
        SSLUtils.getProtocolsToInclude(
            Collections.singletonList("TLSv1.3"),
            Collections.emptyList(),
            new String[] {"TLSv1.2"},
            new String[] {"TLSv1.2"});
    }

    @Test
    public void testProtocolConstraintsUseConfiguredContext() throws Exception {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(null, null, null);

        String[] protocols = SSLUtils.getProtocolsToInclude(
            Collections.emptyList(),
            Collections.emptyList(),
            context.getDefaultSSLParameters().getProtocols(),
            context.getSupportedSSLParameters().getProtocols());

        assertThat(protocols, is(new String[] {"TLSv1.2"}));
    }
    
    @Before
    public void setUp() throws NoSuchAlgorithmException {
        engine = SSLContext.getDefault().createSSLEngine();
    }
    
    @After
    public void tearDown() throws Exception {
        engine.closeInbound();
        engine.closeOutbound();
        engine = null;
    }

    @Test
    public void testCXF9065() throws NoSuchAlgorithmException, InterruptedException {
        SSLEngineWrapper wrapper = new SSLEngineWrapper(engine);

        for (int i = 0; i < 15000; ++i) {
            wrapper = new SSLEngineWrapper(wrapper);
        }
    
        assertThat(wrapper.getSSLParameters(), is(not(nullValue())));
    }

    @Test
    public void testCreateClientSSLEngineBoundToPeer() throws Exception {
        TLSClientParameters parameters = new TLSClientParameters();
        SSLEngine clientEngine = SSLUtils.createClientSSLEngine(parameters, "api.example.com", 443);

        assertThat(clientEngine.getPeerHost(), is("api.example.com"));
        assertThat(clientEngine.getPeerPort(), is(443));
        assertThat(clientEngine.getUseClientMode(), is(true));
    }

    @Test
    public void testHostnameVerifierWrapsConfiguredTrustManager() throws Exception {
        X509TrustManager trustManager = mock(X509TrustManager.class);
        HostnameVerifier verifier = mock(HostnameVerifier.class);
        TLSClientParameters parameters = new TLSClientParameters();
        parameters.setTrustManagers(new X509TrustManager[] {trustManager});
        parameters.setHostnameVerifier(verifier);

        var initParameters = SSLUtils.getSSLContextInitParameters(parameters, true);

        assertThat(initParameters.getTrustManagers()[0] instanceof X509TrustManagerWrapper, is(true));
        assertThat(parameters.getTrustManagers()[0], is(trustManager));
    }

    /**
     * Regression test: a plain (non-extended) X509TrustManager delegate must still get
     * hostname verification. JSSE endpoint identification is suppressed by
     * SSLEngineWrapper, so skipping the CXF HostnameVerifier for plain delegates left
     * no hostname check at all - any certificate the trust manager accepted (e.g. any
     * cert from a pinned corporate CA, issued for any host) enabled silent MITM.
     */
    @Test
    public void testPlainTrustManagerStillGetsHostnameVerification() throws Exception {
        X509TrustManager plainTrustManager = mock(X509TrustManager.class);
        HostnameVerifier failingVerifier = mock(HostnameVerifier.class);
        when(failingVerifier.verify(anyString(), any())).thenReturn(false);

        ExtendedSSLSession session = mock(ExtendedSSLSession.class);
        when(session.getPeerHost()).thenReturn("evil.example.net");
        when(session.getRequestedServerNames()).thenReturn(Collections.emptyList());
        SSLEngine mockEngine = mock(SSLEngine.class);
        when(mockEngine.getHandshakeSession()).thenReturn(session);

        X509TrustManagerWrapper wrapper =
            new X509TrustManagerWrapper(plainTrustManager, failingVerifier);
        X509Certificate[] chain = new X509Certificate[0];
        try {
            wrapper.checkServerTrusted(chain, "RSA", mockEngine);
            fail("hostname verification must run for plain X509TrustManager delegates");
        } catch (CertificateException expected) {
            // expected: no name matching evil.example.net
        }
        // the delegate's chain validation was still consulted
        verify(plainTrustManager).checkServerTrusted(chain, "RSA");
    }

    @Test
    public void testPlainTrustManagerAcceptedWhenHostnameMatches() throws Exception {
        X509TrustManager plainTrustManager = mock(X509TrustManager.class);
        HostnameVerifier passingVerifier = mock(HostnameVerifier.class);
        when(passingVerifier.verify(anyString(), any())).thenReturn(true);

        ExtendedSSLSession session = mock(ExtendedSSLSession.class);
        when(session.getPeerHost()).thenReturn("service.example.com");
        when(session.getRequestedServerNames()).thenReturn(Collections.emptyList());
        SSLEngine mockEngine = mock(SSLEngine.class);
        when(mockEngine.getHandshakeSession()).thenReturn(session);

        X509TrustManagerWrapper wrapper =
            new X509TrustManagerWrapper(plainTrustManager, passingVerifier);
        wrapper.checkServerTrusted(new X509Certificate[0], "RSA", mockEngine);
        verify(passingVerifier).verify(eq("service.example.com"), any());
    }

    @Test
    public void testServerEngineHonorsConfiguredConstraints() throws Exception {
        TLSServerParameters parameters = new TLSServerParameters();
        parameters.setExcludeProtocols(Arrays.asList("SSLv3", "TLSv1", "TLSv1.1"));
        ClientAuthentication clientAuth = new ClientAuthentication();
        clientAuth.setWant(Boolean.TRUE);
        parameters.setClientAuthentication(clientAuth);

        SSLEngine serverEngine = SSLUtils.createServerSSLEngine(parameters);

        assertThat(serverEngine.getUseClientMode(), is(false));
        assertThat(serverEngine.getWantClientAuth(), is(true));
        List<String> enabled = Arrays.asList(serverEngine.getEnabledProtocols());
        assertThat(enabled.contains("SSLv3"), is(false));
        assertThat(enabled.contains("TLSv1"), is(false));
        assertThat(enabled.contains("TLSv1.1"), is(false));
    }
}
