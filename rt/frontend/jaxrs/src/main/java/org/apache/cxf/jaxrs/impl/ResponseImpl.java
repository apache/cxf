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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public final class ResponseImpl extends Response {
    private final int status;
    private final Object entity;
    private MultivaluedMap<String, Object> metadata;
    
    
    ResponseImpl(int s, Object e) {
        this.status = s;
        this.entity = e;
    }

    public Object getEntity() {
        return entity;
    }

    public int getStatus() {
        return status;
    }

    void addMetadata(MultivaluedMap<String, Object> meta) { 
        this.metadata = meta;
    }
    
    public MultivaluedMap<String, Object> getMetadata() {
        // don't worry about cloning for now
        return metadata;
    }

    @Override
    public boolean bufferEntity() throws MessageProcessingException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() throws MessageProcessingException {
        // TODO Auto-generated method stub
        
    }

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
    public EntityTag getEntityTag() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHeader(String arg0) {
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
    public StatusType getStatusInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasEntity() {
        return getEntity() != null;
    }

    @Override
    public boolean hasLink(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T readEntity(Class<T> arg0) throws MessageProcessingException, IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> arg0) throws MessageProcessingException, IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(Class<T> arg0, Annotation[] arg1) throws MessageProcessingException,
        IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> arg0, Annotation[] arg1) throws MessageProcessingException,
        IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
