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

package org.apache.cxf.jaxrs.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RequestImplTest extends Assert {
    
    private Message m;
    private MultivaluedMap<String, String> metadata;
    
    @Before
    public void setUp() {
        m = new MessageImpl();
        metadata = new MetadataMap<String, String>();
        m.put(Message.PROTOCOL_HEADERS, metadata);
    }
    
    @After
    public void tearUp() {
        m = null;
        metadata.clear();
    }
    
    @Test
    public void testWeakEtags() {
        metadata.putSingle("If-Match", new EntityTag("123", true).toString());
        
        ResponseBuilder rb = 
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertNotNull("Strict compararison is required", rb);
        Response r = rb.build();
        assertEquals("If-Match precondition was not met", 412, r.getStatus());
        assertEquals("Response should include ETag", 
                     "\"123\"", r.getMetadata().getFirst("ETag"));
    }
    
    @Test
    public void testStrictEtags() {
        metadata.putSingle("If-Match", new EntityTag("123").toString());
        
        ResponseBuilder rb = 
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertNull("Precondition must be met", rb);
    }
    
    @Test
    public void testStarEtags() {
        metadata.putSingle("If-Match", "*");
        
        ResponseBuilder rb = 
            new RequestImpl(m).evaluatePreconditions(new EntityTag("123"));
        assertNull("Precondition must be met", rb);
    }
    
    @Test
    public void testBeforeDate() throws Exception {
        metadata.putSingle("If-Modified-Since", "Tue, 21 Oct 2008 14:00:00 GMT");
        Date serverDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
            .parse("Tue, 21 Oct 2008 17:00:00 GMT");
        
        ResponseBuilder rb = 
            new RequestImpl(m).evaluatePreconditions(serverDate);
        assertNull("Precondition must be met", rb);
    }
    
    @Test
    public void testAfterDate() throws Exception {
        metadata.putSingle("If-Modified-Since", "Tue, 21 Oct 2008 14:00:00 GMT");
        Date serverDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
            .parse("Mon, 20 Oct 2008 14:00:00 GMT");
        
        
        ResponseBuilder rb = 
            new RequestImpl(m).evaluatePreconditions(serverDate);
        assertNotNull("Precondition is not met", rb);
        
        Response r = rb.build();
        assertEquals("If-Modified-Since precondition was not met", 304, r.getStatus());
    }
}
