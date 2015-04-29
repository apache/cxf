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

import java.io.ByteArrayInputStream;
import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

public class LinkBuilderImplTest extends Assert {
    

    @Test
    public void testBuild() throws Exception {
        Link.Builder linkBuilder = new LinkBuilderImpl();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        assertEquals("<http://example.com/page1>;rel=\"previous\"", prevLink.toString());
    }
    
    @Test
    public void testbBuildObjects() throws Exception {
        StringBuilder path1 = new StringBuilder().append("p1");
        ByteArrayInputStream path2 = new ByteArrayInputStream("p2".getBytes()) {
            @Override
            public String toString() {
                return "p2";
            }
        };
        URI path3 = new URI("p3");
        
        String expected = "<" + "http://host.com:888/" + "p1/p2/p3" + ">";
        Link.Builder builder = Link.fromUri("http://host.com:888/" + "{x1}/{x2}/{x3}");
        Link link = builder.build(path1, path2, path3);
        assertNotNull(link);
        assertEquals(link.toString(), expected);
    }
    
    @Test
    public void testBuildManyRels() throws Exception {
        Link.Builder linkBuilder = new LinkBuilderImpl();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("1").rel("2").build();
        assertEquals("<http://example.com/page1>;rel=\"1 2\"", prevLink.toString());
    }

    @Test
    public void testBuildRelativized() throws Exception {
        
        Link.Builder linkBuilder = new LinkBuilderImpl();
        URI base = URI.create("http://example.com/page2");
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").buildRelativized(base);
        assertEquals("<page1>;rel=\"previous\"", prevLink.toString());
    }
    
    @Test
    public void testRelativeLink() throws Exception {
        Link.Builder linkBuilder = Link.fromUri("relative");
        linkBuilder.baseUri("http://localhost:8080/base/path");
        Link link = linkBuilder.rel("next").build();
        assertEquals("<http://localhost:8080/base/relative>;rel=\"next\"", link.toString());
    }
    
    @Test
    public void testRelativeLink2() throws Exception {
        Link.Builder linkBuilder = Link.fromUri("/relative");
        linkBuilder.baseUri("http://localhost:8080/base/path");
        Link link = linkBuilder.rel("next").build();
        assertEquals("<http://localhost:8080/relative>;rel=\"next\"", link.toString());
    }
    
    @Test
    public void testSeveralAttributes() throws Exception {
        Link.Builder linkBuilder = new LinkBuilderImpl();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").title("A title").build();
        assertEquals("<http://example.com/page1>;rel=\"previous\";title=\"A title\"", prevLink.toString());
    }
    
    @Test
    public void testCreateFromMethod() throws Exception {
        Link.Builder linkBuilder = Link.fromMethod(TestResource.class, "consumesAppJson");
        Link link = linkBuilder.build();
        String resource = link.toString();
        assertTrue(resource.contains("<consumesappjson>"));
    }
    
    @Path("resource")
    public static class TestResource {
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Path("consumesappjson")
        public String consumesAppJson() {
            return MediaType.APPLICATION_JSON;
        }
    }
}
