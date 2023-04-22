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

import jakarta.ws.rs.core.Link;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LinkHeaderProviderTest {

    @Test
    public void testFromSimpleString() {
        Link l = Link.valueOf("<http://bar>");
        assertEquals("http://bar", l.getUri().toString());
    }

    @Test
    public void testFromSimpleString2() {
        Link l = Link.valueOf("</>");
        assertEquals("/", l.getUri().toString());
    }

    @Test
    public void testFromComplexString() {
        Link l = Link.valueOf("<http://bar>;rel=next;title=\"Next Link\";type=text/xml;method=get");
        assertEquals("http://bar", l.getUri().toString());
        String rel = l.getRel();
        assertEquals("next", rel);
        assertEquals("Next Link", l.getTitle());
        assertEquals("text/xml", l.getType());
        assertEquals("get", l.getParams().get("method"));
    }

    @Test
    public void testToString() {
        String headerValue = "<http://bar>;rel=next;title=\"Next Link\";type=text/xml;method=get";
        String expected = "<http://bar>;rel=\"next\";title=\"Next Link\";type=\"text/xml\";method=\"get\"";
        Link l = Link.valueOf(headerValue);
        String result = l.toString();
        assertEquals(expected, result);
    }


}