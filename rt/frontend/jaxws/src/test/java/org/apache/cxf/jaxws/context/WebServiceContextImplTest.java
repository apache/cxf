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

package org.apache.cxf.jaxws.context;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.cxf.message.MessageImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;


public class WebServiceContextImplTest extends Assert {

    @After
    public void tearDown() { 
        WebServiceContextImpl.clear();
    }    

    @Test
    public void testGetSetMessageContext() { 
        WebServiceContextImpl wsci = new WebServiceContextImpl(); 
        assertNull(wsci.getMessageContext());
        
        MessageImpl msg = new MessageImpl();
        final MessageContext ctx = new WrappedMessageContext(msg);
        WebServiceContextImpl.setMessageContext(ctx);

        assertSame(ctx, wsci.getMessageContext());

        Thread t = new Thread() { 
                public void run() {
                    WebServiceContextImpl threadLocalWSCI = new WebServiceContextImpl(); 

                    assertNull(threadLocalWSCI.getMessageContext());

                    MessageImpl msg1 = new MessageImpl();
                    MessageContext threadLocalCtx = new WrappedMessageContext(msg1); 
                    WebServiceContextImpl.setMessageContext(threadLocalCtx);


                    assertSame(threadLocalCtx, threadLocalWSCI.getMessageContext());
                    assertTrue(ctx !=  threadLocalWSCI.getMessageContext());
                    
                }
            };

        t.start(); 
        
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    // CXF-3989
    @Test
    public void testSetHttpRequestHeadersScope() {
        MessageImpl msg = new MessageImpl();
        MessageContext context = new WrappedMessageContext(msg);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> values = new ArrayList<String>();
        values.add("Value1");
        headers.put("Header1", values);
        context.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
        context.setScope(MessageContext.HTTP_REQUEST_HEADERS, Scope.APPLICATION);
    }
}
