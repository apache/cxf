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

package org.apache.cxf.https.ssl3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * A set of tests SSL v3 protocol support. It should be disallowed by default on both the
 * (Jetty) server and CXF client side.
 */
public class SSLv3Test extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(SSLv3Server.class);
    static final String PORT2 = allocatePort(SSLv3Server.class, 2);
    static final String PORT3 = allocatePort(SSLv3Server.class, 3);
    static final String PORT4 = allocatePort(SSLv3Server.class, 4);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(SSLv3Server.class, true)
        );
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testSSLv3ServerNotAllowedByDefault() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SSLv3Test.class.getResource("sslv3-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        System.setProperty("https.protocols", "SSLv3");

        URL service = new URL("https://localhost:" + PORT);
        HttpsURLConnection connection = (HttpsURLConnection) service.openConnection();
        
        connection.setHostnameVerifier(new DisableCNCheckVerifier());
        
        SSLContext sslContext = SSLContext.getInstance("SSL");
        
        KeyStore trustedCertStore = KeyStore.getInstance("jks");
        try (InputStream keystore = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", SSLv3Test.class)) {
            trustedCertStore.load(keystore, null);
        }
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(trustedCertStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        
        sslContext.init(null, trustManagers, new java.security.SecureRandom());
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        
        try {
            connection.connect();
            fail("Failure expected on an SSLv3 connection attempt");
        } catch (IOException ex) {
            // expected
        }
        
        System.clearProperty("https.protocols");
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSSLv3ServerAllowed() throws Exception {
        
        // Doesn't work with IBM JDK 
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SSLv3Test.class.getResource("sslv3-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        System.setProperty("https.protocols", "SSLv3");

        URL service = new URL("https://localhost:" + PORT2);
        HttpsURLConnection connection = (HttpsURLConnection) service.openConnection();
        
        connection.setHostnameVerifier(new DisableCNCheckVerifier());
        
        SSLContext sslContext = SSLContext.getInstance("SSL");
        KeyStore trustedCertStore = KeyStore.getInstance("jks");
        try (InputStream keystore = ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", SSLv3Test.class)) {
            trustedCertStore.load(keystore, null);
        }
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(trustedCertStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        
        sslContext.init(null, trustManagers, new java.security.SecureRandom());
        
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        
        connection.connect();
        
        connection.disconnect();
        
        System.clearProperty("https.protocols");
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testClientSSL3NotAllowed() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SSLv3Test.class.getResource("sslv3-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT3);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on the client not supporting SSLv3 by default");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsyncClientSSL3NotAllowed() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SSLv3Test.class.getResource("sslv3-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        updateAddressPort(port, PORT3);
        
        try {
            port.greetMe("Kitty");
            fail("Failure expected on the client not supporting SSLv3 by default");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testClientSSL3Allowed() throws Exception {
        // Doesn't work with IBM JDK 
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SSLv3Test.class.getResource("sslv3-client-allow.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT3);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsyncClientSSL3Allowed() throws Exception {
        // Doesn't work with IBM JDK 
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SSLv3Test.class.getResource("sslv3-client-allow.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        // Enable Async
        ((BindingProvider)port).getRequestContext().put("use.async.http.conduit", true);
        
        updateAddressPort(port, PORT3);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
 
    @org.junit.Test
    public void testTLSClientToEndpointWithSSL3Allowed() throws Exception {
        // Doesn't work with IBM JDK 
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SSLv3Test.class.getResource("sslv3-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT4);
        
        port.greetMe("Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSSL3ClientToEndpointWithSSL3Allowed() throws Exception {
        // Doesn't work with IBM JDK 
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SSLv3Test.class.getResource("sslv3-client-allow.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT4);
        
        port.greetMe("Kitty");
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    private static final class DisableCNCheckVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String arg0, SSLSession arg1) {
            return true;
        }
        
    };
}
