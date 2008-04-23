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

package org.apache.cxf.jaxb;

import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.WrappedInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.junit.Test;

public class WrappedInInterceptorTest extends TestBase {
    
    @Test
    public void testInterceptorInbound() throws Exception {
        WrappedInInterceptor interceptor = new WrappedInInterceptor();

        message.setContent(XMLStreamReader.class, XMLInputFactory.newInstance()
            .createXMLStreamReader(getTestStream(getClass(), "resources/GreetMeDocLiteralReq.xml")));

        interceptor.handleMessage(message);

        assertNull(message.getContent(Exception.class));
        BindingOperationInfo op = (BindingOperationInfo)message.getExchange().get(BindingOperationInfo.class
                                                                                      .getName());
        assertNotNull(op);

        List<?> objs = message.getContent(List.class);
        assertTrue(objs != null && objs.size() > 0);
        Object obj = objs.get(0);
        assertTrue(obj instanceof org.apache.hello_world_soap_http.types.GreetMe);
        org.apache.hello_world_soap_http.types.GreetMe gm
            = (org.apache.hello_world_soap_http.types.GreetMe)obj;

        assertEquals("TestSOAPInputPMessage", gm.getRequestType());
    }

    @Test
    public void testInterceptorOutbound() throws Exception {
        WrappedInInterceptor interceptor = new WrappedInInterceptor();

        message.setContent(XMLStreamReader.class, XMLInputFactory.newInstance()
            .createXMLStreamReader(getTestStream(getClass(), "resources/GreetMeDocLiteralResp.xml")));
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);

        interceptor.handleMessage(message);
        assertNull(message.getContent(Exception.class));

        List<?> objs = message.getContent(List.class);
        assertTrue(objs != null && objs.size() > 0);

        Object retValue = objs.get(0);
        assertNotNull(retValue);

        assertTrue(retValue instanceof org.apache.hello_world_soap_http.types.GreetMeResponse);

        org.apache.hello_world_soap_http.types.GreetMeResponse gm
            = (org.apache.hello_world_soap_http.types.GreetMeResponse)retValue;
        assertEquals("TestSOAPOutputPMessage", gm.getResponseType());
    }
}
