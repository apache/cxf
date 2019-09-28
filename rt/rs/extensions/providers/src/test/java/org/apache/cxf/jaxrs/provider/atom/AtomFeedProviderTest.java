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

package org.apache.cxf.jaxrs.provider.atom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.stax.FOMFeed;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AtomFeedProviderTest {

    private AtomFeedProvider afd;

    @Before
    public void setUp() {
        afd = new AtomFeedProvider();
    }

    @Test
    public void testReadFrom() throws Exception {
        InputStream is = getClass().getResourceAsStream("atomFeed.xml");
        Feed simple = afd.readFrom(Feed.class, null, null, null, null, is);
        assertEquals("Wrong feed title", "Example Feed", simple.getTitle());

    }

    @Test
    public void testWriteTo() throws Exception {
        InputStream is = getClass().getResourceAsStream("atomFeed.xml");
        Feed simple = afd.readFrom(Feed.class, null, null, null, null, is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        afd.writeTo(simple, null, null, null, null, null, bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Feed simpleCopy = afd.readFrom(Feed.class, null, null, null, null, bis);
        assertEquals("Wrong entry title",
                     "Example Feed", simpleCopy.getTitle());
        assertEquals("Wrong entry title",
                     simple.getTitle(), simpleCopy.getTitle());
    }

    @Test
    public void testWriteable() {
        assertTrue(afd.isWriteable(Feed.class, null, null, null));
        assertTrue(afd.isWriteable(FOMFeed.class, null, null, null));
        assertFalse(afd.isWriteable(Entry.class, null, null, null));
    }

    @Test
    public void testReadable() {
        assertTrue(afd.isReadable(Feed.class, null, null, null));
        assertTrue(afd.isReadable(FOMFeed.class, null, null, null));
        assertFalse(afd.isReadable(Entry.class, null, null, null));
    }

    @Test
    public void testAnnotations() {
        String[] values = afd.getClass().getAnnotation(Produces.class).value();
        assertEquals("3 types can be produced", 3, values.length);
        assertTrue("application/atom+xml".equals(values[0])
                   && "application/atom+xml;type=feed".equals(values[1])
                   && "application/json".equals(values[2]));
        values = afd.getClass().getAnnotation(Consumes.class).value();
        assertEquals("2 types can be consumed", 2, values.length);
        assertTrue("application/atom+xml".equals(values[0])
                   && "application/atom+xml;type=feed".equals(values[1]));
    }

}