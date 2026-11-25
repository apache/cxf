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

package org.apache.cxf.transport.http.asyncclient;

import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.https.SSLUtils;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * The TLS named groups of the conduit's TLS client parameters are applied to its SSLEngine.
 */
public class AsyncHTTPConduitNamedGroupsTest {
    private static final List<String> GROUPS = Arrays.asList("secp384r1", "x25519");
    private Bus bus;
    private AsyncHTTPConduitFactory factory;

    @Before
    public void setUp() {
        bus = new ExtensionManagerBus();
        factory = new AsyncHTTPConduitFactory(bus);
    }

    @After
    public void tearDown() {
        factory.shutdown();
        bus.shutdown(true);
    }

    @Test
    public void testNamedGroupsAreApplied() throws Exception {
        Assume.assumeTrue("SSLParameters.setNamedGroups() requires JDK 20+", hasNamedGroupsApi());
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("https://localhost/test");
        AsyncHTTPConduit conduit = new AsyncHTTPConduit(bus, ei, null, factory);
        TLSClientParameters tls = new TLSClientParameters();
        tls.setNamedGroups(GROUPS);
        conduit.setTlsClientParameters(tls);

        SSLContext context = SSLContext.getDefault();
        SSLEngine engine = context.createSSLEngine("localhost", 443);
        engine.setUseClientMode(true);
        conduit.initializeSSLEngine(context, engine);
        assertArrayEquals(GROUPS.toArray(), SSLUtils.getNamedGroups(engine.getSSLParameters()));
        conduit.close();
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
