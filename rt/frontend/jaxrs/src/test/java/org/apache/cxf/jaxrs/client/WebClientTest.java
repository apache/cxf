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

package org.apache.cxf.jaxrs.client;

import java.net.URI;

import org.apache.cxf.jaxrs.resources.BookInterface;
import org.apache.cxf.jaxrs.resources.BookStore;

import org.junit.Assert;
import org.junit.Test;


public class WebClientTest extends Assert {

    @Test
    public void testEncoding() {
        URI u = WebClient.create("http://foo").path("bar+ %2B").matrix("a", "value+ ")
            .query("b", "bv+ ").getCurrentURI();
        assertEquals("http://foo/bar+%20%2B;a=value+%20?b=bv%2B+", u.toString());
    }
    
    @Test
    public void testExistingAsteriscs() {
        URI u = WebClient.create("http://foo/*").getCurrentURI();
        assertEquals("http://foo/*", u.toString());
    }
    
    @Test
    public void testAsteriscs() {
        URI u = WebClient.create("http://foo").path("*").getCurrentURI();
        assertEquals("http://foo/*", u.toString());
    }
    
    @Test
    public void testDoubleAsteriscs() {
        URI u = WebClient.create("http://foo").path("**").getCurrentURI();
        assertEquals("http://foo/**", u.toString());
    }
    
    @Test 
    public void testBaseCurrentPath() {
        assertEquals(URI.create("http://foo"), WebClient.create("http://foo").getBaseURI());
        assertEquals(URI.create("http://foo"), WebClient.create("http://foo").getCurrentURI());
    }
    
    @Test 
    public void testNewBaseCurrentPath() {
        WebClient wc = WebClient.create("http://foo");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
        wc.to("http://bar", false);
        assertEquals(URI.create("http://bar"), wc.getBaseURI());
        assertEquals(URI.create("http://bar"), wc.getCurrentURI());
    }
    
    @Test 
    public void testForward() {
        WebClient wc = WebClient.create("http://foo");
        wc.to("http://foo/bar", true);
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar"), wc.getCurrentURI());
    }
    
    @Test 
    public void testCompositePath() {
        WebClient wc = WebClient.create("http://foo");
        wc.path("/bar/baz/");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz/"), wc.getCurrentURI());
    }
    
    
    @Test(expected = IllegalArgumentException.class) 
    public void testWrongForward() {
        WebClient wc = WebClient.create("http://foo");
        wc.to("http://bar", true);
    }
    
    @Test 
    public void testBaseCurrentPathAfterChange() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").matrix("m1", "m1value").query("q1", "q1value");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz;m1=m1value?q1=q1value"), wc.getCurrentURI());
    }
    
    
    @Test 
    public void testBaseCurrentPathAfterCopy() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").matrix("m1", "m1value").query("q1", "q1value");
        WebClient wc1 = WebClient.fromClient(wc);
        assertEquals(URI.create("http://foo/bar/baz;m1=m1value?q1=q1value"), wc1.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz;m1=m1value?q1=q1value"), wc1.getCurrentURI());
    }
    
    @Test 
    public void testHeaders() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.header("h1", "h1value").header("h2", "h2value");
        assertEquals(1, wc.getHeaders().get("h1").size());
        assertEquals("h1value", wc.getHeaders().getFirst("h1"));
        assertEquals(1, wc.getHeaders().get("h2").size());
        assertEquals("h2value", wc.getHeaders().getFirst("h2"));
        wc.getHeaders().clear();
        assertEquals(1, wc.getHeaders().get("h1").size());
        assertEquals("h1value", wc.getHeaders().getFirst("h1"));
        assertEquals(1, wc.getHeaders().get("h2").size());
        assertEquals("h2value", wc.getHeaders().getFirst("h2"));
        wc.reset();
        assertTrue(wc.getHeaders().isEmpty());
    }
    
    @Test
    public void testBackFast() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").matrix("m1", "m1value");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz;m1=m1value"), wc.getCurrentURI());
        wc.back(true);
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
    }
    
    @Test
    public void testBack() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz"), wc.getCurrentURI());
        wc.back(false);
        assertEquals(URI.create("http://foo/bar"), wc.getCurrentURI());
        wc.back(false);
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
        wc.back(false);
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
    }
    
    @Test
    public void testResetQueryAndBack() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").query("foo", "bar");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz?foo=bar"), wc.getCurrentURI());
        wc.resetQuery().back(false);
        assertEquals(URI.create("http://foo/bar"), wc.getCurrentURI());
    }
    
    @Test
    public void testFragment() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        wc.path("bar").path("baz").query("foo", "bar").fragment("1");
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/bar/baz?foo=bar#1"), wc.getCurrentURI());
    }
    
    
    @Test
    public void testPathWithTemplates() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo"), wc.getCurrentURI());
        
        wc.path("{bar}/{foo}", 1, 2);
        assertEquals(URI.create("http://foo"), wc.getBaseURI());
        assertEquals(URI.create("http://foo/1/2"), wc.getCurrentURI());
    }
    
    @Test
    public void testWebClientConfiguration() {
        WebClient wc = WebClient.create(URI.create("http://foo"));
        assertNotNull(WebClient.getConfig(wc) != null);
    }
    
    @Test
    public void testProxyConfiguration() {
        // interface
        BookInterface proxy = JAXRSClientFactory.create("http://foo", BookInterface.class);
        assertNotNull(WebClient.getConfig(proxy) != null);
        // cglib
        BookStore proxy2 = JAXRSClientFactory.create("http://foo", BookStore.class);
        assertNotNull(WebClient.getConfig(proxy2) != null);
    }
    
}
