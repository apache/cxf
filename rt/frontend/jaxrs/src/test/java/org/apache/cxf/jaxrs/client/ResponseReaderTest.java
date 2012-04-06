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

package org.apache.cxf.jaxrs.client;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class ResponseReaderTest extends Assert {

    @Test
    public void testResponseReader() throws Exception {
        String data = "<Book><id>123</id><name>CXF in Action</name></Book>";
        MultivaluedMap<String, String> headers = new MetadataMap<String, String>();
        headers.add("a", "a1");
        headers.add("a", "a2");
        headers.add("b", "b1");
        
        final Message m = new MessageImpl();
        Exchange exc = new ExchangeImpl();
        exc.setInMessage(m);
        
        ProviderFactory instance = ProviderFactory.getInstance();
        
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        endpoint.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        endpoint.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(instance).anyTimes();
        EasyMock.replay(endpoint);
        
        exc.put(Endpoint.class, endpoint);
        
        m.setExchange(exc);
        m.put(Message.RESPONSE_CODE, "200");
        
        MessageBodyReader<Response> reader = new ResponseReader(Book.class) {
            protected MessageContext getContext() {
                return new MessageContextImpl(m);
            }
        };
        ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
        Response r = reader.readFrom(Response.class, Response.class,
            new Annotation[0], MediaType.TEXT_XML_TYPE, headers, is);
        
        assertNotNull(r);
        assertEquals(200, r.getStatus());
        Book b = (Book)r.getEntity();
        
        assertEquals(123, b.getId());
        assertEquals("CXF in Action", b.getName());
        
        MultivaluedMap<String, Object> respHeaders = r.getMetadata();
        assertNotSame(headers, respHeaders);
        assertEquals(headers, respHeaders);
    }
    
}
