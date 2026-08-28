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

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509TrustManager;

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
}
