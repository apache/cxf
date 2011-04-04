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

package org.apache.cxf.systest.ws.saml;

import java.math.BigInteger;
import java.net.URL;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.saml.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import wssec.saml.DoubleItPortType;
import wssec.saml.DoubleItService;

/**
 * A set of tests for SAML Tokens.
 */
public class SamlTokenTest extends AbstractBusClientServerTestBase {
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
    }

    @org.junit.Test
    public void testSaml1OverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType saml1Port = service.getDoubleItSaml1TransportPort();
        
        try {
            saml1Port.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assert ex.getMessage().contains("No SAML CallbackHandler available");
        }
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler",
            new org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler()
        );
        try {
            saml1Port.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with a SAML2 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assert ex.getMessage().contains("Wrong SAML Version");
        }

        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler",
            new org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler(false)
        );
        BigInteger result = saml1Port.doubleIt(BigInteger.valueOf(25));
        assert result.equals(BigInteger.valueOf(50));
    }
    
    @org.junit.Test
    public void testSaml2OverSymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType saml2Port = service.getDoubleItSaml2SymmetricPort();
        
        try {
            saml2Port.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assert ex.getMessage().contains("No SAML CallbackHandler available");
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler",
            new org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assert ex.getMessage().contains("Wrong SAML Version");
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler",
            new org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler()
        );
        BigInteger result = saml2Port.doubleIt(BigInteger.valueOf(25));
        assert result.equals(BigInteger.valueOf(50));
    }

    @org.junit.Test
    public void testSaml2OverAsymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType saml2Port = service.getDoubleItSaml2AsymmetricPort();
        
        try {
            saml2Port.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assert ex.getMessage().contains("No SAML CallbackHandler available");
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler",
            new org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assert ex.getMessage().contains("Wrong SAML Version");
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler",
            new org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler()
        );
        BigInteger result = saml2Port.doubleIt(BigInteger.valueOf(25));
        assert result.equals(BigInteger.valueOf(50));
    }
    
}
