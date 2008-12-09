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

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.message.Message;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HttpHeadersImplTest extends Assert {
    
    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
    @Test
    public void testGetHeaders() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        EasyMock.expectLastCall().andReturn(createHeaders());
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        MultivaluedMap<String, String> hs = h.getRequestHeaders();
        List<String> acceptValues = hs.get("Accept");
        assertEquals("text/*;q=1", acceptValues.get(0));
        assertEquals("application/xml", acceptValues.get(1));
        assertEquals(hs.getFirst("Content-Type"), "*/*");
    }

    public void testGetHeader() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        EasyMock.expectLastCall().andReturn(createHeaders());
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        List<String> acceptValues = h.getRequestHeader("Accept");
        assertEquals(2, acceptValues.size());
        assertEquals("text/*;q=1", acceptValues.get(0));
        assertEquals("application/xml", acceptValues.get(1));
        List<String> contentValues = h.getRequestHeader("Content-Type");
        assertEquals(1, contentValues.size());
        assertEquals("*/*", contentValues.get(0));
        
    }
    
    @Test
    public void testGetNullLanguage() throws Exception {
        
        Message m = control.createMock(Message.class);
        m.get(Message.PROTOCOL_HEADERS);
        EasyMock.expectLastCall().andReturn(createHeaders());
        control.replay();
        HttpHeaders h = new HttpHeadersImpl(m);
        assertEquals("UTF-8", h.getLanguage());
    }
    
        
    private Map<String, List<String>> createHeaders() {
        MetadataMap<String, String> hs = new MetadataMap<String, String>();
        hs.putSingle("Accept", "text/*;q=1,application/xml");
        hs.putSingle("Content-Type", "*/*");
        return hs;
    }
}
