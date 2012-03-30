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

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.ut.server.ServerDerived;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * A set of tests for keys derived from Username Tokens.
 */
public class UsernameTokenDerivedTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ServerDerived.class);
    static final String PORT2 = allocatePort(ServerDerived.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(ServerDerived.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }

    /**
     * Here the key derived from a UsernameToken is used as a protection token for the 
     * symmetric binding, and used to sign the SOAP Body.
     */
    @org.junit.Test
    public void testSymmetricProtectionSignatureToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenDerivedTest.class.getResource("client/client-derived.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenDerivedTest.class.getResource("DoubleItUtDerived.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricProtectionSigPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(25);
    }
    
    /**
     * Here the key derived from a UsernameToken (and derived again) is used as a protection 
     * token for the symmetric binding, and used to sign the SOAP Body.
     */
    @org.junit.Test
    public void testSymmetricProtectionSignatureDKToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenDerivedTest.class.getResource("client/client-derived.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenDerivedTest.class.getResource("DoubleItUtDerived.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricProtectionSigDKPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(25);
    }
    
    /**
     * Here the key derived from a UsernameToken is used as a protection token for the 
     * symmetric binding, and used to encrypt the SOAP Body.
     */
    @org.junit.Test
    public void testSymmetricProtectionEncryptionToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenDerivedTest.class.getResource("client/client-derived.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenDerivedTest.class.getResource("DoubleItUtDerived.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricProtectionEncPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(25);
    }
    
    /**
     * Here the key derived from a UsernameToken is used to sign the Timestamp over the Transport
     * binding.
     */
    @org.junit.Test
    public void testTransportEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenDerivedTest.class.getResource("client/client-derived.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenDerivedTest.class.getResource("DoubleItUtDerived.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT2);
        
        utPort.doubleIt(25);
    }
    
    /**
     * Here the key derived from a UsernameToken is used to sign the message signature over the
     * Symmetric binding. The UsernameToken is signed.
     */
    @org.junit.Test
    public void testSymmetricSignedEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenDerivedTest.class.getResource("client/client-derived.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenDerivedTest.class.getResource("DoubleItUtDerived.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSignedEndorsingPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(25);
    }
    
    /**
     * Here the key derived from a UsernameToken is used to sign the message signature over the
     * Symmetric binding. The UsernameToken is encrypted.
     */
    @org.junit.Test
    public void testSymmetricEndorsingEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenDerivedTest.class.getResource("client/client-derived.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenDerivedTest.class.getResource("DoubleItUtDerived.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricEndorsingEncryptedPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(25);
    }
    
    /**
     * Here the key derived from a UsernameToken is used to sign the message signature over the
     * Symmetric binding. The UsernameToken is encrypted and signed.
     */
    @org.junit.Test
    public void testSymmetricSignedEndorsingEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenDerivedTest.class.getResource("client/client-derived.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenDerivedTest.class.getResource("DoubleItUtDerived.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSignedEndorsingEncryptedPort");
        DoubleItPortType utPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(25);
    }
    
}
