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
package org.apache.cxf.systest.servlet;


import java.util.HashSet;
import java.util.Set;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.w3c.dom.Document;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.DOMUtils;
import org.junit.Before;
import org.junit.Test;


public class CXFServletTest extends AbstractServletTest {
    
    
    @Before
    public void setUp() throws Exception {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        super.setUp();
    }
    
    @Override
    protected Bus createBus() throws BusException {
        return null;
    }
    
    @Test
    public void testPostInvokeServices() throws Exception {
        
        invoke("UTF-8");
        invoke("iso-8859-1");        
    }

    private void invoke(String encoding) throws Exception {        
        WebRequest req = new PostMethodWebRequest(CONTEXT_URL + "/services/greeter", 
            getClass().getResourceAsStream("GreeterMessage.xml"), 
            "text/xml; charset=" + encoding);
        
        ServletUnitClient client = newClient();
        WebResponse response = client.getResponse(req);
        client.setExceptionsThrownOnErrorStatus(false);

        assertEquals("text/xml", response.getContentType());
        assertTrue(encoding.equalsIgnoreCase(response.getCharacterSet()));

        Document doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);

        addNamespace("h", "http://apache.org/hello_world_soap_http/types");

        assertValid("/s:Envelope/s:Body", doc);
        assertValid("//h:sayHiResponse", doc);    
    }
    
    @Test
    public void testGetServiceList() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(false);

        WebResponse res = client.getResponse(CONTEXT_URL + "/services");
        
        
        WebLink[] links = res.getLinks();
        assertEquals("There should get two links for the service", 2, links.length);
        
        Set<String> links2 = new HashSet<String>();
        for (WebLink l : links) {
            links2.add(l.getURLString());
        }
        
        assertTrue(links2.contains(CONTEXT_URL + "/services/greeter?wsdl"));       
        assertTrue(links2.contains(CONTEXT_URL + "/services/greeter2?wsdl")); 
        assertEquals("text/html", res.getContentType());
        
        res = client.getResponse(CONTEXT_URL + "/services/");
       
        
        links = res.getLinks();
        links2.clear();
        for (WebLink l : links) {
            links2.add(l.getURLString());
        }
        
        assertEquals("There should get two links for the service", 2, links.length);
        assertTrue(links2.contains(CONTEXT_URL + "/services/greeter?wsdl"));       
        assertTrue(links2.contains(CONTEXT_URL + "/services/greeter2?wsdl")); 
        
        assertEquals("text/html", res.getContentType());
        
       
        // Ensure that the Bus is available for people doing an Endpoint.publish() or similar.
        assertNotNull(BusFactory.getDefaultBus(false));
    }
    
    @Test
    public void testGetWSDL() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter?wsdl");
        
        WebResponse res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        Document doc = DOMUtils.readXml(res.getInputStream());
        assertNotNull(doc);
        
        assertValid("//wsdl:operation[@name='greetMe']", doc);
        assertValid("//wsdlsoap:address[@location='" + CONTEXT_URL + "/services/greeter']", doc);
    }
    
    @Test
    public void testGetWSDLWithXMLBinding() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter2?wsdl");
        
        WebResponse res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        
        Document doc = DOMUtils.readXml(res.getInputStream());
        assertNotNull(doc);
        
        addNamespace("http", "http://schemas.xmlsoap.org/wsdl/http/");
        assertValid("//wsdl:operation[@name='greetMe']", doc);
        assertValid("//http:address[@location='" + CONTEXT_URL + "/services/greeter2']", doc);
    }

    @Test
    public void testInvalidServiceUrl() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(false);

        WebResponse res = client.getResponse(CONTEXT_URL + "/services/NoSuchService");
        assertEquals(404, res.getResponseCode());
        assertEquals("text/html", res.getContentType());
    }

    @Test
    public void testServiceWsdlNotFound() throws Exception {
        WebRequest req = new GetMethodWebRequest(CONTEXT_URL + "/services/NoSuchService?wsdl");

        expectErrorCode(req, 404, "Response code 404 required for invalid WSDL url.");
    }
    
    @Test
    public void testGetImportedXSD() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);

        WebRequest req 
            = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter?wsdl");
        WebResponse res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        String text = res.getText();
        assertEquals("text/xml", res.getContentType());
        assertTrue(text.contains(CONTEXT_URL + "/services/greeter?wsdl=test_import.xsd"));

        req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter?wsdl=test_import.xsd");
        res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        text = res.getText();
        
        assertEquals("text/xml", res.getContentType());
        assertTrue("the xsd should contain the completType SimpleStruct",
                   text.contains("<complexType name=\"SimpleStruct\">"));
    }
    
    
    @Test
    public void testGetBinding() throws Exception {
        Endpoint ep = Endpoint.create("http://schemas.xmlsoap.org/wsdl/soap/http", new HelloImpl());
        System.out.println(ep.getBinding().getClass());
    }

    @WebService(name = "Hello", portName = "HelloPort",
                serviceName = "HelloService", targetNamespace = "http://cxf.apache.org/hello")
    public static class HelloImpl {
        public String hello(String name) {
            return "Hello " + name;
        }
    }
}
