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

package org.apache.cxf.systest.ws.action;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.ut.SecurityHeaderCacheInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A set of tests for WS-Security actions (i.e. the non WS-SecurityPolicy approach).
 */
public class ActionTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static boolean unrestrictedPoliciesInstalled = 
        SecurityTestUtil.checkUnrestrictedPoliciesInstalled();

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void test3DESEncryptionGivenKey() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleIt3DESEncryptionPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        port.doubleIt(25);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // Successful call
        port.doubleIt(25);
        
        // This should fail, as the client is not sending a UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a UsernameToken element");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().equals(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
        
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUsernameTokenReplay() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        Client cxfClient = ClientProxy.getClient(port);
        SecurityHeaderCacheInterceptor cacheInterceptor =
            new SecurityHeaderCacheInterceptor();
        cxfClient.getOutInterceptors().add(cacheInterceptor);
        
        // Make two invocations with the same UsernameToken
        port.doubleIt(25);
        try {
            port.doubleIt(25);
            fail("Failure expected on a replayed UsernameToken");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().equals(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testEncryptedPassword() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedPasswordPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        port.doubleIt(25);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignedTimestampReplay() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedTimestampPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        Client cxfClient = ClientProxy.getClient(port);
        SecurityHeaderCacheInterceptor cacheInterceptor =
            new SecurityHeaderCacheInterceptor();
        cxfClient.getOutInterceptors().add(cacheInterceptor);
        
        // Make two invocations with the same SecurityHeader
        port.doubleIt(25);
        try {
            port.doubleIt(25);
            fail("Failure expected on a replayed Timestamp");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().equals(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Here the client is using "Actions", where the server is using an AsymmetricBinding policy
    @org.junit.Test
    public void testAsymmetricActionToPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // Successful call
        port.doubleIt(25);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Here the client is using "Actions", where the server is using an AsymmetricBinding policy
    @org.junit.Test
    public void testAsymmetricEncryptBeforeSigningActionToPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = ActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = ActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptBeforeSigningPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // Successful call
        port.doubleIt(25);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
