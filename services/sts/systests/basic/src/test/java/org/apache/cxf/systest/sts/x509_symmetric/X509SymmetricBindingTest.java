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
package org.apache.cxf.systest.sts.x509_symmetric;

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
 * Test the Symmetric binding. The CXF client gets a token from the STS by authenticating via an
 * X.509 Cert over the asymmetric binding, and then sends it to the CXF endpoint using 
 * the symmetric binding.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class X509SymmetricBindingTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    static final String STAX_STSPORT2 = allocatePort(StaxSTSServer.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = allocatePort(Server.class);
    private static final String STAX_PORT = allocatePort(StaxServer.class);
    
    final TestParam test;
    
    public X509SymmetricBindingTest(TestParam type) {
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
       
        return Arrays.asList(new TestParam[][] {{new TestParam(PORT, false, STSPORT2)},
                                                {new TestParam(PORT, true, STSPORT2)},
                                                {new TestParam(STAX_PORT, false, STSPORT2)},
                                                {new TestParam(STAX_PORT, true, STSPORT2)},
                                                
                                                {new TestParam(PORT, false, STAX_STSPORT2)},
                                                {new TestParam(PORT, true, STAX_STSPORT2)},
                                                {new TestParam(STAX_PORT, false, STAX_STSPORT2)},
                                                {new TestParam(STAX_PORT, true, STAX_STSPORT2)},
        });
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testX509SAML1() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509SymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509SymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML1Port");
        DoubleItPortType symmetricSaml1Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml1Port, test.getPort());
        
        TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml1Port, test.getStsPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(symmetricSaml1Port);
        }

        doubleIt(symmetricSaml1Port, 25);
        
        ((java.io.Closeable)symmetricSaml1Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testX509SAML2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509SymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509SymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML2Port");
        DoubleItPortType symmetricSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml2Port, test.getPort());
        
        TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml2Port, test.getStsPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(symmetricSaml2Port);
        }
        
        doubleIt(symmetricSaml2Port, 30);
        TokenTestUtils.verifyToken(symmetricSaml2Port);
        
        ((java.io.Closeable)symmetricSaml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testX509SAML2Endorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509SymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509SymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML2EndorsingPort");
        DoubleItPortType symmetricSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml2Port, test.getPort());
        
        TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml2Port, test.getStsPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(symmetricSaml2Port);
        }
        
        // TODO Streaming client is not including a separate main Signature
        if (!test.isStreaming()) {
            doubleIt(symmetricSaml2Port, 30);
        }
        
        ((java.io.Closeable)symmetricSaml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testX509SAML2Supporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509SymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509SymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML2SupportingPort");
        DoubleItPortType symmetricSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml2Port, test.getPort());
        
        TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml2Port, test.getStsPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(symmetricSaml2Port);
        }
        
        doubleIt(symmetricSaml2Port, 30);
        
        ((java.io.Closeable)symmetricSaml2Port).close();
        bus.shutdown(true);
    }

    // Here we refer to the Assertion directly, instead of creating a SecurityTokenReference and using the
    // STR Transform
    @org.junit.Test
    public void testX509SAML2SupportingDirectReferenceToAssertion() throws Exception {
        
        // TODO Not yet supported for the client streaming code
        if (test.isStreaming()) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509SymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509SymmetricBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSAML2SupportingPort");
        DoubleItPortType symmetricSaml2Port = 
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(symmetricSaml2Port, test.getPort());
        
        TokenTestUtils.updateSTSPort((BindingProvider)symmetricSaml2Port, test.getStsPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(symmetricSaml2Port);
        }
        
        ((BindingProvider)symmetricSaml2Port).getRequestContext().put("ws-security.use.str.transform", "false");
        
        doubleIt(symmetricSaml2Port, 30);
        
        ((java.io.Closeable)symmetricSaml2Port).close();
        bus.shutdown(true);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
}
