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

package org.apache.cxf.systest.ws.tokens;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * This is a test for various properties associated with SupportingTokens, i.e.
 * Signed, Encrypted etc.
 * 
 * It tests DOM against the StAX server
 */
public class StaxEndorsingSupportingTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(StaxEndorsingServer.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(StaxEndorsingServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    
    @org.junit.Test
    public void testEndorsingSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxEndorsingSupportingTokenTest.class.getResource("endorsing-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxEndorsingSupportingTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEndorsingSupportingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        port.doubleIt(25);
        
        // This should fail, as the client is signing (but not endorsing) the X.509 Token
        portQName = new QName(NAMESPACE, "DoubleItEndorsingSupportingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not endorsing the X.509 token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = 
            //    "The received token does not match the endorsing supporting token requirement";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        // This should fail, as the client is not endorsing the X.509 Token
        portQName = new QName(NAMESPACE, "DoubleItEndorsingSupportingPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not endorsing the X.509 token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = 
            //   "The received token does not match the endorsing supporting token requirement";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignedEndorsingSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxEndorsingSupportingTokenTest.class.getResource("endorsing-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxEndorsingSupportingTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignedEndorsingSupportingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        port.doubleIt(25);
        
        // This should fail, as the client is signing (but not endorsing) the X.509 Token
        portQName = new QName(NAMESPACE, "DoubleItSignedEndorsingSupportingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not endorsing the X.509 token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = 
            //    "The received token does not match the signed endorsing supporting token requirement";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        // This should fail, as the client is endorsing but not signing the X.509 Token
        portQName = new QName(NAMESPACE, "DoubleItSignedEndorsingSupportingPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the X.509 token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // System.out.println("ERR4: " + ex.getMessage());
            // String error = 
            //     "The received token does not match the signed endorsing supporting token requirement";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    
}
