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

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.utils.HttpUtils;

import org.junit.Assert;
import org.junit.Test;


public class ResponseBuilderImplTest extends Assert {

    
    @Test
    public void testLanguage() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Content-Language", "de");
        checkBuild(Response.ok().language("de").build(), 200, null, m);
    }
    
    @Test
    public void testLanguageReplace() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Content-Language", "en");
        checkBuild(Response.ok().language("de").language((Locale)null)
                   .language("en").build(), 200, null, m);
    }
    
    @Test
    public void testAddHeader() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Content-Language", "en");
        checkBuild(Response.ok().header(HttpHeaders.CONTENT_LANGUAGE, "de")
                                .header(HttpHeaders.CONTENT_LANGUAGE, null)
                                .header(HttpHeaders.CONTENT_LANGUAGE, "en").build(), 
                  200, null, m);
    }
    
    @Test
    public void testAddCookie() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Set-Cookie", "a=b");
        m.add("Set-Cookie", "c=d");
        checkBuild(Response.ok().cookie(new NewCookie("a", "b"))
                                .cookie(new NewCookie("c", "d")).build(), 
                  200, null, m);
    }
    
    @Test
    public void testExpires() throws Exception {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Expires", "Tue, 21 Oct 2008 17:00:00 GMT");
        SimpleDateFormat format = HttpUtils.getHttpDateFormat();
        Date date = format.parse("Tue, 21 Oct 2008 17:00:00 GMT");
        checkBuild(Response.ok().expires(date).build(), 200, null, m);
    }
    
    @Test
    public void testOkBuild() {
      
        checkBuild(Response.ok().build(),
                          200, null, new MetadataMap<String, Object>());
        
    }
    
    @Test
    public void testCreatedNoEntity() throws Exception {
        
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Location", "http://foo");
        
        checkBuild(Response.created(new URI("http://foo")).build(),
                   201, null, m);
        
        
    }
    
    private void checkBuild(Response r, int status, Object entity, 
                            MetadataMap<String, Object> meta) {
        ResponseImpl ri = (ResponseImpl)r;
        assertEquals("Wrong status", ri.getStatus(), status);
        assertSame("Wrong entity", ri.getEntity(), entity);
        assertEquals("Wrong meta", ri.getMetadata(), meta);
    }
    
}
