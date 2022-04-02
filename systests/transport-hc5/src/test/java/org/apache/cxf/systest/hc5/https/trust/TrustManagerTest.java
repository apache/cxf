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

package org.apache.cxf.systest.hc5.https.trust;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.InsecureTrustManager;
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
 * A set of tests for specifying a TrustManager
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class TrustManagerTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(TrustServer.class);
    static final String PORT2 = allocatePort(TrustServer.class, 2);
    static final String PORT3 = allocatePort(TrustServer.class, 3);

    private final Boolean async;

    public TrustManagerTest(Boolean async) {
        this.async = async;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(TrustServer.class, true)
        );
        assertTrue(
             "Server failed to launch",
             // run the server in the same process
             // set this to false to fork
             launchServer(TrustServerNoSpring.class, true)
        );
    }

    @Parameters(name = "{0}")
    public static Collection<Boolean> data() {

        return Arrays.asList(new Boolean[] {Boolean.FALSE, Boolean.TRUE});
    }

    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    // The X509TrustManager is effectively empty here so trust verification should work
    @org.junit.Test
    public void testNoOpX509TrustManager() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TrustManagerTest.class.getResource("client-trust.xml");

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

        TLSClientParameters tlsParams = new TLSClientParameters();
        tlsParams.setTrustManagers(InsecureTrustManager.getNoOpX509TrustManagers());
        tlsParams.setDisableCNCheck(true);

        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // The X509TrustManager is effectively empty here so trust verification should work
    @org.junit.Test
    public void testNoOpX509TrustManagerTrustManagersRef() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TrustManagerTest.class.getResource("client-trust-manager-ref.xml");

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

    // Here the Trust Manager checks the server cert
    @org.junit.Test
    public void testValidServerCertX509TrustManager() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TrustManagerTest.class.getResource("client-trust.xml");

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

        String validPrincipalName = "CN=Bethal,OU=Bethal,O=ApacheTest,L=Syracuse,C=US";

        TLSClientParameters tlsParams = new TLSClientParameters();
        X509TrustManager trustManager =
            new ServerCertX509TrustManager(validPrincipalName);
        TrustManager[] trustManagers = new TrustManager[1];
        trustManagers[0] = trustManager;
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Here we're using spring config but getting the truststore from the standard system properties
    @org.junit.Test
    public void testSystemPropertiesWithEmptyTLSClientParametersConfig() throws Exception {
        try {
            System.setProperty("javax.net.ssl.trustStore", "keys/Bethal.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = TrustManagerTest.class.getResource("client-trust-config.xml");

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
            System.clearProperty("javax.net.ssl.trustStore");
            System.clearProperty("javax.net.ssl.trustStorePassword");
            System.clearProperty("javax.net.ssl.trustStoreType");
        }
    }

    // Here we're using spring config but getting the truststore from the standard system properties
    @org.junit.Test
    public void testSystemPropertiesWithEmptyKeystoreConfig() throws Exception {
        try {
            System.setProperty("javax.net.ssl.trustStore", "keys/Bethal.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = TrustManagerTest.class.getResource("client-trust-empty-config.xml");

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
            System.clearProperty("javax.net.ssl.trustStore");
            System.clearProperty("javax.net.ssl.trustStorePassword");
            System.clearProperty("javax.net.ssl.trustStoreType");
        }
    }

    // Here the Trust Manager checks the server cert. this time we are invoking on the
    // service that is configured in code (not by spring)
    @org.junit.Test
    public void testValidServerCertX509TrustManager2() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TrustManagerTest.class.getResource("client-trust.xml");

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

        String validPrincipalName = "CN=Bethal,OU=Bethal,O=ApacheTest,L=Syracuse,C=US";

        TLSClientParameters tlsParams = new TLSClientParameters();
        X509TrustManager trustManager =
            new ServerCertX509TrustManager(validPrincipalName);
        TrustManager[] trustManagers = new TrustManager[1];
        trustManagers[0] = trustManager;
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testInvalidServerCertX509TrustManager() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TrustManagerTest.class.getResource("client-trust.xml");

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

        String invalidPrincipalName = "CN=Bethal2,OU=Bethal,O=ApacheTest,L=Syracuse,C=US";

        TLSClientParameters tlsParams = new TLSClientParameters();
        X509TrustManager trustManager =
            new ServerCertX509TrustManager(invalidPrincipalName);
        TrustManager[] trustManagers = new TrustManager[1];
        trustManagers[0] = trustManager;
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        try {
            port.greetMe("Kitty");
            fail("Failure expected on an invalid principal name");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testOSCPOverride() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TrustManagerTest.class.getResource("client-trust.xml");

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

        // Read truststore
        KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream trustStore =
            ClassLoaderUtils.getResourceAsStream("keys/cxfca.jks", TrustManagerTest.class)) {
            ts.load(trustStore, "password".toCharArray());
        }

        try {
            Security.setProperty("ocsp.enable", "true");

            PKIXBuilderParameters param = new PKIXBuilderParameters(ts, new X509CertSelector());
            param.setRevocationEnabled(true);

            TrustManagerFactory tmf  =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(new CertPathTrustManagerParameters(param));

            TLSClientParameters tlsParams = new TLSClientParameters();
            tlsParams.setTrustManagers(tmf.getTrustManagers());
            tlsParams.setDisableCNCheck(true);

            Client client = ClientProxy.getClient(port);
            HTTPConduit http = (HTTPConduit) client.getConduit();
            http.setTlsClientParameters(tlsParams);

            try {
                port.greetMe("Kitty");
                fail("Failure expected on an invalid OCSP responder URL");
            } catch (Exception ex) {
                // expected
            }

        } finally {
            Security.setProperty("ocsp.enable", "false");
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    public static class ServerCertX509TrustManager implements X509TrustManager {

        private String requiredServerPrincipalName;

        public ServerCertX509TrustManager(String principalName) {
            requiredServerPrincipalName = principalName;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException("X509 Certificate chain is empty");
            }
            X509Certificate serverCert = chain[0];
            if (requiredServerPrincipalName != null
                && !requiredServerPrincipalName.equals(serverCert.getSubjectX500Principal().getName())) {
                throw new CertificateException("X509 server certificate does not match requirement");
            }

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

    }

}
