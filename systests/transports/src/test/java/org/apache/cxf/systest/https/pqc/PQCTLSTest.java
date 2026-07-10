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
package org.apache.cxf.systest.https.pqc;

import java.lang.reflect.Method;
import java.net.URL;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that CXF can complete a TLS 1.3 handshake using the post-quantum
 * hybrid key-encapsulation mechanism X25519MLKEM768 (JEP 527, JDK 27+).
 *
 * <p>The test requires SunJSSE to list {@value #MLKEM_GROUP} as a supported TLS
 * named group.  This is the case only when running under the {@code pqc-tls-test}
 * surefire execution, which overrides the parent-pom's classical-only
 * {@code jdk.tls.namedGroups} setting and requires JDK 27+.  The test is
 * automatically skipped on older JDKs or when the named-group override is absent.
 *
 * <p>The server is configured via {@code pqc-server.xml} (standard
 * {@code httpj:tlsServerParameters}) and the client via {@code pqc-client.xml}.
 * Because {@code jdk.tls.namedGroups} lists {@code X25519MLKEM768} first in the
 * {@code pqc-tls-test} surefire execution, SunJSSE negotiates it by preference
 * without any per-connection named-group override.
 */
public class PQCTLSTest extends AbstractBusClientServerTestBase {

    static final String MLKEM_GROUP = "X25519MLKEM768";

    static final String PORT = allocatePort(PQCServer.class);

    /**
     * True when SunJSSE lists {@value #MLKEM_GROUP} as a supported TLS named
     * group in the current JVM configuration — JDK 27+ (JEP 527) with the right
     * {@code jdk.tls.namedGroups} setting.
     */
    private static final boolean ML_KEM_AVAILABLE = isMlKemTlsSupported();

    // ------------------------------------------------------------------ server

    public static class PQCServer extends AbstractBusTestServerBase {
        @Override
        protected void run() {
            URL busFile = PQCServer.class.getResource("pqc-server.xml");
            Bus busLocal = new SpringBusFactory().createBus(busFile);
            BusFactory.setDefaultBus(busLocal);
            setBus(busLocal);
        }
    }

    // ------------------------------------------------------------------ setup

    @BeforeClass
    public static void startServer() throws Exception {
        Assume.assumeTrue(
            "X25519MLKEM768 TLS not available — requires JDK 27+ with "
            + "jdk.tls.namedGroups including " + MLKEM_GROUP,
            ML_KEM_AVAILABLE);
        assertTrue("Server failed to launch",
            launchServer(PQCServer.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    // ------------------------------------------------------------------ tests

    @Test
    public void testMlKemHandshakeSucceeds() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PQCTLSTest.class.getResource("pqc-client.xml");
        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        QName serviceName =
            new QName("http://apache.org/hello_world/services", "SOAPService");
        URL wsdl = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        assertEquals("Hello Kitty", port.greetMe("Kitty"));

        ((java.io.Closeable) port).close();
        bus.shutdown(true);
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Returns true only when SunJSSE lists {@value #MLKEM_GROUP} in its supported
     * TLS named groups — JDK 27+ (JEP 527) with the right
     * {@code jdk.tls.namedGroups} configuration.
     */
    private static boolean isMlKemTlsSupported() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, null, null);
            Method gm = SSLParameters.class.getMethod("getNamedGroups");
            String[] groups = (String[]) gm.invoke(ctx.getSupportedSSLParameters());
            return groups != null && java.util.Arrays.asList(groups).contains(MLKEM_GROUP);
        } catch (Exception e) {
            return false;
        }
    }
}
