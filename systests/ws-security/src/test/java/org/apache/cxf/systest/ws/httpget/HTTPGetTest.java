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

package org.apache.cxf.systest.ws.httpget;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A set of tests for CXF-4629.
 */
public class HTTPGetTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

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
    public void testSOAPClientSecurityPolicy() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HTTPGetTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = HTTPGetTest.class.getResource("DoubleItHTTPGet.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKeyIdentifierPort");
        DoubleItPortType x509Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(x509Port, PORT);
        int result = x509Port.doubleIt(25);
        assertEquals(result, 50);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testHTTPGetClientSecurityPolicy() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HTTPGetTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        String address = "http://localhost:" + PORT + "/DoubleItX509KeyIdentifier/DoubleIt";
        WebClient client = WebClient.create(address);
        client.query("numberToDouble", "20");
        
        try {
            client.get(XMLSource.class);
            fail("Failure expected on security policy failure");
        } catch (Exception ex) {
            // expected
        }
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignedBodyTimestamp() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HTTPGetTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = HTTPGetTest.class.getResource("DoubleItHTTPGet.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignBodyPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass", 
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
                     + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                     + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;");
        
        bus.getOutInterceptors().add(new WSS4JOutInterceptor(outProps));
        
        int result = port.doubleIt(25);
        assertEquals(result, 50);
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testHTTPGetSignedBody() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HTTPGetTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        String address = "http://localhost:" + PORT + "/DoubleItSignBody/DoubleIt";
        WebClient client = WebClient.create(address);
        client.query("numberToDouble", "20");
        /*
        XMLSource result = client.get(XMLSource.class);
        result.setBuffering(true);
        
        String input = result.getNode("//doubledNumber", String.class);
        assertTrue(input.startsWith("<doubledNumber>40"));
        */
        
        try {
            client.get(XMLSource.class);
            fail("Failure expected on security policy failure");
        } catch (Exception ex) {
            // expected
        }
        
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
