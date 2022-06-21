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

package org.apache.cxf.systest.ws.xkms;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.LocateResultType;
import org.apache.cxf.xkms.model.xkms.PrototypeKeyBindingType;
import org.apache.cxf.xkms.model.xkms.QueryKeyBindingType;
import org.apache.cxf.xkms.model.xkms.RegisterRequestType;
import org.apache.cxf.xkms.model.xkms.RegisterResultType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.apache.cxf.xkms.x509.utils.X509Utils;
import org.example.contract.doubleit.DoubleItPortType;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A set of tests that uses XKMS with WS-Security to locate + validate X.509 tokens.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class XKMSTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String STAX_PORT = allocatePort(StaxServer.class);
    static final String PORT2 = allocatePort(XKMSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public XKMSTest(TestParam type) {
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
                   launchServer(XKMSServer.class, true)
        );
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {new TestParam(PORT, false),
                                              new TestParam(PORT, true),
                                              new TestParam(STAX_PORT, false),
                                              new TestParam(STAX_PORT, true),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testRegisterUnitTest() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = XKMSTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = //XKMSTest.class.getResource("xkms.wsdl");
            new URL("https://localhost:" + PORT2 + "/XKMS?wsdl");

        String ns = "http://www.w3.org/2002/03/xkms#wsdl";
        QName serviceQName = new QName(ns, "XKMSService");
        Service service = Service.create(wsdl, serviceQName);
        QName portQName = new QName(NAMESPACE, "XKMSPort");
        XKMSPortType port =
                service.getPort(portQName, XKMSPortType.class);
        //updateAddressPort(port, PORT2);

        // First try to locate - which should fail

        LocateRequestType locateRequest = new LocateRequestType();
        locateRequest.setId("_xyz");
        locateRequest.setService("http://cxf.apache.org/services/XKMS/");
        QueryKeyBindingType queryKeyBinding = new QueryKeyBindingType();
        UseKeyWithType useKeyWithType = new UseKeyWithType();
        useKeyWithType.setApplication("urn:ietf:rfc:2459");
        useKeyWithType.setIdentifier("CN=client");
        queryKeyBinding.getUseKeyWith().add(useKeyWithType);
        locateRequest.setQueryKeyBinding(queryKeyBinding);

        LocateResultType locateResultType = port.locate(locateRequest);
        assertTrue(locateResultType.getResultMajor().endsWith("Success"));
        assertTrue(locateResultType.getResultMinor().endsWith("NoMatch"));

        // Now register

        RegisterRequestType registerRequest = new RegisterRequestType();
        registerRequest.setId("_xyz");
        registerRequest.setService("http://cxf.apache.org/services/XKMS/");

        PrototypeKeyBindingType prototypeKeyBinding = new PrototypeKeyBindingType();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        InputStream certInputStream = ClassLoaderUtils.getResourceAsStream("xkmstest.cer", this.getClass());
        Certificate certificate =
            certificateFactory.generateCertificate(certInputStream);
        KeyInfoType keyInfo = X509Utils.getKeyInfo((X509Certificate)certificate);
        prototypeKeyBinding.setKeyInfo(keyInfo);

        prototypeKeyBinding.getUseKeyWith().add(useKeyWithType);
        registerRequest.setPrototypeKeyBinding(prototypeKeyBinding);

        RegisterResultType registerResult = port.register(registerRequest);
        assertTrue(registerResult.getResultMajor().endsWith("Success"));
        assertFalse(registerResult.getKeyBinding().isEmpty());

        // Now locate again - which should work

        locateResultType = port.locate(locateRequest);
        assertTrue(locateResultType.getResultMajor().endsWith("Success"));
        assertFalse(locateResultType.getUnverifiedKeyBinding().isEmpty());

        // Delete the certificate so that the test works when run again
        Path path = FileSystems.getDefault().getPath("target/test-classes/certs/xkms/CN-client.cer");
        Files.delete(path);

    }


    // The client uses XKMS to locate the public key of the service with which to encrypt
    // the message.
    @org.junit.Test
    public void testSymmetricBinding() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = XKMSTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = XKMSTest.class.getResource("DoubleItXKMS.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // The client uses XKMS to locate the public key of the service with which to encrypt
    // the message. Then the client uses XKMS to both locate + validate the signing cert
    // on processing the service response
    @org.junit.Test
    public void testAsymmetricBinding() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = XKMSTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = XKMSTest.class.getResource("DoubleItXKMS.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

}
