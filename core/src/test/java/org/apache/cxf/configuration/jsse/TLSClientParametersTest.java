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

package org.apache.cxf.configuration.jsse;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.configuration.security.FiltersType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class TLSClientParametersTest {
    private String oldProtocols;
    private String oldIgnoreProtocols;
    
    @Before
    public void setUp() {
        oldProtocols = System.getProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS);
        oldIgnoreProtocols = System.getProperty(TLSClientParameters.IGNORE_CONFIGURED_HTTPS_PROTOCOLS);
    }
    
    @After
    public void tearDown() {
        if (oldProtocols != null) {
            System.setProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS, oldProtocols);
        } else {
            System.clearProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS);
        }

        if (oldIgnoreProtocols != null) {
            System.setProperty(TLSClientParameters.IGNORE_CONFIGURED_HTTPS_PROTOCOLS, oldIgnoreProtocols);
        } else {
            System.clearProperty(TLSClientParameters.IGNORE_CONFIGURED_HTTPS_PROTOCOLS);
        }
    }
    
    @Test
    public void testDefaultHttpsProtocols() {
        assertThat(TLSClientParameters.getPreferredClientProtocols(), arrayContainingInAnyOrder(
            "TLSv1.2", 
            "TLSv1.3"));
    }
    
    @Test
    public void testConfiguredHttpsProtocols() {
        System.setProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS, "SSLv3,");
        assertThat(TLSClientParameters.getPreferredClientProtocols(), arrayContainingInAnyOrder("SSLv3"));
    }
    
    @Test
    public void testIgnoreConfiguredHttpsProtocols() {
        System.setProperty(TLSClientParameters.IGNORE_CONFIGURED_HTTPS_PROTOCOLS, "true");
        System.setProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS, "SSLv3,");
        assertThat(TLSClientParameters.getPreferredClientProtocols(), arrayContainingInAnyOrder(
            "TLSv1.2", 
            "TLSv1.3"));
    }

    @Test
    public void testCopy() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((java.security.KeyStore)null);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        TLSClientParameters params = new TLSClientParameters();
        params.setDisableCNCheck(true);
        params.setSslCacheTimeout(100);
        params.setUseHttpsURLConnectionDefaultSslSocketFactory(true);
        params.setUseHttpsURLConnectionDefaultHostnameVerifier(true);
        params.setHostnameVerifier((hostname, session) -> true);
        params.setServerNames(new ArrayList<>(Arrays.asList("server")));
        params.setKeyManagers(new KeyManager[0]);
        params.setTrustManagers(trustManagers.clone());
        params.setJsseProvider("SunJSSE");
        params.setCipherSuites(new ArrayList<>(Arrays.asList("TLS_AES_128_GCM_SHA256")));
        FiltersType filters = new FiltersType();
        filters.getInclude().add(".*_AES_.*");
        filters.getExclude().add(".*_NULL_.*");
        params.setCipherSuitesFilter(filters);
        params.setCertConstraints(new CertificateConstraintsType());
        params.setSecureRandom(new SecureRandom());
        params.setSecureSocketProtocol("TLSv1.3");
        params.setCertAlias("alias");
        params.setNamedGroups(new ArrayList<>(Arrays.asList("x25519")));

        TLSClientParameters copy = params.copy();
        assertEquals(params, copy);
        assertEquals(params.hashCode(), copy.hashCode());
        assertEquals(100, copy.getSslCacheTimeout());
        assertSame(params.getHostnameVerifier(), copy.getHostnameVerifier());
        assertArrayEquals(params.getTrustManagers(), copy.getTrustManagers());
        assertSame(params.getCertConstraints(), copy.getCertConstraints());
        assertSame(params.getSecureRandom(), copy.getSecureRandom());
        assertEquals("SunJSSE", copy.getJsseProvider());
        assertEquals("alias", copy.getCertAlias());

        // Later changes to the parameters, made in place, do not affect the copy
        params.getServerNames().add("other");
        params.getTrustManagers()[0] = null;
        params.getCipherSuites().add("TLS_AES_256_GCM_SHA384");
        params.getCipherSuitesFilter().getInclude().add(".*");
        params.getNamedGroups().add("secp256r1");
        assertEquals(Arrays.asList("server"), copy.getServerNames());
        assertArrayEquals(trustManagers, copy.getTrustManagers());
        assertEquals(Arrays.asList("TLS_AES_128_GCM_SHA256"), copy.getCipherSuites());
        assertEquals(Arrays.asList(".*_AES_.*"), copy.getCipherSuitesFilter().getInclude());
        assertEquals(Arrays.asList(".*_NULL_.*"), copy.getCipherSuitesFilter().getExclude());
        assertEquals(Arrays.asList("x25519"), copy.getNamedGroups());
        assertNotEquals(params, copy);
    }

    @Test
    public void testCopyOfDefaults() {
        TLSClientParameters params = new TLSClientParameters();
        TLSClientParameters copy = params.copy();
        assertEquals(params, copy);
        assertNull(copy.getServerNames());
        assertNull(copy.getTrustManagers());
        assertNull(copy.getCipherSuitesFilter());
    }
}
