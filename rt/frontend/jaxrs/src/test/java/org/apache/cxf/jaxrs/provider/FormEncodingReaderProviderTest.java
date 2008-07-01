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

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FormEncodingReaderProviderTest extends Assert {

    private FormEncodingReaderProvider ferp;

    @Before
    public void setUp() {
        ferp = new FormEncodingReaderProvider();
    }

    @Test
    public void testReadFrom() throws Exception {
        InputStream is = getClass().getResourceAsStream("singleValPostBody.txt");
        MultivaluedMap<String, String> mvMap = 
            ferp.readFrom(Object.class, null, null, null, null, is);
        assertEquals("Wrong entry for foo", "bar", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "far", mvMap.getFirst("boo"));

    }

    @Test
    public void testReadFromMultiples() throws Exception {
        InputStream is = getClass().getResourceAsStream("multiValPostBody.txt");
        MultivaluedMap<String, String> mvMap = 
            ferp.readFrom(Object.class, null, null, null, null, is);
        List<String> vals = mvMap.get("foo");

        assertEquals("Wrong size for foo params", 2, vals.size());
        assertEquals("Wrong size for foo params", 1, mvMap.get("boo").size());
        assertEquals("Wrong entry for foo 0", "bar", vals.get(0));
        assertEquals("Wrong entry for foo 1", "bar2", vals.get(1));
        assertEquals("Wrong entry for boo", "far", mvMap.getFirst("boo"));

    }

    @Test
    public void testReadable() {
        assertTrue(ferp.isReadable(MultivaluedMap.class, null, null));
    }

    @Test
    public void testAnnotations() {
        assertEquals("application/x-www-form-urlencoded", ferp.getClass().getAnnotation(ConsumeMime.class)
                     .value()[0]);
    }

}
