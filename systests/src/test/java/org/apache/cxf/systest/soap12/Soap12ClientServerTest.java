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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap12_http.Greeter;
import org.apache.hello_world_soap12_http.PingMeFault;
import org.apache.hello_world_soap12_http.SOAPService;
import org.apache.hello_world_soap12_http.types.FaultDetail;
import org.junit.BeforeClass;
import org.junit.Test;

public class Soap12ClientServerTest extends AbstractBusClientServerTestBase {    

    private final QName serviceName = new QName("http://apache.org/hello_world_soap12_http",
                                                "SOAPService");
    private final QName portName = new QName("http://apache.org/hello_world_soap12_http", "SoapPort");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
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
    
    @Test
    public void testGetSayHi() throws Exception {
        HttpURLConnection httpConnection = 
            getHttpConnection("http://localhost:9012/SoapContext/SoapPort/sayHi");    
        httpConnection.connect();        
        
        InputStream in = httpConnection.getInputStream();        
        assertNotNull(in);
        assertEquals("application/soap+xml; charset=utf-8", httpConnection.getContentType().toLowerCase());
       
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("soap12", Soap12.SOAP_NAMESPACE);
        ns.put("ns2", "http://apache.org/hello_world_soap12_http/types");
        XPathUtils xu = new XPathUtils(ns);
        Node body = (Node) xu.getValue("/soap12:Envelope/soap12:Body", doc, XPathConstants.NODE);
        assertNotNull(body);
        String response = (String) xu.getValue("//ns2:sayHiResponse/ns2:responseType/text()", 
                                               body, 
                                               XPathConstants.STRING);
        assertEquals("Bonjour", response);
    }

    @Test
    public void testGetPingMe() throws Exception  {
        HttpURLConnection httpConnection = 
            getHttpConnection("http://localhost:9012/SoapContext/SoapPort/pingMe");    
        httpConnection.connect();
        
        assertEquals(500, httpConnection.getResponseCode());
        
        assertEquals("Internal Server Error", httpConnection.getResponseMessage());

        InputStream in = httpConnection.getErrorStream();
        assertNotNull(in);     
        assertEquals("application/soap+xml; charset=utf-8", httpConnection.getContentType().toLowerCase());
        
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);        

        Map<String, String> ns = new HashMap<String, String>();
        ns.put("s", Soap12.SOAP_NAMESPACE);
        ns.put("ns2", "http://apache.org/hello_world_soap12_http/types");
        XPathUtils xu = new XPathUtils(ns);
        String codeValue  = (String) xu.getValue("/s:Envelope/s:Body/s:Fault/s:Code/s:Value/text()", 
                                                 doc, 
                                                 XPathConstants.STRING);
       
        assertEquals("soap:Receiver", codeValue);
        String reason = (String) xu.getValue("//s:Reason//text()", 
                                             doc, 
                                             XPathConstants.STRING);
        assertEquals("PingMeFault raised by server", reason);
        
        String minor = (String) xu.getValue("//s:Detail//ns2:faultDetail/ns2:minor/text()", 
                                               doc, 
                                               XPathConstants.STRING);
        assertEquals("1", minor);
        String major = (String) xu.getValue("//s:Detail//ns2:faultDetail/ns2:major/text()", 
                                            doc, 
                                            XPathConstants.STRING);
        assertEquals("2", major);
    }
    
    @Test
    public void testGetMethodNotExist() throws Exception  {
        HttpURLConnection httpConnection = 
            getHttpConnection("http://localhost:9012/SoapContext/SoapPort/greetMe");
        httpConnection.connect();
        
        assertEquals(500, httpConnection.getResponseCode());

        assertEquals("application/soap+xml; charset=utf-8", httpConnection.getContentType().toLowerCase());
        
        assertEquals("Internal Server Error", httpConnection.getResponseMessage());

        InputStream in = httpConnection.getErrorStream();                  
        assertNotNull(in);        
            
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("s", Soap12.SOAP_NAMESPACE);
        ns.put("ns2", "http://apache.org/hello_world_soap12_http/types");
        XPathUtils xu = new XPathUtils(ns);
        String codeValue  = (String) xu.getValue("/s:Envelope/s:Body/s:Fault/s:Code/s:Value/text()", 
                                                 doc, 
                                                 XPathConstants.STRING);
       
        assertEquals("soap:Receiver", codeValue);
        String reason = (String) xu.getValue("//s:Reason//text()", 
                                             doc, 
                                             XPathConstants.STRING);
        assertTrue(reason.contains("No such operation: greetMe"));        
    }
    

    private Greeter getGreeter() {
        URL wsdl = getClass().getResource("/wsdl/hello_world_soap12.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is ull ", service);
        
        return service.getPort(portName, Greeter.class);
    }

}

