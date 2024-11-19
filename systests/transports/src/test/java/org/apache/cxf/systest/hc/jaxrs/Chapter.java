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

package org.apache.cxf.systest.hc.jaxrs;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "Chapter")
public class Chapter {
    private String title;
    private long id;

    public Chapter() {
    }

    public void setTitle(String n) {
        title = n;
    }

    public String getTitle() {
        return title;
    }

    public void setId(long i) {
        id = i;
    }
    public long getId() {
        return id;
    }

    @GET
    @Path("/recurse")
    @Produces("application/xml")
    public Chapter getItself() {
        return this;
    }

    @Path("/recurse2")
    public Chapter getItself2() {
        return this;
    }

    @GET
    @Produces("application/xml;charset=ISO-8859-1")
    public Chapter get() {
        return this;
    }

    @GET
    @Path("/ids")
    @Produces("application/xml;charset=ISO-8859-1")
    public Chapter getWithBookId(@PathParam("bookId") int bookId,
                                 @PathParam("chapterid") int chapterId) {
        if (bookId != 123 || chapterId != 1) {
            throw new RuntimeException();
        }
        return this;
    }


    @GET
    @Path("/matched-resources")
    @Produces("text/plain")
    public String getMatchedResources(@Context UriInfo ui) {
        List<String> list = new ArrayList<>();
        for (Object obj : ui.getMatchedResources()) {
            list.add(obj.toString());
        }
        return list.toString();
    }

    @GET
    @Path("/matched!uris")
    @Produces("text/plain")
    public String getMatchedUris(@Context UriInfo ui,
                                 @QueryParam("decode") String decode) {
        return ui.getMatchedURIs(Boolean.parseBoolean(decode)).toString();
    }
}
