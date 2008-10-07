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

import org.junit.Assert;
import org.junit.Test;

public class UriBuilderImplTest extends Assert {
    
    @Test
    public void testUri() throws Exception {
        URI uri = new URI("http://foo/bar/baz?query=1#fragment");
        URI newUri = new UriBuilderImpl().uri(uri).build();
        assertEquals("URI is not built correctly", newUri, uri);
    }
    
    @Test
    public void testAddPath() throws Exception {
        URI uri = new URI("http://foo/bar");
        URI newUri = new UriBuilderImpl().uri(uri).path("baz").build();
        assertEquals("URI is not built correctly", newUri, 
                     new URI("http://foo/bar/baz"));
        newUri = new UriBuilderImpl().uri(uri).path("baz", "/1", "/2").build();
        assertEquals("URI is not built correctly", newUri, 
                     new URI("http://foo/bar/baz/1/2"));
    }
    
    @Test
    public void testSchemeHostPortQueryFragment() throws Exception {
        URI uri;
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            uri = new URI("http://foo:1234/bar?n2=v2&n1=v1#fragment");
        } else {
            uri = new URI("http://foo:1234/bar?n1=v1&n2=v2#fragment");
        }
        URI newUri = new UriBuilderImpl().scheme("http").host("foo")
                     .port(1234).path("bar")
                     .queryParam("n1", "v1").queryParam("n2", "v2")
                     .fragment("fragment").build();
        assertEquals("URI is not built correctly", newUri, uri);
    }

}
