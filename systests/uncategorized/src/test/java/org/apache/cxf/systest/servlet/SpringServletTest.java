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

import javax.xml.ws.Endpoint;

import org.w3c.dom.Document;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.testsupport.AbstractServletTest;

import org.junit.Test;

public class SpringServletTest extends AbstractServletTest {
    @Override
    protected String getConfiguration() {
        return "/org/apache/cxf/systest/servlet/web-spring.xml";
    }

    @Override
    protected Bus createBus() throws BusException {
        // don't set up the bus, let the servlet do it
        return null;
    }

    @Test
    public void testInvokingSpringBeans() throws Exception {

        WebRequest req = new PostMethodWebRequest(CONTEXT_URL + "/services/Greeter",
            getClass().getResourceAsStream("GreeterMessage.xml"),
            "text/xml; charset=utf-8");

        invokingEndpoint(req);
        
        req = new PostMethodWebRequest(CONTEXT_URL + "/services/Greeter1",
            getClass().getResourceAsStream("GreeterMessage.xml"), "text/xml; charset=utf-8");
        
        invokingEndpoint(req);
    }
    
    public void invokingEndpoint(WebRequest req) throws Exception {
        
        WebResponse response = newClient().getResponse(req);
        assertEquals("text/xml", response.getContentType());
        assertTrue("utf-8".equalsIgnoreCase(response.getCharacterSet()));

        Document doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);

        addNamespace("h", "http://apache.org/hello_world_soap_http/types");
        assertValid("/s:Envelope/s:Body", doc);
        assertValid("//h:sayHiResponse", doc);
    }
    
    @Test
    public void testGreetMeGetRequest() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + "/services/Greeter/greetMe?"
                                         + "requestType=hello");
        
        WebResponse response = client.getResponse(req);        
        Document doc = DOMUtils.readXml(response.getInputStream());
        addNamespace("h", "http://apache.org/hello_world_soap_http/types");
        assertValid("/s:Envelope/s:Body", doc);
        assertValid("//h:greetMeResponse", doc);
        
        req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + "/services/Greeter1/greetMe?"
                                         + "requestType=hello");
        
        response = client.getResponse(req);        
        doc = DOMUtils.readXml(response.getInputStream());
        addNamespace("h", "http://apache.org/hello_world_soap_http/types");
        assertValid("/s:Envelope/s:Body", doc);
        assertValid("//h:greetMeResponse", doc);
    }
    
        
    @Test
    public void testGetWSDL() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + "/services/Greeter?wsdl"); 
       
        WebResponse res = client.getResponse(req);        
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        
        Document doc = DOMUtils.readXml(res.getInputStream());
        assertNotNull(doc);
        
        assertValid("//wsdl:operation[@name='greetMe']", doc);
        assertValid("//wsdlsoap:address[@location='" + CONTEXT_URL + "/services/Greeter']", doc);
        
        req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + "/services/Greeter2?wsdl");
        res = client.getResponse(req);    
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        
        doc = DOMUtils.readXml(res.getInputStream());
        assertNotNull(doc);
        
        assertValid("//wsdl:operation[@name='greetMe']", doc);
        assertValid("//wsdlsoap:address[@location='http://cxf.apache.org/Greeter']", doc);
        
        Endpoint.publish("/services/Greeter3", new org.apache.hello_world_soap_http.GreeterImpl());
        req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + "/services/Greeter3?wsdl");
        res = client.getResponse(req);    
        assertEquals(200, res.getResponseCode());
        assertEquals("text/xml", res.getContentType());
        
        doc = DOMUtils.readXml(res.getInputStream());
        assertNotNull(doc);
        
        assertValid("//wsdl:operation[@name='greetMe']", doc);
        assertValid("//wsdlsoap:address[@location='" + CONTEXT_URL + "/services/Greeter3']", doc);

    }
    
    @Test
    public void testGetServiceList() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);
        
        WebRequest req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + "/services/");
        WebResponse res = client.getResponse(req);
        assertEquals(200, res.getResponseCode());
        assertEquals("text/html", res.getContentType());
        assertEquals("Here should have no services links ", 0, res.getLinks().length);
                
    }
    
}
