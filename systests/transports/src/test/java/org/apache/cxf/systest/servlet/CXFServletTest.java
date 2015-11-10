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



import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import javax.jws.WebService;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPBinding;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_soap_http.BaseGreeterImpl;

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
        
        invoke(StandardCharsets.UTF_8.name());
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

        Document doc = StaxUtils.read(response.getInputStream());
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
        assertEquals("Wrong number of service links", 6, links.length);
        
        Set<String> links2 = new HashSet<String>();
        for (WebLink l : links) {
            links2.add(l.getURLString());
        }
        
        assertTrue(links2.contains(CONTEXT_URL + "/services/greeter?wsdl"));       
        assertTrue(links2.contains(CONTEXT_URL + "/services/greeter2?wsdl")); 
        assertTrue(links2.contains("http://cxf.apache.org/MyGreeter?wsdl")); 
        assertEquals("text/html", res.getContentType());
        
        res = client.getResponse(CONTEXT_URL + "/services/");
       
        
        links = res.getLinks();
        links2.clear();
        for (WebLink l : links) {
            links2.add(l.getURLString());
        }
        
        assertEquals("Wrong number of service links", 6, links.length);
        assertTrue(links2.contains(CONTEXT_URL + "/services/greeter?wsdl"));       
        assertTrue(links2.contains(CONTEXT_URL + "/services/greeter2?wsdl")); 
        assertTrue(links2.contains("http://cxf.apache.org/MyGreeter?wsdl")); 
        
        assertEquals("text/html", res.getContentType());
        
       
        // Ensure that the Bus is available for people doing an Endpoint.publish() or similar.
        assertNotNull(BusFactory.getDefaultBus(false));
    }
    
    @Test
    public void testGetUnformatServiceList() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(false);

        WebResponse res = client.getResponse(CONTEXT_URL + "/services?formatted=false");
        
        assertTrue(res.getText().contains("http://localhost/mycontext/services/greeter3"));
        assertTrue(res.getText().contains("http://localhost/mycontext/services/greeter2"));
        assertTrue(res.getText().contains("http://localhost/mycontext/services/greeter"));
        
    }
    
    @Test
    public void testServiceListWithLoopAddress() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(false);

        WebResponse res = client.getResponse(CONTEXT_URL + "/services");
        
        assertTrue(res.getText().contains("http://localhost/mycontext/services/greeter3"));
        assertTrue(res.getText().contains("http://localhost/mycontext/services/greeter2"));
        assertTrue(res.getText().contains("http://localhost/mycontext/services/greeter"));
        WebRequest req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter?wsdl");
        res = client.getResponse(req); 
        req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter2?wsdl");
        res = client.getResponse(req); 
        req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter3?wsdl");
        res = client.getResponse(req); 
        String loopAddr = "http://127.0.0.1/mycontext";
        res = client.getResponse(loopAddr + "/services");
        assertFalse(res.getText().contains(
             "http://127.0.0.1/mycontext/serviceshttp://localhost/mycontext/services/greeter"));
                
    }
    
    @Test
    public void testGetWSDL() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter?wsdl");
        
        WebResponse res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        Document doc = StaxUtils.read(res.getInputStream());
        assertNotNull(doc);
        
        assertValid("//wsdl:operation[@name='greetMe']", doc);
        assertValid("//wsdlsoap:address[@location='" + CONTEXT_URL + "/services/greeter']", doc);
    }
    
    
    @Test
    public void testGetWSDLWithMultiplePublishedEndpointUrl() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter5?wsdl");
        
        WebResponse res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        Document doc = StaxUtils.read(res.getInputStream());
        assertNotNull(doc);
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        
        
        assertValid("//wsdl:service[@name='SOAPService']/wsdl:port[@name='SoapPort']/wsdlsoap:address[@location='" 
            + "http://cxf.apache.org/publishedEndpointUrl1']", doc);
        assertValid("//wsdl:service[@name='SOAPService']/wsdl:port[@name='SoapPort1']/wsdlsoap:address[@location='" 
            + "http://cxf.apache.org/publishedEndpointUrl2']", doc);
        
    }
    @Test
    public void testGetWSDLWithIncludes() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter3?wsdl");
        
        WebResponse res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        Document doc = StaxUtils.read(res.getInputStream());
        assertNotNull(doc);
        
        assertXPathEquals("//xsd:include/@schemaLocation",
                          "http://localhost/mycontext/services/greeter3?xsd=hello_world_includes2.xsd",
                          doc.getDocumentElement());
        
        req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter3?xsd=hello_world_includes2.xsd");
        
        res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        doc = StaxUtils.read(res.getInputStream());
        assertNotNull(doc);

        assertValid("//xsd:complexType[@name='ErrorCode']", doc);
    }
    
    @Test
    public void testGetWSDLWithXMLBinding() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/greeter2?wsdl");
        
        WebResponse res = client.getResponse(req); 
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        
        Document doc = StaxUtils.read(res.getInputStream());
        assertNotNull(doc);
        
        addNamespace("http", "http://schemas.xmlsoap.org/wsdl/http/");
        assertValid("//wsdl:operation[@name='greetMe']", doc);
        NodeList addresses = assertValid("//http:address/@location", doc);
        boolean found = true;
        for (int i = 0; i < addresses.getLength(); i++) {
            String address = addresses.item(i).getLocalName();
            if (address.startsWith("http://localhost") && address.endsWith("/services/greeter2")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
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
        assertTrue(ep.getBinding() instanceof SOAPBinding);
    }

    @WebService(name = "Hello", portName = "HelloPort",
                serviceName = "HelloService", targetNamespace = "http://cxf.apache.org/hello")
    public static class HelloImpl {
        public String hello(String name) {
            return "Hello " + name;
        }
    }
    @WebService(serviceName = "SOAPService",
                portName = "SoapPort",
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http")
    public static class NoWsdlGreeter extends BaseGreeterImpl {


    }
}
