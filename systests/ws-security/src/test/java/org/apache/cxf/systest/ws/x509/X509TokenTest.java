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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.ut.SecurityHeaderCacheInterceptor;
import org.apache.cxf.systest.ws.x509.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * A set of tests for X.509 Tokens.
 */
public class X509TokenTest extends AbstractBusClientServerTestBase {
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
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
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
        
        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
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

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItIssuerSerialPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
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

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItThumbprintPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
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

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricIssuerSerialPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricThumbprint() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricThumbprintPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
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

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricProtectTokensPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
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

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricProtectTokensPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
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

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportEndorsingSP11() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingSP11Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSignedEndorsing() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSignedEndorsingPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportEndorsingEncrypted() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportEndorsingEncryptedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSignedEndorsingEncrypted() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSignedEndorsingEncryptedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignature() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignatureSP11() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignatureSP11Port");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricEncryption() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncryptionPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSignatureReplay() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509Signature.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignaturePort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        
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
            String error = "A replay attack has been detected";
            assertTrue(ex.getMessage().contains(error));
        }
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testTransportSupportingSigned() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingSignedPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        x509Port.doubleIt(25);
    }
    
    @org.junit.Test
    public void testTransportSupportingSignedCertConstraints() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingSignedCertConstraintsPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                "org/apache/cxf/systest/ws/wssec10/client/bob.properties");
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "bob");
        
        try {
            x509Port.doubleIt(25);
            fail("Failure expected on bob");
        } catch (Exception ex) {
            // expected
        }
        
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
            "org/apache/cxf/systest/ws/wssec10/client/alice.properties");
        ((BindingProvider)x509Port).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "alice");
    
        x509Port.doubleIt(25);
    }
    
    @org.junit.Test
    public void testTransportKVT() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509TokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509TokenTest.class.getResource("DoubleItX509.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportKVTPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT2);
        x509Port.doubleIt(25);
        
        bus.shutdown(true);
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
