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

package org.apache.cxf.systest.http_undertow.http2;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.https.SSLUtils;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The TLS named groups configured for an Undertow server endpoint are used in the handshake. The test JVM
 * offers both secp256r1 and secp384r1 (jdk.tls.namedGroups); the endpoint accepts only secp384r1.
 */
public class UndertowNamedGroupsTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        Assume.assumeTrue("SSLParameters.setNamedGroups() requires JDK 20+", hasNamedGroupsApi());
        assertTrue("Server failed to launch", launchServer(NamedGroupsBookServer.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @Test
    public void testServerAcceptsOnlyConfiguredGroups() throws Exception {
        int port = Integer.parseInt(NamedGroupsBookServer.PORT);
        assertTrue(handshake(port, "secp384r1"));
        assertFalse(handshake(port, "secp256r1"));
    }

    private static boolean handshake(int port, String group) throws Exception {
        KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream is = UndertowNamedGroupsTest.class.getClassLoader()
            .getResourceAsStream("keys/Truststore.jks")) {
            ts.load(is, "password".toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        try (SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket("localhost", port)) {
            socket.setSoTimeout(10000);
            SSLUtils.applyNamedGroups(socket, List.of(group));
            socket.startHandshake();
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static boolean hasNamedGroupsApi() {
        try {
            SSLParameters.class.getMethod("setNamedGroups", String[].class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
