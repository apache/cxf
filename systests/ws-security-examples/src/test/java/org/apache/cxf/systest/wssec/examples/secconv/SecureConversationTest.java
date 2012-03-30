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

package org.apache.cxf.systest.wssec.examples.secconv;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.wssec.examples.common.SecurityTestUtil;
import org.apache.cxf.systest.wssec.examples.secconv.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * A set of tests for SecureConversation using policies defined in the OASIS spec:
 * "WS-SecurityPolicy Examples Version 1.0".
 */
public class SecureConversationTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    
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
     * 2.4.1 (WSS 1.0) Secure Conversation bootstrapped by Mutual
     * Authentication with X.509 Certificates
     */
    @org.junit.Test
    public void testSecureConversation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SecureConversationTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecureConversationTest.class.getResource("DoubleItSecConv.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSecureConversationPort");
        DoubleItPortType samlPort = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(samlPort, PORT);
        
        samlPort.doubleIt(25);
    }
    
}
