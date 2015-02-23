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

package org.apache.cxf.jaxrs.provider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.provider.atom.AtomPojoProvider;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.TagVO;
import org.apache.cxf.message.MessageImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProviderFactoryAllTest extends Assert {

    @Before
    public void setUp() {
        ServerProviderFactory.getInstance().clearProviders();
    }
    
    @Test
    public void testAtomPojoProvider() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        AtomPojoProvider provider = new AtomPojoProvider();
        pf.registerUserProvider(provider);
        MessageBodyReader<?> feedReader = pf.createMessageBodyReader(Book.class,
                                               Book.class, null, 
                                               MediaType.valueOf("application/atom+xml"), 
                                               new MessageImpl());
        assertSame(feedReader, provider);
        
        MessageBodyReader<?> entryReader = pf.createMessageBodyReader(TagVO.class, 
                                               TagVO.class, null, 
                                               MediaType.valueOf("application/atom+xml;type=entry"), 
                                               new MessageImpl());
        assertSame(entryReader, provider);
    }
    
    
    @Test
    public void testCustomJsonProvider() {
        ProviderFactory pf = ServerProviderFactory.getInstance();
        JSONProvider<Book> provider = new JSONProvider<Book>();
        pf.registerUserProvider(provider);
        MessageBodyReader<?> customJsonReader = pf.createMessageBodyReader(Book.class, null, null, 
                                               MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertSame(customJsonReader, provider);
        
        MessageBodyWriter<?> customJsonWriter = pf.createMessageBodyWriter(Book.class, null, null, 
                                               MediaType.APPLICATION_JSON_TYPE, new MessageImpl());
        assertSame(customJsonWriter, provider);
    }
    
    private void verifyProvider(ProviderFactory pf, Class<?> type, Class<?> provider, String mediaType) 
        throws Exception {
        
        if (pf == null) {
            pf = ServerProviderFactory.getInstance();
        }
        
        MediaType mType = MediaType.valueOf(mediaType);
        
        MessageBodyReader<?> reader = pf.createMessageBodyReader(type, type, null, mType, new MessageImpl());
        assertSame("Unexpected provider found", provider, reader.getClass());
    
        MessageBodyWriter<?> writer = pf.createMessageBodyWriter(type, type, null, mType, new MessageImpl());
        assertTrue("Unexpected provider found", provider == writer.getClass());
    }
    
    
    private void verifyProvider(Class<?> type, Class<?> provider, String mediaType) 
        throws Exception {
        verifyProvider(null, type, provider, mediaType);
        
    }
       
    @Test
    public void testGetJSONProviderConsumeMime() throws Exception {
        verifyProvider(org.apache.cxf.jaxrs.resources.Book.class, JSONProvider.class, 
                       "application/json");
    }
    
}
