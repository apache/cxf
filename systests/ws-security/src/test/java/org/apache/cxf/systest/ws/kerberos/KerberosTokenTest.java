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

import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.kerberos.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * A set of tests for Kerberos Tokens. The tests are @Ignore'd, as they require a running KDC. To run the
 * tests, set up a KDC of realm "WS.APACHE.ORG", with principal "alice" and service principal 
 * "bob/service.ws.apache.org". Create keytabs for both principals in "/etc/alice.keytab" and
 * "/etc/bob.keytab" (this can all be edited in src/test/resource/kerberos.jaas". Then disable the
 * @Ignore annotations and run the tests with:
 *  
 * mvn test -Pnochecks -Dtest=KerberosTokenTest 
 *     -Djava.security.auth.login.config=src/test/resources/kerberos.jaas
 * 
 * See here for more information:
 * http://coheigea.blogspot.com/2011/10/using-kerberos-with-web-services-part.html
 */
public class KerberosTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

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
    
    @org.junit.AfterClass
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }

    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosTransportPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);
        
        updateAddressPort(kerberosPort, PORT2);
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverSymmetric() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosSymmetricPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(kerberosPort, PORT);
        
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverSymmetricSupporting() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosSymmetricSupportingPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(kerberosPort, PORT);
        
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverAsymmetric() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosAsymmetricPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(kerberosPort, PORT);
        
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverTransportEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosTransportEndorsingPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(kerberosPort, PORT2);
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverAsymmetricEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosAsymmetricEndorsingPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(kerberosPort, PORT);
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverSymmetricProtection() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosSymmetricProtectionPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);
        
        updateAddressPort(kerberosPort, PORT);
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverSymmetricDerivedProtection() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosSymmetricDerivedProtectionPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);
        
        updateAddressPort(kerberosPort, PORT);
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverAsymmetricSignedEndorsing() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosAsymmetricSignedEndorsingPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);
        
        updateAddressPort(kerberosPort, PORT);
        
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverAsymmetricSignedEncrypted() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosAsymmetricSignedEncryptedPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);
        
        updateAddressPort(kerberosPort, PORT);
        
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverSymmetricEndorsingEncrypted() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosSymmetricEndorsingEncryptedPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);
        
        updateAddressPort(kerberosPort, PORT);
        
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
    }
    
    @org.junit.Test
    @org.junit.Ignore
    public void testKerberosOverSymmetricSignedEndorsingEncrypted() throws Exception {
        
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosSymmetricSignedEndorsingEncryptedPort");
        DoubleItPortType kerberosPort = 
                service.getPort(portQName, DoubleItPortType.class);
        
        updateAddressPort(kerberosPort, PORT);
        
        int result = kerberosPort.doubleIt(25);
        assertTrue(result == 50);
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
