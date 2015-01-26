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

package org.apache.cxf.systest.ws.ut;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * A set of tests for Username Tokens over the Transport Binding. 
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class UsernameTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;
    
    public UsernameTokenTest(TestParam type) {
        this.test = type;
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
    }
    
    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {
       
        return Arrays.asList(new TestParam[][] {{new TestParam(PORT, false)},
                                                {new TestParam(PORT, true)},
                                                {new TestParam(STAX_PORT, false)},
                                                {new TestParam(STAX_PORT, true)},
        });
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testPlaintext() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        utPort.doubleIt(25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testPlaintextCreated() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextCreatedPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        utPort.doubleIt(25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testPlaintextSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextSupportingPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        utPort.doubleIt(25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testPasswordHashed() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItHashedPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        utPort.doubleIt(25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testNoPassword() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItNoPasswordPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        utPort.doubleIt(25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignedEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedEndorsingPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        utPort.doubleIt(25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignedEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedEncryptedPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        utPort.doubleIt(25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        utPort.doubleIt(25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testNoUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItInlinePolicyPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        try {
            utPort.doubleIt(25);
            fail("Failure expected on no UsernameToken");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the token inclusion requirement";
            assertTrue(ex.getMessage().contains(error)
                   || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testPasswordHashedReplay() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        QName portQName = new QName(NAMESPACE, "DoubleItHashedPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (!test.isStreaming()) {
            Client cxfClient = ClientProxy.getClient(utPort);
            SecurityHeaderCacheInterceptor cacheInterceptor =
                new SecurityHeaderCacheInterceptor();
            cxfClient.getOutInterceptors().add(cacheInterceptor);
            
            // Make two invocations with the same UsernameToken
            utPort.doubleIt(25);
            try {
                utPort.doubleIt(25);
                fail("Failure expected on a replayed UsernameToken");
            } catch (javax.xml.ws.soap.SOAPFaultException ex) {
                assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
            }
        }
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    // In this test, the service is using the UsernameTokenInterceptor, but the
    // client is using the WSS4JOutInterceptor
    @org.junit.Test
    public void testPasswordHashedNoBindingReplay() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        QName portQName = new QName(NAMESPACE, "DoubleItDigestNoBindingPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (!test.isStreaming() && PORT.equals(test.getPort())) {
            Client cxfClient = ClientProxy.getClient(utPort);
            SecurityHeaderCacheInterceptor cacheInterceptor =
                new SecurityHeaderCacheInterceptor();
            cxfClient.getOutInterceptors().add(cacheInterceptor);
            
            // Make two invocations with the same UsernameToken
            utPort.doubleIt(25);
            try {
                utPort.doubleIt(25);
                fail("Failure expected on a replayed UsernameToken");
            } catch (javax.xml.ws.soap.SOAPFaultException ex) {
                assertTrue(ex.getMessage().equals(WSSecurityException.UNIFIED_SECURITY_ERR));
            }
        }
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testPlaintextPrincipal() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextPrincipalPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }
        
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "Alice");
        utPort.doubleIt(25);
        
        try {
            ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "Frank");
            utPort.doubleIt(30);
            fail("Failure expected on a user with the wrong role");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Unauthorized";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
}
