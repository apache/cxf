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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.jsse.TLSServerParameters;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * The configured TLS named groups reach the SSLEngine / SSLSocket that CXF creates (SunJSSE, JDK 20+).
 */
public class SSLUtilsNamedGroupsTest {
    private static final List<String> GROUPS = Arrays.asList("secp384r1", "x25519");

    @BeforeClass
    public static void requireNamedGroupsApi() {
        Assume.assumeTrue("SSLParameters.setNamedGroups() requires JDK 20+", hasNamedGroupsApi());
    }

    private static boolean hasNamedGroupsApi() {
        try {
            SSLParameters.class.getMethod("setNamedGroups", String[].class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Test
    public void testApplyToEngine() throws Exception {
        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
        SSLUtils.applyNamedGroups(engine, GROUPS);
        assertArrayEquals(GROUPS.toArray(), SSLUtils.getNamedGroups(engine.getSSLParameters()));
    }

    @Test
    public void testNothingConfiguredLeavesEngineUnchanged() throws Exception {
        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
        String[] before = SSLUtils.getNamedGroups(engine.getSSLParameters());
        SSLUtils.applyNamedGroups(engine, null);
        SSLUtils.applyNamedGroups(engine, List.of());
        assertArrayEquals(before, SSLUtils.getNamedGroups(engine.getSSLParameters()));
    }

    @Test
    public void testApplyToSocket() throws Exception {
        try (SSLSocket socket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket()) {
            SSLUtils.applyNamedGroups(socket, GROUPS);
            assertArrayEquals(GROUPS.toArray(), SSLUtils.getNamedGroups(socket.getSSLParameters()));
        }
    }

    @Test
    public void testServerEngine() throws Exception {
        TLSServerParameters params = new TLSServerParameters();
        params.setNamedGroups(GROUPS);
        SSLEngine engine = SSLUtils.createServerSSLEngine(params);
        assertArrayEquals(GROUPS.toArray(), SSLUtils.getNamedGroups(engine.getSSLParameters()));
    }

    @Test
    public void testClientEngine() throws Exception {
        TLSClientParameters params = new TLSClientParameters();
        params.setNamedGroups(GROUPS);
        SSLEngine engine = SSLUtils.createClientSSLEngine(params, "localhost", 443);
        assertArrayEquals(GROUPS.toArray(), SSLUtils.getNamedGroups(engine.getSSLParameters()));
    }

    @Test
    public void testSocketFactoryWrapper() throws Exception {
        SSLSocketFactoryWrapper factory = new SSLSocketFactoryWrapper(
            SSLContext.getDefault().getSocketFactory(), null, null, GROUPS);
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket plain = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
             SSLSocket socket = (SSLSocket) factory.createSocket(plain, "localhost", server.getLocalPort(), true)) {
            assertArrayEquals(GROUPS.toArray(), SSLUtils.getNamedGroups(socket.getSSLParameters()));
        }
    }
}
