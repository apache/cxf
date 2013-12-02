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
package org.apache.cxf.systest.sts.transport;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.systest.sts.deployment.StaxSTSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the TransportBinding. The CXF client gets a token from the STS over TLS, and then
 * sends it to the CXF endpoint over TLS.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class TransportBindingTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    static final String STAX_STSPORT2 = allocatePort(StaxSTSServer.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(Server.class);
    private static final String STAX_PORT = allocatePort(StaxServer.class);
    
    final TestParam test;
    
    public TransportBindingTest(TestParam type) {
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
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxSTSServer.class, true)
        );
    }
    
    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {
       
        return Arrays.asList(new TestParam[][] {{new TestParam(PORT, false, STSPORT)},
                                                {new TestParam(PORT, true, STSPORT)},
                                                {new TestParam(STAX_PORT, false, STSPORT)},
                                                {new TestParam(STAX_PORT, true, STSPORT)},
                                                
                                                {new TestParam(PORT, false, STAX_STSPORT)},
                                                {new TestParam(PORT, true, STAX_STSPORT)},
                                                {new TestParam(STAX_PORT, false, STAX_STSPORT)},
                                                {new TestParam(STAX_PORT, true, STAX_STSPORT)},
        });
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testSAML1() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TransportBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());
        
        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }
        
        doubleIt(transportSaml1Port, 25);
        
        ((java.io.Closeable)transportSaml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSAML2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TransportBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
        DoubleItPortType transportSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, test.getPort());
        
        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml2Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml2Port);
        }
        
        doubleIt(transportSaml2Port, 25);
        
        ((java.io.Closeable)transportSaml2Port).close();
        bus.shutdown(true);
    }
    
    /**
     * In this test-case, the client sends another cert to the STS for inclusion in the
     * SAML Assertion and connects via 2-way TLS as normal to the service provider. The
     * service provider will fail, as the TLS cert does not match the cert provided in
     * the SAML Assertion.
     */
    @org.junit.Test
    public void testUnknownClient() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TransportBindingTest.class.getResource("cxf-bad-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }
        
        try {
            doubleIt(transportSaml1Port, 35);
            fail("Expected failure on an unknown client");
        } catch (javax.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }
        
        ((java.io.Closeable)transportSaml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSAML1Endorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TransportBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1EndorsingPort");
        DoubleItPortType transportSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }
        
        doubleIt(transportSaml1Port, 25);
        
        ((java.io.Closeable)transportSaml1Port).close();
        bus.shutdown(true);
    }
    
    /**
     * In this test-case, the client sends a request for a Security Token with no
     * AppliesTo address (configured in Spring on the STSClient object). The STS fails as
     * it will not issue a token to an unknown address.
     */
    @org.junit.Test
    public void testUnknownAddress() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = TransportBindingTest.class.getResource("cxf-bad-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1EndorsingPort");
        DoubleItPortType transportSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }
        
        try {
            doubleIt(transportSaml1Port, 35);
            //fail("Expected failure on an unknown address");
        } catch (javax.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }
        
        ((java.io.Closeable)transportSaml1Port).close();
        bus.shutdown(true);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
}
