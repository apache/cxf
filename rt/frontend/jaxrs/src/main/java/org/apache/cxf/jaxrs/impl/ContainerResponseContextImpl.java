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
import java.lang.reflect.Method;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

public class ContainerResponseContextImpl implements ContainerResponseContext {

    private Message m;
    private Response r;
    private OperationResourceInfo ori;
    public ContainerResponseContextImpl(Response r, 
                                        Message m,
                                        OperationResourceInfo ori) {
        this.m = m;
        this.r = r;
        this.ori = ori;
    }
    
    @Override
    public Set<String> getAllowedMethods() {
        return r.getAllowedMethods();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return r.getCookies();
    }

    @Override
    public Date getDate() {
        return r.getDate();
    }

    @Override
    public Object getEntity() {
        return r.getEntity();
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        Method method = ori == null ? null : ori.getAnnotatedMethod();
        return method == null ? null : method.getAnnotations();
    }

    @Override
    public Class<?> getEntityClass() {
        return ori == null ? null : ori.getMethodToInvoke().getReturnType();
    }

    @Override
    public Type getEntityType() {
        return ori == null ? null : ori.getMethodToInvoke().getGenericReturnType();
    }
    
    @Override
    public OutputStream getEntityStream() {
        return m.get(OutputStream.class);
    }

    @Override
    public EntityTag getEntityTag() {
        return r.getEntityTag();
    }

    @Override
    public String getHeaderString(String name) {
        return r.getHeaderString(name);
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return r.getMetadata();
    }

    @Override
    public Locale getLanguage() {
        return r.getLanguage();
    }

    @Override
    public Date getLastModified() {
        return r.getLastModified();
    }

    @Override
    public int getLength() {
        return r.getLength();
    }

    @Override
    public Link getLink(String rel) {
        return r.getLink(rel);
    }

    @Override
    public Builder getLinkBuilder(String rel) {
        return r.getLinkBuilder(rel);
    }

    @Override
    public Set<Link> getLinks() {
        return r.getLinks();
    }

    @Override
    public URI getLocation() {
        return r.getLocation();
    }

    @Override
    public MediaType getMediaType() {
        return r.getMediaType();
    }

    @Override
    public int getStatus() {
        return r.getStatus();
    }

    @Override
    public StatusType getStatusInfo() {
        return r.getStatusInfo();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        //TODO: right now this view is not modifiable
        return r.getStringHeaders();
    }

    @Override
    public boolean hasEntity() {
        return r.hasEntity();
    }

    @Override
    public boolean hasLink(String rel) {
        return r.hasLink(rel);
    }

    @Override
    public void setEntityStream(OutputStream os) {
        m.put(OutputStream.class, os);

    }

    @Override
    public void setEntity(Object entity, Annotation[] anns, MediaType mt) {
        ((ResponseImpl)r).setEntity(entity);
        updateMessageResponse();
        //TODO: review this code after API gets finalized
    }
    
    @Override
    public void setStatus(int status) {
        ((ResponseImpl)r).setStatus(status);
        updateMessageResponse();
    }

    @Override
    public void setStatusInfo(StatusType status) {
        setStatus(status.getStatusCode());
    }

    private void updateMessageResponse() {
        m.put(Response.class, r);
    }
}
