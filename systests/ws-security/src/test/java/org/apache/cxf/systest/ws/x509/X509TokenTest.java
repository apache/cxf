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

package org.apache.cxf.systest.ws.x509;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.systest.ws.ut.SecurityHeaderCacheInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.example.contract.doubleit.DoubleItPortType;
import org.example.contract.doubleit.DoubleItPortType2;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * A set of tests for X.509 Tokens. 
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class X509TokenTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String STAX_PORT = allocatePort(StaxServer.class);
    public static final String INTERMEDIARY_PORT = allocatePort(Intermediary.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    static final String STAX_PORT2 = allocatePort(StaxServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static boolean unrestrictedPoliciesInstalled = 
        SecurityTestUtil.checkUnrestrictedPoliciesInstalled();

    final TestParam test;
    
    public X509TokenTest(TestParam type) {
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
                "Intermediary failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Intermediary.class, true)
        );
    }
    
    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {
       
        return Arrays.asList(new TestParam[][] {{new TestParam(PORT, false)},
                                                {new TestParam(PORT, true)},
                                                {new TestParam(STAX_PORT, false)},
                                                {new TestParam(STAX_PORT, true)},
        });
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testSymmetricErrorMessage() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricErrorMessagePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        try {
            x509Port.doubleIt(25);
            fail("Failure expected on an incorrect key");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "No certificates were found for decryption";
            if (STAX_PORT.equals(test.getPort())) {
                error = "Referenced security token could not be retrieved";
            }
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testKeyIdentifier() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testKeyIdentifierDerived() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierDerivedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testKeyIdentifierEncryptBeforeSigning() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierEncryptBeforeSigningPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testKeyIdentifierEncryptBeforeSigningDerived() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierEncryptBeforeSigningDerivedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
  
    @org.junit.Test
    public void testKeyIdentifierJaxwsClient() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("jaxws-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                "bob.properties");
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIntermediary() throws Exception {

        if (test.isStreaming() || STAX_PORT.equals(test.getPort())) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("intermediary-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItIntermediary.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, INTERMEDIARY_PORT);
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIssuerSerial() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItIssuerSerialPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testThumbprint() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItThumbprintPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSymmetricThumbprintEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricThumbprintEndorsingPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (!test.isStreaming()) {
            x509Port.doubleIt(25);
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSymmetricEndorsingEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricEndorsingEncryptedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (!test.isStreaming()) {
            x509Port.doubleIt(25);
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testContentEncryptedElements() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItContentEncryptedElementsPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSymmetric256() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetric256Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (!test.isStreaming()) {
            x509Port.doubleIt(25);
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricIssuerSerial() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricIssuerSerialPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricNoInitiatorTokenReference() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricNoInitiatorReferencePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSP11Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryptedPassword() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptedPasswordPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSHA256() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSHA256Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricThumbprint() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricThumbprintPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricPKIPath() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPKIPathPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryptBeforeSigning() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptBeforeSigningPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryptBeforeSigningNoEnc() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptBeforeSigningNoEncPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryptSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricProtectTokens() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricProtectTokensPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricUsernameTokenPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEndorsingPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        if (!test.isStreaming() && !STAX_PORT.equals(test.getPort())) {
            x509Port.doubleIt(25);
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
  
    @org.junit.Test
    public void testSymmetricUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricUsernameTokenPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSymmetricProtectTokens() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricProtectTokensPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        // TODO WSS-456 Streaming
        if (!test.isStreaming()) {
            x509Port.doubleIt(25);
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportEndorsingSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingSP11Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSignedEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSignedEndorsingPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportEndorsingEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingEncryptedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSignedEndorsingEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSignedEndorsingEncryptedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignatureSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignatureSP11Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryption() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptionPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignatureEncryption() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignatureEncryptionPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignatureReplay() throws Exception {
        if (test.isStreaming()) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        Client cxfClient = ClientProxy.getClient(x509Port);
        SecurityHeaderCacheInterceptor cacheInterceptor =
            new SecurityHeaderCacheInterceptor();
        cxfClient.getOutInterceptors().add(cacheInterceptor);
        
        // Make two invocations with the same security header
        x509Port.doubleIt(25);
        try {
            x509Port.doubleIt(25);
            fail("Failure expected on a replayed Timestamp");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSupportingSigned() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingSignedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSupportingSignedCertConstraints() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingSignedCertConstraintsPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                "bob.properties");
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "bob");
        
        try {
            x509Port.doubleIt(25);
            fail("Failure expected on bob");
        } catch (Exception ex) {
            // expected
        }
        
        x509Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, port);
        
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
            "alice.properties");
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "alice");
    
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportKVT() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportKVTPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testKeyIdentifier2() throws Exception {

        if (test.isStreaming()) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItOperations.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierPort2");
        DoubleItPortType2 x509Port = 
                service.getPort(portQName, DoubleItPortType2.class);
        updateAddressPort(x509Port, PORT);
        
        List<Header> headers = new ArrayList<Header>();
        Header dummyHeader = new Header(new QName("uri:org.apache.cxf", "dummy"), "dummy-header",
                                        new JAXBDataBinding(String.class));
        headers.add(dummyHeader);
        ((BindingProvider)x509Port).getRequestContext().put(Header.HEADER_LIST, headers);
        
        int response = x509Port.doubleIt(25);
        assertEquals(50, response);
        
        int response2 = x509Port.doubleIt2(15);
        assertEquals(30, response2);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSupportingToken() throws Exception {
        if (test.isStreaming()) {
            // Just sending an X.509 Token without a Signature is not supported in the StAX layer (yet) 
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingTokenPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);
        
        port.doubleIt(25);
        
        // This should fail, as the client is not sending an X.509 Supporting Token
        portQName = new QName(NAMESPACE, "DoubleItTransportSupportingTokenPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not sending an X.509 Supporting Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "These policy alternatives can not be satisfied";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // This should fail, as the client is not sending a PKI Token
        portQName = new QName(NAMESPACE, "DoubleItTransportPKISupportingTokenPort");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a PKI token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "These policy alternatives can not be satisfied";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testNegativeEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTransportNegativeEndorsingPort");
        DoubleItPortType x509Port = service.getPort(portQName, DoubleItPortType.class);
        String port = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            port = STAX_PORT2;
        }
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        // This should fail, as the client is not endorsing the token
        portQName = new QName(NAMESPACE, "DoubleItTransportNegativeEndorsingPort2");
        x509Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, port);
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        try {
            x509Port.doubleIt(25);
            fail("Failure expected on not endorsing the token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "These policy alternatives can not be satisfied";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("X509Token not satisfied"));
        }
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
  
    @org.junit.Test
    public void testSymmetricSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricProperties() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPropertiesPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, test.getPort());
        
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(x509Port);
        }
        
        x509Port.doubleIt(25);
        
        ((java.io.Closeable)x509Port).close();
        bus.shutdown(true);
    }
    
}
