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

package org.apache.cxf.systest.ws.password;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A set of tests for configuring WS-Security using password properties, as opposed to using a callbackhandler.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class PasswordPropertiesTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public PasswordPropertiesTest(TestParam type) {
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
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {new TestParam(PORT, false),
                                              new TestParam(PORT, true),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PasswordPropertiesTest.class.getResource("DoubleItPassword.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTPort");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        Client client = ClientProxy.getClient(port);
        client.getRequestContext().put(SecurityConstants.USERNAME, "Alice");
        client.getRequestContext().put(SecurityConstants.PASSWORD, "ecilA");

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PasswordPropertiesTest.class.getResource("DoubleItPassword.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUTSignedPort");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        Client client = ClientProxy.getClient(port);
        client.getRequestContext().put(SecurityConstants.USERNAME, "abcd");
        client.getRequestContext().put(SecurityConstants.PASSWORD, "dcba");
        client.getRequestContext().put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        client.getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, "bob.properties");

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testAsymmetricBinding() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PasswordPropertiesTest.class.getResource("DoubleItPassword.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        Client client = ClientProxy.getClient(port);
        client.getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        client.getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES, "alice.properties");
        client.getRequestContext().put(SecurityConstants.SIGNATURE_PASSWORD, "password");
        client.getRequestContext().put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        client.getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, "bob.properties");

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
