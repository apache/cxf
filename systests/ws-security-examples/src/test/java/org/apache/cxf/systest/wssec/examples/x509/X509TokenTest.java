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

package org.apache.cxf.systest.wssec.examples.x509;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.wssec.examples.x509.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * A set of tests for X509 Tokens using policies defined in the OASIS spec:
 * "WS-SecurityPolicy Examples Version 1.0".
 */
public class X509TokenTest extends AbstractBusClientServerTestBase {
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

    /**
     * 2.2.1 (WSS1.0) X.509 Certificates, Sign, Encrypt
     */
    @org.junit.Test
    public void testAsymmetricSignEncrypt() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignEncryptPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        x509Port.doubleIt(25);
    }
    
    /**
     * 2.2.2 (WSS1.0) Mutual Authentication with X.509 Certificates, Sign, Encrypt
     */
    @org.junit.Test
    public void testAsymmetricProtectTokens() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricProtectTokensPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        x509Port.doubleIt(25);
    }
    
    /**
     * 2.2.3 (WSS1.1) Anonymous with X.509 Certificate, Sign, Encrypt
     */
    @org.junit.Test
    public void testSymmetricSignEncrypt() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSignEncryptPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
        x509Port.doubleIt(25);
    }
    
    
    
}
