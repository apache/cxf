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

package org.apache.cxf.systest.soap;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for the XSLT Feature
 */
public class XSLTFeatureTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(XSLTServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(XSLTServer.class, true)
        );
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testNormalInvocation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = XSLTFeatureTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = XSLTFeatureTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // The XSLT Feature adds a "1" to the doubled response
        assertEquals(150, port.doubleIt(25));

        ((java.io.Closeable)port).close();

        bus.shutdown(true);
    }

}
