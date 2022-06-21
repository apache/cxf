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
package org.apache.cxf.systest.sts.distributed_caching;

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
 * This is a series of tests of the distributed caching abilities of the STS. In these test-cases,
 * a CXF client invokes on an STS and obtains a security token, which is sent to a service provider.
 * The service provider is configured to validate the received token against a second STS instance.
 * Both STS instances must have a shared distributed cache, and enough time must have elapsed for
 * the first STS instance to replicate the credential to the second STS instance for the test to
 * work.
 */
public class DistributedCachingTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    static final String HZ_CAST_PORT1 = allocatePort(STSServer.class, 3);
    static final String HZ_CAST_PORT2 = allocatePort(STSServer.class, 4);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            DistributedCachingTest.class.getResource("cxf-service.xml"))));
        assertTrue(launchServer(new STSServer(
            DistributedCachingTest.class.getResource("cxf-sts-1.xml"))));
        assertTrue(launchServer(new STSServer(
            DistributedCachingTest.class.getResource("cxf-sts-2.xml"))));
    }

    @org.junit.Test
    public void testSecurityContextToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = DistributedCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSCTPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    @org.junit.Test
    public void testSAMLToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = DistributedCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSAMLPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    @org.junit.Test
    public void testUsernameToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = DistributedCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
