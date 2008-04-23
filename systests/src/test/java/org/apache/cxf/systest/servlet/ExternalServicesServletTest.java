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

import org.w3c.dom.Document;


import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.helpers.DOMUtils;
import org.junit.Test;

public class ExternalServicesServletTest extends AbstractServletTest {
    static final String FORCED_BASE_ADDRESS 
        = "http://localhost/somewhere";
    @Override
    protected Bus createBus() throws BusException {
        return null;
    }

    @Override
    protected String getConfiguration() {
        return "/org/apache/cxf/systest/servlet/web-external.xml";
    }
    
    @Test
    public void testGetServiceList() throws Exception {
        
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(false);

        //test the '/' context get service list
        WebResponse  res = client.getResponse(CONTEXT_URL + "/");
        WebLink[] links = res.getLinks();
        assertEquals("There should get two links for the services", 2, links.length);
        
        Set<String> links2 = new HashSet<String>();
        for (WebLink l : links) {
            links2.add(l.getURLString());
        }
        assertTrue(links2.contains(FORCED_BASE_ADDRESS + "/greeter?wsdl"));       
        assertTrue(links2.contains(FORCED_BASE_ADDRESS + "/greeter2?wsdl")); 
        
        assertEquals("text/html", res.getContentType());
        
        //HTTPUnit do not support require url with ""
        /*
        res = client.getResponse(CONTEXT_URL);
        links = res.getLinks();
        assertEquals("There should get two links for the services", 1, links.length);
        assertEquals(CONTEXT_URL + "/greeter?wsdl", links[0].getURLString());
        assertEquals(CONTEXT_URL + "/greeter2?wsdl", links[1].getURLString()); 
        assertEquals("text/html", res.getContentType());*/        
        
    }

    @Test
    public void testPostInvokeServices() throws Exception {
        newClient();
        
        WebRequest req = new PostMethodWebRequest(CONTEXT_URL + "/greeter",
                getClass().getResourceAsStream("GreeterMessage.xml"),
                "text/xml; charset=UTF-8");
        
        WebResponse response = newClient().getResponse(req);

        assertEquals("text/xml", response.getContentType());
        assertEquals("UTF-8", response.getCharacterSet());

        Document doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);
        
        addNamespace("h", "http://apache.org/hello_world_soap_http/types");
        
        assertValid("/s:Envelope/s:Body", doc);
        assertValid("//h:sayHiResponse", doc);
    }
}
