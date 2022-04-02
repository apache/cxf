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

package org.apache.cxf.systest.hc5.https.hostname;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test for hostname verification when the Java system property "java.protocol.handler.pkgs" is set to
 * "com.sun.net.ssl.internal.www.protocol". This means that com.sun.net.ssl.HostnameVerifier is used
 * instead of the javax version.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class HostnameVerificationDeprecatedTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(HostnameVerificationDeprecatedServer.class);
    static final String PORT2 = allocatePort(HostnameVerificationDeprecatedServer.class, 2);
    static final String PORT3 = allocatePort(HostnameVerificationDeprecatedServer.class, 3);

    private final Boolean async;

    public HostnameVerificationDeprecatedTest(Boolean async) {
        this.async = async;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(HostnameVerificationDeprecatedServer.class, true)
        );
    }

    @Parameters(name = "{0}")
    public static Collection<Boolean> data() {

        return Arrays.asList(new Boolean[] {Boolean.FALSE, Boolean.TRUE});
    }

    @AfterClass
    public static void cleanup() throws Exception {
        System.clearProperty("java.protocol.handler.pkgs");
        stopAllServers();
    }

    // Here we expect an exception, as the default hostname verifier contributed by CXF will object to the
    // fact that the server certificate does not have "CN=localhost".
    @org.junit.Test
    public void testLocalhostNotMatching() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationDeprecatedTest.class.getResource("hostname-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        // Enable Async
        if (async) {
            ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        }

        try {
            port.greetMe("Kitty");
            fail("Failure expected on the hostname verification");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // No Subject Alternative Name, no matching CN - but we are disabling the CN check so it should work OK
    @org.junit.Test
    public void testLocalhostNotMatchingDisableCN() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationTest.class.getResource("hostname-client-disablecn.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        // Enable Async
        if (async) {
            ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        }

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // No Subject Alternative Name, no matching CN - but we are setting the JVM default hostname verifier to
    // allow it
    @org.junit.Test
    public void testNoSubjectAlternativeNameNoCNMatchDefaultVerifier() throws Exception {
        HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostName, javax.net.ssl.SSLSession session) {
                        return true;
                    }

                    // Note we need this method as well or else it won't work the with the
                    // deprecated HostnameVerifier interface
                    @SuppressWarnings("unused")
                    public boolean verify(final String host, final String certHostname) {
                        return true;
                    }
                });

            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = HostnameVerificationTest.class.getResource("hostname-client-usedefault.xml");

            Bus bus = bf.createBus(busFile.toString());
            BusFactory.setDefaultBus(bus);
            BusFactory.setThreadDefaultBus(bus);

            URL url = SOAPService.WSDL_LOCATION;
            SOAPService service = new SOAPService(url, SOAPService.SERVICE);
            assertNotNull("Service is null", service);
            final Greeter port = service.getHttpsPort();
            assertNotNull("Port is null", port);

            updateAddressPort(port, PORT);

            // Enable Async
            if (async) {
                ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
            }

            assertEquals(port.greetMe("Kitty"), "Hello Kitty");

            ((java.io.Closeable)port).close();
            bus.shutdown(true);
        } finally {
            if (hostnameVerifier != null) {
                HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
            }
        }
    }

    // No Subject Alternative Name, no matching CN - but we are setting the JVM default hostname verifier to
    // allow it. It differs to the method above, that we are not using a Spring configuration file, but
    // instead are setting a TLSClientParameters on the HTTPConduit
    @org.junit.Test
    public void testNoSubjectAlternativeNameNoCNMatchDefaultVerifierNoConfig() throws Exception {
        HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        try {
            System.setProperty("javax.net.ssl.trustStore", "keys/subjalt.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "security");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostName, javax.net.ssl.SSLSession session) {
                        return true;
                    }

                    // Note we need this method as well or else it won't work the with the
                    // deprecated HostnameVerifier interface
                    @SuppressWarnings("unused")
                    public boolean verify(final String host, final String certHostname) {
                        return true;
                    }
                });

            URL url = SOAPService.WSDL_LOCATION;
            SOAPService service = new SOAPService(url, SOAPService.SERVICE);
            assertNotNull("Service is null", service);
            final Greeter port = service.getHttpsPort();
            assertNotNull("Port is null", port);

            updateAddressPort(port, PORT);

            // Enable Async
            if (async) {
                ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
            }

            TLSClientParameters clientParameters = new TLSClientParameters();
            clientParameters.setUseHttpsURLConnectionDefaultHostnameVerifier(true);
            Client client = ClientProxy.getClient(port);
            ((HTTPConduit)client.getConduit()).setTlsClientParameters(clientParameters);

            assertEquals(port.greetMe("Kitty"), "Hello Kitty");

            ((java.io.Closeable)port).close();
        } finally {
            if (hostnameVerifier != null) {
                HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
            }
            System.clearProperty("javax.net.ssl.trustStore");
            System.clearProperty("javax.net.ssl.trustStorePassword");
            System.clearProperty("javax.net.ssl.trustStoreType");
        }
    }

    // No Subject Alternative Name, but the CN matches ("localhost"), so the default HostnameVerifier
    // should work fine
    @org.junit.Test
    public void testNoSubjectAlternativeNameCNMatch() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationDeprecatedTest.class.getResource("hostname-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT2);

        // Enable Async
        if (async) {
            ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        }

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // No Subject Alternative Name, but the CN wildcard matches
    @org.junit.Test
    public void testNoSubjectAlternativeNameCNWildcardMatch() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HostnameVerificationTest.class.getResource("hostname-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT3);

        // Enable Async
        if (async) {
            ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        }

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

}
