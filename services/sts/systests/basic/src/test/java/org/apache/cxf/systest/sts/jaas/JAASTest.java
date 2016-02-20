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
package org.apache.cxf.systest.sts.jaas;

import java.net.URL;

import javax.ws.rs.WebApplicationException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * This tests JAAS authentication to the STS. A Username + Password extracted from either
 * a WS-Security UsernameToken for the JAX-WS service, or via HTTP/BA for a JAX-RS service, 
 * is dispatches to the STS for validation via JAAS. 
 * 
 * The service also asks for a SAML Token with roles enabled in it, and these roles 
 * are stored in the security context for authorization.
 */
public class JAASTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(Server.class);
    private static final String PORT2 = allocatePort(Server2.class);
    
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
                   launchServer(Server2.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
        
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testSuccessfulInvocation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAASTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = JAASTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort");
        DoubleItPortType utPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice");
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "clarinet");
        
        doubleIt(utPort, 25);
        
        // Note that the UsernameToken should be cached for the second invocation
        doubleIt(utPort, 35);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSuccessfulInvocationWithProperties() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAASTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = JAASTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort2");
        DoubleItPortType utPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice");
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "clarinet");
        
        doubleIt(utPort, 25);
        
        // Note that the UsernameToken should be cached for the second invocation
        doubleIt(utPort, 35);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUnsuccessfulAuthentication() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAASTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = JAASTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort");
        DoubleItPortType utPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice");
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "clarinet2");
        
        try {
            doubleIt(utPort, 25);
            fail("Failure expected on an incorrect password");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUnsuccessfulAuthorization() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAASTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = JAASTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort");
        DoubleItPortType utPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.USERNAME, "bob");
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "trombone");
        
        try {
            doubleIt(utPort, 25);
            fail("Failure expected on an incorrect role");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSuccessfulPassthroughInvocation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAASTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = JAASTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort3");
        DoubleItPortType utPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice");
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "clarinet");
        
        doubleIt(utPort, 25);
        
        // Note that the UsernameToken should be cached for the second invocation
        doubleIt(utPort, 35);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUnsuccessfulAuthenticationPassthroughInvocation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAASTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = JAASTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort3");
        DoubleItPortType utPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice");
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "clarinet2");
        
        try {
            doubleIt(utPort, 25);
            fail("Failure expected on an incorrect password");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUnsuccessfulAuthorizationPassthroughInvocation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAASTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = JAASTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort3");
        DoubleItPortType utPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.USERNAME, "bob");
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "trombone");
        
        try {
            doubleIt(utPort, 25);
            fail("Failure expected on an incorrect role");
        } catch (Exception ex) {
            // expected
        }
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    // Here the service config has no TLS settings for the call to the STS...it's configured
    // separately via the JAAS configuration
    @org.junit.Test
    @org.junit.Ignore
    public void testSuccessfulInvocationConfig() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAASTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = JAASTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort");
        DoubleItPortType utPort = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT2);
        
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.USERNAME, "alice");
        ((BindingProvider)utPort).getRequestContext().put(
            SecurityConstants.PASSWORD, "clarinet");
        
        doubleIt(utPort, 25);
        
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testJAXRSSuccessfulInvocation() throws Exception {
        final String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs";
        doubleIt("alice", "clarinet", address, false);
    }
    
    @org.junit.Test(expected = RuntimeException.class)
    public void testJAXRSUnsuccessfulAuthentication() throws Exception {
        final String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs";
        doubleIt("alice", "clarinet2", address, true);
    }
    
    @org.junit.Test(expected = RuntimeException.class)
    public void testJAXRSUnsuccessfulAuthorization() throws Exception {
        final String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs";
        doubleIt("bob", "trombone", address, true);
    }
    
    @org.junit.Test
    public void testJAXRSSuccessfulPassthroughInvocation() throws Exception {
        final String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs3";
        doubleIt("alice", "clarinet", address, false);
    }
    
    @org.junit.Test(expected = RuntimeException.class)
    public void testJAXRSUnsuccessfulAuthenticationPassthroughInvocation() throws Exception {
        final String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs3";
        doubleIt("alice", "clarinet2", address, true);
    }
    
    @org.junit.Test(expected = RuntimeException.class)
    public void testJAXRSUnsuccessfulAuthorizationPassthroughInvocation() throws Exception {
        final String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs3";
        doubleIt("bob", "trombone", address, true);
    }
    
    @org.junit.Test
    public void testJAXRSSuccessfulInvocationConfig() throws Exception {
        final String address = "https://localhost:" + PORT2 + "/doubleit/services/doubleit-rs";
        doubleIt("alice", "clarinet", address, false);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
    
    private static void doubleIt(String username, String password, 
                                 String address,
                                 boolean authFailureExpected) {
        final String configLocation = "org/apache/cxf/systest/sts/jaas/cxf-client.xml";
        final int numToDouble = 25;  
       
        WebClient client = null;
        if (username != null && password != null) {
            client = WebClient.create(address, username, password, configLocation);
        } else {
            client = WebClient.create(address, configLocation);
        }
        client.type("text/plain").accept("text/plain");
        try {
            int resp = client.post(numToDouble, Integer.class);
            if (authFailureExpected) {
                throw new RuntimeException("Exception expected");
            }
            org.junit.Assert.assertEquals(2 * numToDouble, resp);
        } catch (WebApplicationException ex) {
            if (!authFailureExpected) {
                throw new RuntimeException("Unexpected exception");
            }
            org.junit.Assert.assertEquals(500, ex.getResponse().getStatus());
        }
    }
    
}
