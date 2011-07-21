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

package org.apache.cxf.systest.ws.kerberos;

import java.math.BigInteger;
import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.kerberos.client.KerberosCallbackHandler;
import org.apache.cxf.systest.ws.kerberos.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.ws.security.WSConstants;

import org.junit.BeforeClass;

import wssec.kerberos.DoubleItPortType;
import wssec.kerberos.DoubleItService;

/**
 * A set of tests for Kerberos Tokens.
 */
public class KerberosTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);

    private boolean unrestrictedPoliciesInstalled = checkUnrestrictedPoliciesInstalled();
    
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
    public void testKerberosOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType kerberosPort = service.getDoubleItKerberosTransportPort();
        updateAddressPort(kerberosPort, PORT2);
        
        try {
            kerberosPort.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with no Kerberos Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No BST CallbackHandler available"));
        }
        
        try {
            KerberosCallbackHandler handler = new KerberosCallbackHandler();
            handler.setValueType(WSConstants.WSS_GSS_KRB_V5_AP_REQ);
                ((BindingProvider)kerberosPort).getRequestContext().put(
                    "ws-security.bst-callback-handler", handler
                );
            kerberosPort.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with the wrong Kerberos Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("incorrect Kerberos Token Type"));
        }
        
        ((BindingProvider)kerberosPort).getRequestContext().put(
            "ws-security.bst-callback-handler", new KerberosCallbackHandler()
        );

        BigInteger result = kerberosPort.doubleIt(BigInteger.valueOf(25));
        assertTrue(result.equals(BigInteger.valueOf(50)));
    }
    
    @org.junit.Test
    public void testKerberosOverSymmetric() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType kerberosPort = service.getDoubleItKerberosSymmetricPort();
        updateAddressPort(kerberosPort, PORT);
        
        try {
            kerberosPort.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with no Kerberos Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No BST CallbackHandler available"));
        }
        
        try {
            KerberosCallbackHandler handler = new KerberosCallbackHandler();
            handler.setToken("123456566");
                ((BindingProvider)kerberosPort).getRequestContext().put(
                    "ws-security.bst-callback-handler", handler
                );
            kerberosPort.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with the wrong Kerberos Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((BindingProvider)kerberosPort).getRequestContext().put(
            "ws-security.bst-callback-handler", new KerberosCallbackHandler()
        );

        BigInteger result = kerberosPort.doubleIt(BigInteger.valueOf(25));
        assertTrue(result.equals(BigInteger.valueOf(50)));
    }
    
    @org.junit.Test
    public void testKerberosOverSymmetricSupporting() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType kerberosPort = service.getDoubleItKerberosSymmetricSupportingPort();
        updateAddressPort(kerberosPort, PORT);
        
        try {
            kerberosPort.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with no Kerberos Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No BST CallbackHandler available"));
        }
        
        ((BindingProvider)kerberosPort).getRequestContext().put(
            "ws-security.bst-callback-handler", new KerberosCallbackHandler()
        );

        BigInteger result = kerberosPort.doubleIt(BigInteger.valueOf(25));
        assertTrue(result.equals(BigInteger.valueOf(50)));
    }
    
    @org.junit.Test
    public void testKerberosOverAsymmetric() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        
        DoubleItPortType kerberosPort = service.getDoubleItKerberosAsymmetricPort();
        updateAddressPort(kerberosPort, PORT);
        
        try {
            kerberosPort.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with no Kerberos Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No BST CallbackHandler available"));
        }
        
        try {
            ((BindingProvider)kerberosPort).getRequestContext().put(
                "ws-security.bst-callback-handler", new KerberosCallbackHandler()
            );
            kerberosPort.doubleIt(BigInteger.valueOf(25));
            fail("Expected failure on an invocation with the wrong Kerberos Token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("incorrect Kerberos Token Type"));
        }
        
        KerberosCallbackHandler handler = new KerberosCallbackHandler();
        handler.setValueType(WSConstants.WSS_GSS_KRB_V5_AP_REQ);
        ((BindingProvider)kerberosPort).getRequestContext().put(
            "ws-security.bst-callback-handler", handler
        );

        BigInteger result = kerberosPort.doubleIt(BigInteger.valueOf(25));
        assertTrue(result.equals(BigInteger.valueOf(50)));
    }
    
    private boolean checkUnrestrictedPoliciesInstalled() {
        try {
            byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

            SecretKey key192 = new SecretKeySpec(
                new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17},
                            "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key192);
            c.doFinal(data);
            return true;
        } catch (Exception e) {
            //
        }
        return false;
    }
}
