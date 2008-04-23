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



import org.w3c.dom.Document;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.helpers.DOMUtils;
import org.junit.Test;



public class HttpBindingServletTest extends AbstractServletTest {
    static final String JSON_CUSTOMER = "{\"jra.customer\":"
              + "{\"jra.id\":123,\"jra.name\":\"Dan Diephouse\"}}";
   

    @Override
    protected String getConfiguration() {
        return "/org/apache/cxf/systest/servlet/web-restful.xml";
    }

    @Override
    protected Bus createBus() throws BusException {
        // don't set up the bus, let the servlet do it
        return null;
    }
    
    @Test
    public void testServerFactoryRestService() throws Exception {
        testInvokingRestService("/services/serverFactory/restful");
    }
    
    @Test
    public void testEndpointRestService() throws Exception {
        testInvokingRestService("/services/endpoint/restful");
    }
    
    @Test
    public void testJsonService() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(false);
        
        WebRequest req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + "/services/serverFactory/json/customers");
        WebResponse response = client.getResponse(req);
        assertTrue("Can't get the right Json customers ", response.getText().indexOf(JSON_CUSTOMER) > 0);
        
        req = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/serverFactory/json/customers/123");
        response = client.getResponse(req);        
        assertEquals("Can't get the right Json customer ", response.getText(), JSON_CUSTOMER);
    }
    
    
    private void testInvokingRestService(String serviceAddress) throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(false);
        
        WebRequest req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + serviceAddress + "/customers");
        
        WebResponse response = client.getResponse(req);        
        Document doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);

        addNamespace("c", "http://cxf.apache.org/jra");
        assertValid("/c:customers", doc);
        assertValid("/c:customers/c:customer/c:id[text()='123']", doc);
        assertValid("/c:customers/c:customer/c:name[text()='Dan Diephouse']", doc);
        
        req = new GetMethodQueryWebRequest(CONTEXT_URL + serviceAddress + "/customers/123");
        response = client.getResponse(req);        
        doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);
        
        assertValid("/c:customer", doc);
        assertValid("/c:customer/c:id[text()='123']", doc);
        assertValid("/c:customer/c:name[text()='Dan Diephouse']", doc);
        
        // Try invalid customer
        req = new GetMethodQueryWebRequest(CONTEXT_URL + serviceAddress + "/customers/0");
        response = client.getResponse(req); 
        
        assertEquals("Expect the wrong response code", response.getResponseCode(), 500);
        doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);
        
        assertValid("//c:CustomerNotFoundFault", doc);
        
        PostMethodWebRequest postReq = 
            new PostMethodWebRequest(CONTEXT_URL + serviceAddress + "/customers", 
                                 getClass().getResourceAsStream("add.xml"),
                                 "text/xml; charset=UTF-8");
        response = client.getResponse(postReq);
        doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);
        assertValid("/c:addCustomer", doc);
        
        PutMethodWebRequest putReq = 
            new PutMethodWebRequest(CONTEXT_URL + serviceAddress + "/customers/123", 
                                 getClass().getResourceAsStream("update.xml"),
                                 "text/xml; charset=UTF-8");
        response = client.getResponse(putReq);
        doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);       
        assertValid("/c:updateCustomer", doc);
      
        // Get the updated document
        req = new GetMethodQueryWebRequest(CONTEXT_URL + serviceAddress + "/customers/123");
        response = client.getResponse(req);
        doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);  
        
        assertValid("/c:customer", doc);
        assertValid("/c:customer/c:id[text()='123']", doc);
        assertValid("/c:customer/c:name[text()='Danno Manno']", doc);
    }
}
