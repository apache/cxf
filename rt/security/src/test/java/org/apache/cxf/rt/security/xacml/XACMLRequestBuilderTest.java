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

package org.apache.cxf.rt.security.xacml;

import java.security.Principal;
import java.util.Collections;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.opensaml.xacml.ctx.RequestType;


/**
 * Some unit tests to create a XACML Request via the XACMLRequestBuilder interface.
 */
public class XACMLRequestBuilderTest extends org.junit.Assert {
    
    static {
        OpenSAMLUtil.initSamlEngine();
    }

    @org.junit.Test
    public void testXACMLRequestBuilder() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };
        
        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, operation);
        
        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request = 
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);
    }
    
    @org.junit.Test
    public void testResource() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };
        
        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, operation);
        
        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request = 
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request); 
        
        assertTrue(builder.getResources(msg).contains(operation));
        
        operation = "user/list.json";
        msg = new MessageImpl();
        msg.put(Message.REQUEST_URI, operation);
        
        request = builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request); 
        
        assertTrue(builder.getResources(msg).contains(operation));
        
        operation = "https://localhost:8080/user/list.json";
        msg = new MessageImpl();
        msg.put(Message.REQUEST_URL, operation);
        
        ((DefaultXACMLRequestBuilder)builder).setSendFullRequestURL(true);
        request = builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request); 
        
        assertTrue(builder.getResources(msg).contains(operation));
    }
    
    @org.junit.Test
    public void testAction() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };
        
        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, operation);
        
        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request = 
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request); 
        
        String action = 
            request.getAction().getAttributes().get(0).getAttributeValues().get(0).getValue();
        assertEquals(action, "execute");
        
        ((DefaultXACMLRequestBuilder)builder).setAction("write");
        request = builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request); 
        
        action = 
            request.getAction().getAttributes().get(0).getAttributeValues().get(0).getValue();
        assertEquals(action, "write");
    }
    
    @org.junit.Test
    public void testEnvironment() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };
        
        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, operation);
        
        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request = 
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);
        assertFalse(request.getEnvironment().getAttributes().isEmpty());
        
        ((DefaultXACMLRequestBuilder)builder).setSendDateTime(false);
        request = builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);
        assertTrue(request.getEnvironment().getAttributes().isEmpty());
    }
    
}
