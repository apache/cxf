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

import javax.ws.rs.core.Link;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LinkBuilderImplTest {
    

    @Test
    public void build() throws Exception {
        Link.Builder linkBuilder = new LinkBuilderImpl();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        assertEquals("<http://example.com/page1>;rel=\"previous\"", prevLink.toString());
    }

    @Ignore("Ignored due to CXF-4919")
    @Test
    public void relativeBuild() throws Exception {
        Link.Builder linkBuilder = new LinkBuilderImpl();
        URI base = URI.create("http://example.com/page2");
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").buildRelativized(base);
        assertEquals("<page1>;rel=\"previous\"", prevLink.toString());
    }

    @Test
    public void severalAttributes() throws Exception {
        Link.Builder linkBuilder = new LinkBuilderImpl();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").title("A title").build();
        assertEquals("<http://example.com/page1>;rel=\"previous\";title=\"A title\"", prevLink.toString());
    }

    @Test
    public void copyOnBuild() throws Exception {
        Link.Builder linkBuilder = new LinkBuilderImpl();
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").build();

        // Previously built link should not be affected by reuse of link builder
        assertEquals("<http://example.com/page1>;rel=\"previous\"", prevLink.toString());
        assertEquals("<http://example.com/page3>;rel=\"next\"", nextLink.toString());
    }

    @Ignore("Ignored due to CXF-4919")
    @Test
    public void copyOnRelativeBuild() throws Exception {
        Link.Builder linkBuilder = new LinkBuilderImpl();
        URI base = URI.create("http://example.com/page2");
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").buildRelativized(base);
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").buildRelativized(base);

        // Previously built link should not be affected by reuse of link builder
        assertEquals("<page1>;rel=\"previous\"", prevLink.toString());
        assertEquals("<page3>;rel=\"next\"", nextLink.toString());
    }
}
