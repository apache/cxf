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
import java.util.UUID;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.token.BinarySecurity;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This is a test to add a custom BinarySecurityToken to the security header of a service request,
 * and to process it accordingly.
 */
public class BinarySecurityTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(BSTServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(BSTServer.class, true)
        );
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testBinarySecurityToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BinarySecurityTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BinarySecurityTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItBinarySecurityTokenPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // Mock up a BinarySecurityToken to add
        SecurityToken securityToken = new SecurityToken();
        securityToken.setId("_" + UUID.randomUUID().toString());

        Document doc = DOMUtils.newDocument();
        BinarySecurity binarySecurity = new BinarySecurity(doc);
        binarySecurity.setValueType("http://custom-value-type");
        binarySecurity.setToken("This is a token".getBytes());

        securityToken.setToken(binarySecurity.getElement());

        ((BindingProvider)port).getRequestContext().put(SecurityConstants.TOKEN, securityToken);

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

}
