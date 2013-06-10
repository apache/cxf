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

package org.apache.cxf.systest.ws.x509;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.ut.SecurityHeaderCacheInterceptor;
import org.apache.cxf.systest.ws.x509.server.StaxServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;
import org.example.contract.doubleit.DoubleItPortType2;
import org.junit.BeforeClass;

/**
 * A set of tests for X.509 Tokens using the streaming interceptors. 
 * It tests both DOM + StAX clients against the StAX server
 */
public class StaxX509TokenTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(StaxServer.class);
    static final String PORT2 = allocatePort(StaxServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(StaxServer.class, true)
        );
        /*
        assertTrue(
                "Intermediary failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Intermediary.class, true)
        );
        */
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    /*
    // TODO
    @org.junit.Test
    public void testKeyIdentifier() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testKeyIdentifierJaxwsClient() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/jaxws-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                "org/apache/cxf/systest/ws/wssec10/client/bob.properties");
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIntermediary() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/intermediary-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItIntermediary.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, INTERMEDIARY_PORT);
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssuerSerial() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItIssuerSerialPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testThumbprint() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItThumbprintPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testContentEncryptedElements() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItContentEncryptedElementsPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    */
    
    @org.junit.Test
    public void testAsymmetricIssuerSerial() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricIssuerSerialPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricThumbprint() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricThumbprintPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryptBeforeSigning() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptBeforeSigningPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryptSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        // TODO See WSS-450
        // SecurityTestUtil.enableStreaming(x509Port);
        // x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricProtectTokens() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricProtectTokensPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricUsernameTokenPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    /*
    @org.junit.Test
    public void testSymmetricProtectTokens() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricProtectTokensPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    */
    
    @org.junit.Test
    public void testTransportEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportEndorsingSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingSP11Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSignedEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSignedEndorsingPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    // TODO See WSS-443
    @org.junit.Test
    @org.junit.Ignore
    public void testTransportEndorsingEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingEncryptedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    // TODO See WSS-443
    @org.junit.Test
    @org.junit.Ignore
    public void testTransportSignedEndorsingEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSignedEndorsingEncryptedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignatureSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignatureSP11Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryption() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptionPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignatureReplay() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        Client cxfClient = ClientProxy.getClient(x509Port);
        SecurityHeaderCacheInterceptor cacheInterceptor =
            new SecurityHeaderCacheInterceptor();
        cxfClient.getOutInterceptors().add(cacheInterceptor);
        
        // Make two invocations with the same security header
        x509Port.doubleIt(25);
        try {
            x509Port.doubleIt(25);
            fail("Failure expected on a replayed Timestamp");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "A replay attack has been detected";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSupportingSigned() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingSignedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    // TODO WSS-438
    @org.junit.Test
    @org.junit.Ignore
    public void testTransportSupportingSignedCertConstraints() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingSignedCertConstraintsPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                "org/apache/cxf/systest/ws/wssec10/client/bob.properties");
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "bob");
        
        try {
            x509Port.doubleIt(25);
            fail("Failure expected on bob");
        } catch (Exception ex) {
            // expected
        }
        
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
            "org/apache/cxf/systest/ws/wssec10/client/alice.properties");
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "alice");
    
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportKVT() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportKVTPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        // DOM
        x509Port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(x509Port);
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    // TODO
    @org.junit.Test
    @org.junit.Ignore
    public void testKeyIdentifier2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItOperations.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierPort2");
        DoubleItPortType2 x509Port = 
                service.getPort(portQName, DoubleItPortType2.class);
        updateAddressPort(x509Port, PORT);
        
        List<Header> headers = new ArrayList<Header>();
        Header dummyHeader = new Header(new QName("uri:org.apache.cxf", "dummy"), "dummy-header",
                                        new JAXBDataBinding(String.class));
        headers.add(dummyHeader);
        ((BindingProvider)x509Port).getRequestContext().put(Header.HEADER_LIST, headers);
        
        int response = x509Port.doubleIt(25);
        assertEquals(50, response);
        
        int response2 = x509Port.doubleIt2(15);
        assertEquals(30, response2);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    // Just sending an X.509 Token without a Signature is not supported in the StAX layer (yet) 
    @org.junit.Test
    public void testSupportingToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingTokenPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);
        port.doubleIt(25);
        
        // This should fail, as the client is not sending an X.509 Supporting Token
        portQName = new QName(NAMESPACE, "DoubleItTransportSupportingTokenPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not sending an X.509 Supporting Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "These policy alternatives can not be satisfied";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        // This should fail, as the client is not sending a PKI Token
        portQName = new QName(NAMESPACE, "DoubleItTransportPKISupportingTokenPort");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a PKI token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "These policy alternatives can not be satisfied";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testNegativeEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxX509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxX509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTransportNegativeEndorsingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the client is not endorsing the token
        portQName = new QName(NAMESPACE, "DoubleItTransportNegativeEndorsingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);
        
        try {
            // DOM
            port.doubleIt(25);
            fail("Failure expected on not endorsing the token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "These policy alternatives can not be satisfied";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        try {
            // Streaming
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on not endorsing the token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "These policy alternatives can not be satisfied";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
}
