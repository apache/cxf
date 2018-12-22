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



package org.apache.cxf.systest.soap12;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap12_http.Greeter;
import org.apache.hello_world_soap12_http.PingMeFault;
import org.apache.hello_world_soap12_http.SOAPService;
import org.apache.hello_world_soap12_http.types.FaultDetail;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Soap12ClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;

    private final QName serviceName = new QName("http://apache.org/hello_world_soap12_http",
                                                "SOAPService");
    private final QName portName = new QName("http://apache.org/hello_world_soap12_http", "SoapPort");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testBasicConnection() throws Exception {
        Greeter greeter = getGreeter();
        for (int i = 0; i < 5; i++) {
            String echo = greeter.sayHi();
            assertEquals("Bonjour", echo);
        }

    }

    @Test
    public void testPingMeFault() throws Exception {
        Greeter greeter = getGreeter();
        try {
            greeter.pingMe();
            fail("Should throw Exception!");
        } catch (PingMeFault ex) {
            FaultDetail detail = ex.getFaultInfo();
            assertEquals((short)2, detail.getMajor());
            assertEquals((short)1, detail.getMinor());
            assertEquals("PingMeFault raised by server", ex.getMessage());
        }
    }
    String stripSpaces(String s) {
        String s2 = s.replace(" ", "");
        while (!s2.equals(s)) {
            s = s2;
            s2 = s.replace(" ", "");
        }
        return s2;
    }
    @Test
    public void testSayHiSoap12ToSoap11() throws Exception {
        HttpURLConnection httpConnection =
            getHttpConnection("http://localhost:" + PORT + "/SoapContext/Soap11Port/sayHi");
        httpConnection.setDoOutput(true);

        InputStream reqin = Soap12ClientServerTest.class.getResourceAsStream("sayHiSOAP12Req.xml");
        assertNotNull("could not load test data", reqin);

        httpConnection.setRequestMethod("POST");
        httpConnection.addRequestProperty("Content-Type", "text/xml;charset=utf-8");
        OutputStream reqout = httpConnection.getOutputStream();
        IOUtils.copy(reqin, reqout);
        reqout.close();

        assertEquals(500, httpConnection.getResponseCode());

        InputStream respin = httpConnection.getErrorStream();
        assertNotNull(respin);

        // we expect a soap 1.1 fault from the soap 1.1 test service that does not support soap 1.2
        assertEquals("text/xml;charset=utf-8", stripSpaces(httpConnection.getContentType().toLowerCase()));

        Document doc = StaxUtils.read(respin);
        assertNotNull(doc);

        Map<String, String> ns = new HashMap<>();
        ns.put("soap11", Soap11.SOAP_NAMESPACE);
        XPathUtils xu = new XPathUtils(ns);
        Node fault = (Node) xu.getValue("/soap11:Envelope/soap11:Body/soap11:Fault", doc, XPathConstants.NODE);
        assertNotNull(fault);
        String codev = (String) xu.getValue("//faultcode/text()",
                                            fault,
                                            XPathConstants.STRING);

        assertNotNull(codev);
        assertTrue("VersionMismatch expected", codev.endsWith("VersionMismatch"));
    }

    private Greeter getGreeter() throws NumberFormatException, MalformedURLException {
        URL wsdl = getClass().getResource("/wsdl/hello_world_soap12.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is ull ", service);

        Greeter g = service.getPort(portName, Greeter.class);
        updateAddressPort(g, PORT);
        return g;
    }

}

