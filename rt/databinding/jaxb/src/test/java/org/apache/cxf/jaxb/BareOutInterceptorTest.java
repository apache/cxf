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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.interceptor.BareOutInterceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_soap_http.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class BareOutInterceptorTest extends TestBase {

    BareOutInterceptor interceptor;
    
    private ByteArrayOutputStream baos;
    private XMLStreamWriter writer;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        interceptor = new BareOutInterceptor();
        baos =  new ByteArrayOutputStream();
        writer = getXMLStreamWriter(baos);
        message.setContent(XMLStreamWriter.class, writer);
        message.getExchange().put(BindingOperationInfo.class, operation);
        IMocksControl control = EasyMock.createNiceControl();
        InterceptorChain ic = control.createMock(InterceptorChain.class);
        message.setInterceptorChain(ic);
        control.replay();
    }

    @After
    public void tearDown() throws Exception {
        baos.close();
    }

    @Test
    public void testWriteOutbound() throws Exception {
        GreetMeResponse greetMe = new GreetMeResponse();
        greetMe.setResponseType("responseType");
        
        message.setContent(List.class, Arrays.asList(greetMe));

        interceptor.handleMessage(message);

        writer.close();
        
        assertNull(message.getContent(Exception.class));

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        //System.err.println(baos.toString());
        XMLStreamReader xr = StaxUtils.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "greetMeResponse"),
                     reader.getName());
        
        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "responseType"),
                     reader.getName());
    }

    @Test
    public void testWriteInbound() throws Exception {
        GreetMe greetMe = new GreetMe();
        greetMe.setRequestType("requestType");
        
        message.setContent(List.class, Arrays.asList(greetMe));
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        
        interceptor.handleMessage(message);
        
        writer.close();
        
        assertNull(message.getContent(Exception.class));

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader xr = StaxUtils.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "greetMe"),
                     reader.getName());
        
        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "requestType"),
                     reader.getName());
    }
}
