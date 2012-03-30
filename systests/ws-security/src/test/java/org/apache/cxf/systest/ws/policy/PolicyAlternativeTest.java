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

package org.apache.cxf.systest.ws.policy;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.policy.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * This is a test for policy alternatives. The endpoint requires either a UsernameToken (insecured) OR
 * a message signature using the Asymmetric binding.
 */
public class PolicyAlternativeTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

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
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }
    
    /**
     * The client uses the Asymmetric policy - this should succeed.
     */
    @org.junit.Test
    public void testAsymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT2);
        
        utPort.doubleIt(25);
    }
    
    /**
     * The client uses no security - this should fail.
     */
    @org.junit.Test
    public void testNoSecurity() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItNoSecurityPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT2);
        
        try {
            utPort.doubleIt(25);
            fail("Failure expected on no Security");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
    }
    
    /**
     * The client uses the UsernameToken policy - this should succeed.
     */
    @org.junit.Test
    public void testUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT2);
        
        utPort.doubleIt(25);
    }
    
}
