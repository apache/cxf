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
package org.apache.cxf.systest.jaxrs;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

public class CustomResponse extends Response {
    private Response r;
    public CustomResponse(Response r) {
        this.r = r;
    }
    @Override
    public boolean bufferEntity() {
        return false;
    }
    @Override
    public void close() {

    }
    @Override
    public Set<String> getAllowedMethods() {
        return null;
    }
    @Override
    public Map<String, NewCookie> getCookies() {
        return null;
    }
    @Override
    public Date getDate() {
        return null;
    }
    @Override
    public Object getEntity() {
        return r.getEntity();
    }
    @Override
    public EntityTag getEntityTag() {
        return null;
    }
    @Override
    public String getHeaderString(String arg0) {
        return null;
    }
    @Override
    public Locale getLanguage() {
        return null;
    }
    @Override
    public Date getLastModified() {
        return null;
    }
    @Override
    public int getLength() {
        return 0;
    }
    @Override
    public Link getLink(String arg0) {
        return null;
    }
    @Override
    public Builder getLinkBuilder(String arg0) {
        return null;
    }
    @Override
    public Set<Link> getLinks() {
        return null;
    }
    @Override
    public URI getLocation() {
        return null;
    }
    @Override
    public MediaType getMediaType() {
        return null;
    }
    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return r.getMetadata();
    }
    @Override
    public int getStatus() {
        return r.getStatus();
    }
    @Override
    public StatusType getStatusInfo() {
        return null;
    }
    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return null;
    }
    @Override
    public boolean hasEntity() {
        return false;
    }
    @Override
    public boolean hasLink(String arg0) {
        return false;
    }
    @Override
    public <T> T readEntity(Class<T> arg0) {
        return null;
    }
    @Override
    public <T> T readEntity(GenericType<T> arg0) {
        return null;
    }
    @Override
    public <T> T readEntity(Class<T> arg0, Annotation[] arg1) {
        return null;
    }
    @Override
    public <T> T readEntity(GenericType<T> arg0, Annotation[] arg1) {
        return null;
    }
}
