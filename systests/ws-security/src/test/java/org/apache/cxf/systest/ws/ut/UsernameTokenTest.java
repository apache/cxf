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

import java.math.BigInteger;
import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.ut.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import wssec.ut.DoubleItPortType;
import wssec.ut.DoubleItService;

/**
 * A set of tests for Username Tokens over the Transport Binding.
 */
public class UsernameTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
    }

    @org.junit.Test
    public void testPlaintext() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType utPort = service.getDoubleItPlaintextPort();
        updateAddressPort(utPort, PORT);
        utPort.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testPlaintextCreated() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType utPort = service.getDoubleItPlaintextCreatedPort();
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testPasswordHashed() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType utPort = service.getDoubleItHashedPort();
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testNoPassword() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType utPort = service.getDoubleItNoPasswordPort();
        updateAddressPort(utPort, PORT);
        
        utPort.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testSignedEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType utPort = service.getDoubleItSignedEndorsingPort();
        updateAddressPort(utPort, PORT);
        utPort.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testSignedEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType utPort = service.getDoubleItSignedEncryptedPort();
        updateAddressPort(utPort, PORT);
        utPort.doubleIt(BigInteger.valueOf(25));
    }
    
}
