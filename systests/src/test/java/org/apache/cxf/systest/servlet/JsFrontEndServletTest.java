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
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import org.apache.cxf.helpers.DOMUtils;
import org.junit.Test;

public class JsFrontEndServletTest extends AbstractServletTest {
    
    protected String getConfiguration() {
        return "/org/apache/cxf/systest/servlet/web-js.xml";
    }

    @Test
    public void testPostInvokeServices() throws Exception {
                
        WebRequest req = new PostMethodWebRequest(CONTEXT_URL + "/services/Greeter",
                getClass().getResourceAsStream("GreeterMessage.xml"),
                "text/xml; charset=UTF-8");
        
        WebResponse response = newClient().getResponse(req);

        assertEquals("text/xml", response.getContentType());
        //assertEquals("UTF-8", response.getCharacterSet());

        Document doc = DOMUtils.readXml(response.getInputStream());
        assertNotNull(doc);
        
        addNamespace("h", "http://apache.org/hello_world_soap_http/types");
        
        assertValid("/s:Envelope/s:Body", doc);
        assertValid("//h:sayHiResponse", doc);
        assertValid("//h:responseType", doc);
        
    }
    

}
