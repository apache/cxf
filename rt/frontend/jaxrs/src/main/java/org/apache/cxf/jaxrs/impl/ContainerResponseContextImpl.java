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

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.StatusType;

public class ContainerResponseContextImpl implements ContainerResponseContext {

    @Override
    public Set<String> getAllowedMethods() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getEntity() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?> getEntityClass() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OutputStream getEntityStream() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EntityTag getEntityTag() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Type getEntityType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHeaderString(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Locale getLanguage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getLastModified() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Link getLink(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Builder getLinkBuilder(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Link> getLinks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URI getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getStatus() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public StatusType getStatusInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasEntity() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLink(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setEntity(Object arg0, Annotation[] arg1, MediaType arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEntityStream(OutputStream arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStatus(int arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStatusInfo(StatusType arg0) {
        // TODO Auto-generated method stub

    }

}
