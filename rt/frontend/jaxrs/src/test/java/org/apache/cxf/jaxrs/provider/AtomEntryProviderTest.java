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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.stax.FOMEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AtomEntryProviderTest extends Assert {

    private AtomEntryProvider afd;
    
    @Before
    public void setUp() {
        afd = new AtomEntryProvider();
    }
    
    @Test
    public void testReadFrom() throws Exception {
        InputStream is = getClass().getResourceAsStream("atomEntry.xml");
        Entry simple = afd.readFrom(Entry.class, null, null, null, null, is);
        assertEquals("Wrong entry title", 
                     "Atom-Powered Robots Run Amok", simple.getTitle());
        
    }
    
    @Test
    public void testWriteTo() throws Exception {
        InputStream is = getClass().getResourceAsStream("atomEntry.xml");
        Entry simple = afd.readFrom(Entry.class, null, 
            null, MediaType.valueOf("application/atom+xml;type=entry"), null, is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        afd.writeTo(simple, null, null, null, 
            MediaType.valueOf("application/atom+xml;type=entry"), null, bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Entry simpleCopy = afd.readFrom(Entry.class, null, 
            null, MediaType.valueOf("application/atom+xml"), null, bis);
        assertEquals("Wrong entry title", 
                     "Atom-Powered Robots Run Amok", simpleCopy.getTitle());
        assertEquals("Wrong entry title", 
                     simple.getTitle(), simpleCopy.getTitle());
    }
    
    @Test
    public void testWriteable() {
        assertTrue(afd.isWriteable(Entry.class, null, null, null));
        assertTrue(afd.isWriteable(FOMEntry.class, null, null, null));
        assertFalse(afd.isWriteable(Feed.class, null, null, null));
    }
    
    @Test
    public void testReadable() {
        assertTrue(afd.isReadable(Entry.class, null, null, null));
        assertTrue(afd.isReadable(FOMEntry.class, null, null, null));
        assertFalse(afd.isReadable(Feed.class, null, null, null));
    }
    
    @Test
    public void testAnnotations() {
        String[] values = afd.getClass().getAnnotation(Produces.class).value();
        assertEquals("3 types can be produced", 3, values.length);
        assertTrue("application/atom+xml".equals(values[0])
                   && "application/atom+xml;type=entry".equals(values[1])
                   && "application/json".equals(values[2]));
        values = afd.getClass().getAnnotation(Consumes.class).value();
        assertEquals("2 types can be consumed", 2, values.length);
        assertTrue("application/atom+xml".equals(values[0])
                   && "application/atom+xml;type=entry".equals(values[1]));
    }
    
}
