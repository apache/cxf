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
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;

public final class ResponseImpl extends Response {
    private final int status;
    private Object entity;
    private MultivaluedMap<String, Object> metadata;
    private boolean entityClosed;    
    
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

    public boolean bufferEntity() throws MessageProcessingException {
        if (entity instanceof InputStream) {
            if (entity instanceof ByteArrayInputStream) {
                return false;
            } else {
                try {
                    InputStream oldEntity = (InputStream)entity;
                    entity = IOUtils.loadIntoBAIS(oldEntity);
                    return true;
                } catch (IOException ex) {
                    throw new MessageProcessingException(ex);
                }
            }
        }
        return false;
    }

    public void close() throws MessageProcessingException {
        if (!entityClosed && entity instanceof InputStream
            && !(entity instanceof ByteArrayInputStream)) {
            // unbuffered entity
            try {
                ((InputStream)entity).close();
                entity = null;
                entityClosed = true;
            } catch (IOException ex) {
                throw new MessageProcessingException(ex);
            }
            
        }
        
    }

    public Set<String> getAllowedMethods() {
        List<Object> methodValues = metadata.get(HttpHeaders.ALLOW);
        if (methodValues == null) {
            return Collections.emptySet();
        } else {
            Set<String> methods = new HashSet<String>();
            for (Object o : methodValues) {
                methods.add(o.toString());
            }
            return methods;
        }
    }

    public Map<String, NewCookie> getCookies() {
        List<Object> cookieValues = metadata.get(HttpHeaders.SET_COOKIE);
        if (cookieValues == null) {
            return Collections.emptyMap();
        } else {
            Map<String, NewCookie> cookies = new HashMap<String, NewCookie>();
            for (Object o : cookieValues) {
                NewCookie newCookie = NewCookie.valueOf(o.toString());
                cookies.put(newCookie.getName(), newCookie);
            }
            return cookies;
        }
    }

    public Date getDate() {
        return doGetDate(HttpHeaders.DATE);
    }

    private Date doGetDate(String dateHeader) {
        return HttpUtils.getHttpDate(getHeader(dateHeader));
    }
    
    public EntityTag getEntityTag() {
        String header = getHeader(HttpHeaders.ETAG);
        return header == null ? null : EntityTag.valueOf(header);
    }

    public String getHeader(String header) {
        Object value = metadata.getFirst(header);
        return value == null ? null : value.toString();
    }

    public Locale getLanguage() {
        return HttpUtils.getLocale(getHeader(HttpHeaders.CONTENT_LANGUAGE));
    }

    public Date getLastModified() {
        return doGetDate(HttpHeaders.LAST_MODIFIED);
    }

    public int getLength() {
        return HttpUtils.getContentLength(getHeader(HttpHeaders.CONTENT_LENGTH));
    }

    public Link getLink(String relation) {
        // TODO Auto-generated method stub
        return null;
    }

    public Builder getLinkBuilder(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<Link> getLinks() {
        // TODO Auto-generated method stub
        return null;
    }

    public URI getLocation() {
        String header = getHeader(HttpHeaders.LOCATION);
        return header == null ? null : URI.create(header);
    }

    public MediaType getMediaType() {
        String header = getHeader(HttpHeaders.CONTENT_LENGTH);
        return header == null ? null : MediaType.valueOf(header);
    }

    public StatusType getStatusInfo() {
        final Response.Status responseStatus = Response.Status.fromStatusCode(status);
        return new Response.StatusType() {

            public Family getFamily() {
                return responseStatus.getFamily();
            }

            public String getReasonPhrase() {
                return responseStatus.getReasonPhrase();
            }

            public int getStatusCode() {
                return responseStatus.getStatusCode();
            } 
            
        };
    }

    public boolean hasEntity() {
        return getEntity() != null;
    }

    public boolean hasLink(String relation) {
        // TODO Auto-generated method stub
        return false;
    }

    public <T> T readEntity(Class<T> cls) throws MessageProcessingException, IllegalStateException {
        checkEntityIsAvailable();
        return null;
    }

    public <T> T readEntity(GenericType<T> genType) throws MessageProcessingException, IllegalStateException {
        checkEntityIsAvailable();
        return null;
    }

    public <T> T readEntity(Class<T> cls, Annotation[] anns) throws MessageProcessingException,
        IllegalStateException {
        checkEntityIsAvailable();
        return null;
    }

    public <T> T readEntity(GenericType<T> genType, Annotation[] anns) throws MessageProcessingException,
        IllegalStateException {
        checkEntityIsAvailable();
        return null;
    }
    
    private void checkEntityIsAvailable() throws MessageProcessingException {
        if (entity == null) {
            throw new MessageProcessingException("Entity is not available");
        }
    }
}
