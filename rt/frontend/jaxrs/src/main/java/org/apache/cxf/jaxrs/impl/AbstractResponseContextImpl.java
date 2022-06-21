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

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response.StatusType;
import org.apache.cxf.message.Message;


public abstract class AbstractResponseContextImpl {

    protected Message m;
    protected ResponseImpl r;
    public AbstractResponseContextImpl(ResponseImpl r, Message m) {
        this.m = m;
        this.r = r;
    }

    public Set<String> getAllowedMethods() {
        return r.getAllowedMethods();
    }

    public Map<String, NewCookie> getCookies() {
        return r.getCookies();
    }

    public Date getDate() {
        return r.getDate();
    }

    public Object getEntity() {
        return r.getEntity();
    }


    public EntityTag getEntityTag() {
        return r.getEntityTag();
    }

    public String getHeaderString(String name) {
        return r.getHeaderString(name);
    }

    public Locale getLanguage() {
        return r.getLanguage();
    }

    public Date getLastModified() {
        return r.getLastModified();
    }

    public int getLength() {
        return r.getLength();
    }

    public Link getLink(String rel) {
        return r.getLink(rel);
    }

    public Builder getLinkBuilder(String rel) {
        return r.getLinkBuilder(rel);
    }

    public Set<Link> getLinks() {
        return r.getLinks();
    }

    public URI getLocation() {
        return r.getLocation();
    }

    public MediaType getMediaType() {
        return r.getMediaType();
    }

    public int getStatus() {
        return r.getStatus();
    }

    public StatusType getStatusInfo() {
        return r.getStatusInfo();
    }

    public MultivaluedMap<String, String> getStringHeaders() {
        return r.getStringHeaders();
    }

    public boolean hasEntity() {
        return r.hasEntity();
    }

    public boolean hasLink(String rel) {
        return r.hasLink(rel);
    }

    public void setEntity(Object entity, Annotation[] anns, MediaType mt) {
        r.setEntity(entity, anns);
        if (mt != null) {
            r.getMetadata().putSingle(HttpHeaders.CONTENT_TYPE, mt);
            m.put(Message.CONTENT_TYPE, mt.toString());
        }
    }

    public void setEntity(Object entity) {
        r.setEntity(entity, getResponseEntityAnnotations());
    }

    protected Annotation[] getResponseEntityAnnotations() {
        return r.getEntityAnnotations();
    }


    public void setStatus(int status) {
        m.getExchange().put(Message.RESPONSE_CODE, status);
        m.put(Message.RESPONSE_CODE, status);
        r.setStatus(status);
    }

    public void setStatusInfo(StatusType status) {
        setStatus(status.getStatusCode());
    }
}
