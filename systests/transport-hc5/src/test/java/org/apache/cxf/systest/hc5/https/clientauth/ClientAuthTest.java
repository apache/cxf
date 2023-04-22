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

package org.apache.cxf.systest.hc5.https.clientauth;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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
 * A set of tests for TLS client authentication.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class ClientAuthTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ClientAuthServer.class);
    static final String PORT2 = allocatePort(ClientAuthServer.class, 2);

    final Boolean async;

    public ClientAuthTest(Boolean async) {
        this.async = async;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(ClientAuthServer.class, true)
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

    // Server directly trusts the client cert
    @org.junit.Test
    public void testDirectTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth.xml");

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

    // Server does not (directly) trust the client cert
    @org.junit.Test
    public void testInvalidDirectTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-invalid.xml");

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
            fail("Failure expected on an untrusted cert");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client does not specify a KeyStore, only a TrustStore
    @org.junit.Test
    public void testNoClientCert() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-no-auth.xml");

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
            fail("Failure expected on no trusted cert");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Ignoring this test as it fails when run as part of the test class - testNoClientCert interferes with it
    // It succeeds when run with testNoClientCert commented out
    @org.junit.Test
    @org.junit.Ignore
    public void testSystemPropertiesWithEmptyKeystoreConfig() throws Exception {
        try {
            System.setProperty("javax.net.ssl.keyStore", "keys/Morpit.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            System.setProperty("javax.net.ssl.keyPassword", "password");
            System.setProperty("javax.net.ssl.keyStoreType", "JKS");
            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = ClientAuthTest.class.getResource("client-no-auth.xml");

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
        }  finally {
            System.clearProperty("javax.net.ssl.keyStore");
            System.clearProperty("javax.net.ssl.keyStorePassword");
            System.clearProperty("javax.net.ssl.keyPassword");
            System.clearProperty("javax.net.ssl.keyStoreType");
        }
    }

    // Server trusts the issuer of the client cert
    @org.junit.Test
    public void testChainTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-chain.xml");

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

    // Server does not trust the issuer of the client cert
    @org.junit.Test
    public void testInvalidChainTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-invalid2.xml");

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

        try {
            port.greetMe("Kitty");
            fail("Failure expected on no trusted cert");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client does not trust the issuer of the server cert
    @org.junit.Test
    public void testClientInvalidCertChain() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-invalid2.xml");

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
            fail("Failure expected on no trusted cert");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Client does not directly trust the server cert
    @org.junit.Test
    public void testClientInvalidDirectTrust() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ClientAuthTest.class.getResource("client-auth-invalid.xml");

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

        try {
            port.greetMe("Kitty");
            fail("Failure expected on no trusted cert");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSSLConnectionUsingJavaAPIs() throws Exception {
        URL service = new URL("https://localhost:" + PORT);
        HttpsURLConnection connection = (HttpsURLConnection) service.openConnection();

        connection.setHostnameVerifier(new DisableCNCheckVerifier());

        SSLContext sslContext = SSLContext.getInstance("TLS");

        KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream trustStore =
            ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", ClientAuthTest.class)) {
            ts.load(trustStore, "password".toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream keyStore =
            ClassLoaderUtils.getResourceAsStream("keys/Morpit.jks", ClientAuthTest.class)) {
            ks.load(keyStore, "password".toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());

        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

        connection.setSSLSocketFactory(sslContext.getSocketFactory());

        connection.connect();

        connection.disconnect();
    }

    // https://issues.apache.org/jira/browse/CXF-7763
    @org.junit.Test
    public void testCheckKeyManagersWithCertAlias() throws Exception {
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);

        // Set up (shared) KeyManagers/TrustManagers
        TrustManager[] trustManagers = InsecureTrustManager.getNoOpX509TrustManagers();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        try (InputStream inputStream = ClassLoaderUtils.getResourceAsStream("keymanagers.jks", this.getClass())) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(inputStream, "password".toCharArray());

            kmf.init(keyStore, "password".toCharArray());
        }
        KeyManager[] keyManagers = kmf.getKeyManagers();

        // First call to PORT using Morpit
        TLSClientParameters tlsParams = new TLSClientParameters();
        tlsParams.setKeyManagers(keyManagers);
        tlsParams.setCertAlias("morpit");
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT);

        // Enable Async
        if (async) {
            ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        }

        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        ((java.io.Closeable)port).close();

        // Second call to PORT2 using "alice"
        tlsParams = new TLSClientParameters();
        tlsParams.setKeyManagers(keyManagers);
        tlsParams.setCertAlias("alice");
        tlsParams.setTrustManagers(trustManagers);
        tlsParams.setDisableCNCheck(true);

        port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        updateAddressPort(port, PORT2);

        // Enable Async
        if (async) {
            ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        }

        client = ClientProxy.getClient(port);
        http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        ((java.io.Closeable)port).close();
    }

    // Server directly trusts the client cert
    @org.junit.Test
    public void testDirectTrustUsingKeyManagers() throws Exception {

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

        // Set up KeyManagers/TrustManagers
        KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream trustStore =
            ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", ClientAuthTest.class)) {
            ts.load(trustStore, "password".toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream keyStore =
            ClassLoaderUtils.getResourceAsStream("keys/Morpit.jks", ClientAuthTest.class)) {
            ks.load(keyStore, "password".toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());

        TLSClientParameters tlsParams = new TLSClientParameters();
        tlsParams.setKeyManagers(kmf.getKeyManagers());
        tlsParams.setTrustManagers(tmf.getTrustManagers());
        tlsParams.setDisableCNCheck(true);

        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
    }

    // Server directly trusts the client cert
    @org.junit.Test
    public void testDirectTrustUsingSSLContext() throws Exception {

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

        // Set up KeyManagers/TrustManagers
        KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream trustStore =
            ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", ClientAuthTest.class)) {
            ts.load(trustStore, "password".toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream keyStore =
            ClassLoaderUtils.getResourceAsStream("keys/Morpit.jks", ClientAuthTest.class)) {
            ks.load(keyStore, "password".toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

        TLSClientParameters tlsParams = new TLSClientParameters();
        tlsParams.setSslContext(sslContext);
        tlsParams.setDisableCNCheck(true);

        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);

        assertEquals(port.greetMe("Kitty"), "Hello Kitty");

        ((java.io.Closeable)port).close();
    }

    private static final class DisableCNCheckVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String arg0, SSLSession arg1) {
            return true;
        }

    };

}
