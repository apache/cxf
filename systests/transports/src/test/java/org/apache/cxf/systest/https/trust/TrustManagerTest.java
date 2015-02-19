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

package org.apache.cxf.systest.https.trust;

import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.Bus;
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

/**
 * A set of tests for specifying a TrustManager
 */
public class TrustManagerTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(TrustServer.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(TrustServer.class, true)
        );
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
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT);
        
        TLSClientParameters tlsParams = new TLSClientParameters();
        X509TrustManager trustManager = new NoOpX509TrustManager();
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
    
    // Here the Trust Manager checks the server cert
    @org.junit.Test
    public void testValidServerCertX509TrustManager() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TrustManagerTest.class.getResource("client-trust.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT);
        
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
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        updateAddressPort(port, PORT);
        
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
    
    public static class NoOpX509TrustManager implements X509TrustManager {

        public NoOpX509TrustManager() {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

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
