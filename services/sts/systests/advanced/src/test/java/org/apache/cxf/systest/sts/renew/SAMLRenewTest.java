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
package org.apache.cxf.systest.sts.renew;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;

import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * This is a set of system tests to renew SAML tokens. The client obtains a (soon to be expired token)
 * from the STS, and sends it to the service provider, which should succeed. The client then sleeps to
 * expire the token, and the IssuedTokenInterceptorProvider should realise that the token is expired,
 * and renew it with the STS, before making another service invocation.
 * 
 * These tests also illustrate proof-of-possession for renewing a token.
 */
public class SAMLRenewTest extends AbstractBusClientServerTestBase {
    
    static final String STSPORT = allocatePort(STSServerPOP.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(Server.class);
    
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
                   launchServer(STSServerPOP.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testRenewExpiredTokens() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SAMLRenewTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SAMLRenewTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        QName saml1PortQName = new QName(NAMESPACE, "DoubleItTransportSaml1Port");
        DoubleItPortType saml1Port = 
            service.getPort(saml1PortQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT);
        
        QName saml1BearerPortQName = new QName(NAMESPACE, "DoubleItTransportSaml1BearerPort");
        DoubleItPortType saml1BearerPort = 
            service.getPort(saml1BearerPortQName, DoubleItPortType.class);
        updateAddressPort(saml1BearerPort, PORT);
        
        QName saml2PortQName = new QName(NAMESPACE, "DoubleItTransportSaml2Port");
        DoubleItPortType saml2Port = 
            service.getPort(saml2PortQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        
        QName saml2NoRenewPortQName = new QName(NAMESPACE, "DoubleItTransportSaml2NoRenewPort");
        DoubleItPortType saml2NoRenewPort = 
            service.getPort(saml2NoRenewPortQName, DoubleItPortType.class);
        updateAddressPort(saml2NoRenewPort, PORT);
        
        QName saml2IntermediaryPortQName = new QName(NAMESPACE, "DoubleItTransportSaml2IntermediaryPort");
        DoubleItPortType saml2IntermediaryPort = 
            service.getPort(saml2IntermediaryPortQName, DoubleItPortType.class);
        updateAddressPort(saml2IntermediaryPort, PORT);
        
        ((BindingProvider)saml2IntermediaryPort).getRequestContext().put(
            "security.username", "alice"
        );
        
        // Make initial successful invocation(s)
        doubleIt(saml1Port, 25);
        doubleIt(saml1BearerPort, 30);
        doubleIt(saml2Port, 35);
        doubleIt(saml2NoRenewPort, 35);
        doubleIt(saml2IntermediaryPort, 40);
        
        // Now sleep to expire the token(s)
        Thread.sleep(8 * 1000);
        
        // The IssuedTokenInterceptorProvider should renew the token 
        BindingProvider p = (BindingProvider)saml1Port;
        STSClient stsClient = (STSClient)p.getRequestContext().get(SecurityConstants.STS_CLIENT);
        stsClient.setTtl(300);
        doubleIt(saml1Port, 25);
        
        try {
            // The IssuedTokenInterceptorProvider should renew the token - but it should fail on 
            // lack of Proof-of-Possession
            doubleIt(saml1BearerPort, 30);
            fail("Expected failure on no Proof-of-Possession");
        } catch (Exception ex) {
            // expected
        }
        
        // The IssuedTokenInterceptorProvider should renew the token 
        p = (BindingProvider)saml2Port;
        stsClient = (STSClient)p.getRequestContext().get(SecurityConstants.STS_CLIENT);
        stsClient.setTtl(300);
        doubleIt(saml2Port, 35);
        
        // Renew should fail here, but it should fall back to issue
        doubleIt(saml2NoRenewPort, 35);
        
        doubleIt(saml2IntermediaryPort, 40);
        
        ((java.io.Closeable)saml1Port).close();
        ((java.io.Closeable)saml1BearerPort).close();
        ((java.io.Closeable)saml2Port).close();
        ((java.io.Closeable)saml2IntermediaryPort).close();
        bus.shutdown(true);
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
}
