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
package org.apache.cxf.systest.sts.stsclient;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for STSClient configuration.
 */
public class STSClientTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            STSClientTest.class.getResource("cxf-service.xml")
        )));
        assertTrue(launchServer(new STSServer("cxf-transport.xml")));
    }

    @org.junit.Test
    public void testSTSClientName() throws Exception {
        createBus(getClass().getResource("cxf-client-name.xml").toString());

        URL wsdl = STSClientTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, PORT);

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
    }

    @org.junit.Test
    public void testDefaultSTSClient() throws Exception {
        createBus(getClass().getResource("cxf-default-client.xml").toString());

        URL wsdl = STSClientTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, PORT);

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
