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
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;


public final class ResponseBuilderImpl extends ResponseBuilder {
    private int status = 200;
    private Object entity;
    private MultivaluedMap<String, Object> metadata = new MetadataMap<String, Object>();

    public ResponseBuilderImpl() {
    }

    private ResponseBuilderImpl(ResponseBuilderImpl copy) {
        status = copy.status;
        metadata.putAll(copy.metadata);
        entity = copy.entity;
    }
       
    public Response build() {
        ResponseImpl r = new ResponseImpl(status, entity);
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putAll(metadata);
        r.addMetadata(m);
        reset();
        return r;
    }

    public ResponseBuilder status(int s) {
        status = s;
        return this;
    }

    public ResponseBuilder entity(Object e) {
        entity = e;
        return this;
    }

    public ResponseBuilder type(MediaType type) {
        return type(type.toString());
    }

    public ResponseBuilder type(String type) {
        metadata.putSingle("Content-Type", type);
        return this;
    }

    public ResponseBuilder language(String language) {
        metadata.putSingle("Content-Language", language.toString());
        return this;
    }

    public ResponseBuilder location(URI location) {
        metadata.putSingle("Location", location.toString());
        return this;
    }

    public ResponseBuilder contentLocation(URI location) {
        metadata.putSingle("Content-Location", location.toString());
        return this;
    }

    public ResponseBuilder tag(EntityTag tag) {
        return tag(tag.toString());
    }

    public ResponseBuilder tag(String tag) {
        metadata.putSingle("ETag", tag.toString());
        return this;
    }

    public ResponseBuilder lastModified(Date lastModified) {
        metadata.putSingle("Last-Modified", lastModified.toString());
        return this;
    }

    public ResponseBuilder cacheControl(CacheControl cacheControl) {
        metadata.putSingle("Cache-Control", cacheControl.toString());
        return this;
    }

    public ResponseBuilder cookie(NewCookie cookie) {
        metadata.putSingle("Set-Cookie", cookie.toString());
        return this;
    }
    
    @Override
    public ResponseBuilder cookie(NewCookie... cookies) {
        for (NewCookie cookie : cookies) {
            metadata.add("Set-Cookie", cookie.toString());
        }
        return this;
    }
    
    public ResponseBuilder header(String name, Object value) {
        metadata.add(name, value.toString());
        return this;
    }

    
    @Override
    public ResponseBuilder variant(Variant variant) {
        if (variant.getMediaType() != null) {
            type(variant.getMediaType());
        }
        if (variant.getLanguage() != null) {
            language(variant.getLanguage());
        }
        if (variant.getEncoding() != null) {
            metadata.putSingle("Content-Encoding", variant.getEncoding());
        }
        return this;
    }


    @Override
    public ResponseBuilder variants(List<Variant> variants) {
        throw new UnsupportedOperationException("Only a single variant option is supported");
    }
    
//  CHECKSTYLE:OFF
    @Override
    public ResponseBuilder clone() {
        return new ResponseBuilderImpl(this);
    }
//  CHECKSTYLE:ON

    
    private void reset() {
        metadata.clear();
        entity = null;
        status = 200;
    }

    
}
