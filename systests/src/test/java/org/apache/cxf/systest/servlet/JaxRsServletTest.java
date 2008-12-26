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

import java.io.InputStream;

import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.systest.jaxrs.JAXRSClientServerProxySpringBookTest;
import org.junit.Test;



public class JaxRsServletTest extends AbstractServletTest {
      

    @Override
    protected String getConfiguration() {
        return "/org/apache/cxf/systest/servlet/web-jaxrs.xml";
    }

    @Override
    protected Bus createBus() throws BusException {
        // don't set up the bus, let the servlet do it
        return null;
    }
    
    @Test
    public void testGetThatBook123() throws Exception {
        testInvokingBookService("/jaxrs/bookstorestorage/thosebooks/123");
    }   
    
    private void testInvokingBookService(String serviceAddress) throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(false);
        
        WebRequest req = 
            new GetMethodQueryWebRequest(CONTEXT_URL + serviceAddress);
        
        WebResponse response = client.getResponse(req);
        InputStream in = response.getInputStream();        
        InputStream expected = JAXRSClientServerProxySpringBookTest.class
            .getResourceAsStream("resources/expected_get_book123.txt");
        
        assertEquals(" Can't get the expected result ", 
                     getStringFromInputStream(expected),
                     getStringFromInputStream(in));
     
    }
    
    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();            
        return bos.getOut().toString();        
    }
}
