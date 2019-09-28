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

package org.apache.cxf.jaxrs.ext.multipart;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContentDispositionTest {

    @Test
    public void testContentDisposition() {
        ContentDisposition cd = new ContentDisposition(" attachment ; bar=foo ; baz = baz1");
        assertEquals("attachment", cd.getType());
        assertEquals("foo", cd.getParameter("bar"));
        assertEquals("baz1", cd.getParameter("baz"));
    }
    @Test
    public void testContentDispositionWithQuotes() {
        ContentDisposition cd = new ContentDisposition(" attachment ; bar=\"foo.txt\" ; baz = baz1");
        assertEquals("attachment", cd.getType());
        assertEquals("foo.txt", cd.getParameter("bar"));
        assertEquals("baz1", cd.getParameter("baz"));
    }
    @Test
    public void testContentDispositionWithQuotesAndSemicolon() {
        ContentDisposition cd = new ContentDisposition(" attachment ; bar=\"foo;txt\" ; baz = baz1");
        assertEquals("attachment", cd.getType());
        assertEquals("foo;txt", cd.getParameter("bar"));
        assertEquals("baz1", cd.getParameter("baz"));
    }

    @Test
    public void testContentDispositionWithCreationDate() {
        ContentDisposition cd = new ContentDisposition(" attachment ; creation-date=\"21:08:08 14:00:00\"");
        assertEquals("attachment", cd.getType());
        assertEquals("21:08:08 14:00:00", cd.getParameter("creation-date"));
    }

}