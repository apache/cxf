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

import java.math.BigInteger;
import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.x509.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import wssec.x509.DoubleItPortType;
import wssec.x509.DoubleItService;

/**
 * A set of tests for X.509 Tokens.
 */
public class X509TokenTest extends AbstractBusClientServerTestBase {
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
    public void testKeyIdentifier() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        DoubleItPortType x509Port = service.getDoubleItKeyIdentifierPort();
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testIssuerSerial() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        DoubleItPortType x509Port = service.getDoubleItIssuerSerialPort();
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testThumbprint() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        DoubleItPortType x509Port = service.getDoubleItThumbprintPort();
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testAsymmetricIssuerSerial() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        DoubleItPortType x509Port = service.getDoubleItAsymmetricIssuerSerialPort();
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testAsymmetricProtectTokens() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        DoubleItPortType x509Port = service.getDoubleItAsymmetricProtectTokensPort();
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testSymmetricProtectTokens() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        DoubleItPortType x509Port = service.getDoubleItSymmetricProtectTokensPort();
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(BigInteger.valueOf(25));
    }
    
    @org.junit.Test
    public void testTransportEndorsing() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        DoubleItService service = new DoubleItService();
        DoubleItPortType x509Port = service.getDoubleItTransportEndorsingPort();
        updateAddressPort(x509Port, PORT2);
        x509Port.doubleIt(BigInteger.valueOf(25));
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
