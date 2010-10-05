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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

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
        MetadataMap<String, Object> m = 
            new MetadataMap<String, Object>(metadata, false, true);
        r.addMetadata(m);
        reset();
        return r;
    }

    public ResponseBuilder status(int s) {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("Illegal status value : " + s);
        }
        status = s;
        return this;
    }

    public ResponseBuilder entity(Object e) {
        entity = e;
        return this;
    }

    public ResponseBuilder type(MediaType type) {
        return type(type == null ? null : type.toString());
    }

    public ResponseBuilder type(String type) {
        return setHeader(HttpHeaders.CONTENT_TYPE, type);
    }

    @Override
    public ResponseBuilder language(Locale locale) {
        return language(locale == null ? null : locale.toString());
    }
    
    public ResponseBuilder language(String language) {
        return setHeader(HttpHeaders.CONTENT_LANGUAGE, language);
    }

    public ResponseBuilder location(URI loc) {
        if (!loc.isAbsolute()) {
            Message currentMessage = PhaseInterceptorChain.getCurrentMessage();
            if (currentMessage != null) {
                
                UriInfo ui = new UriInfoImpl(currentMessage.getExchange().getInMessage(), null);
                loc = ui.getBaseUriBuilder()
                        .path(loc.getRawPath())
                        .replaceQuery(loc.getRawQuery())
                        .fragment(loc.getRawFragment()).buildFromEncoded();
            }
        }
        return setHeader(HttpHeaders.LOCATION, loc);
    }

    public ResponseBuilder contentLocation(URI location) {
        return setHeader(HttpHeaders.CONTENT_LOCATION, location);
    }

    public ResponseBuilder tag(EntityTag tag) {
        return tag(tag == null ? null : tag.toString());
    }

    public ResponseBuilder tag(String tag) {
        return setHeader(HttpHeaders.ETAG, tag);
    }

    public ResponseBuilder lastModified(Date date) {
        return setHeader(HttpHeaders.LAST_MODIFIED, date == null ? null : toHttpDate(date));
    }

    public ResponseBuilder cacheControl(CacheControl cacheControl) {
        return setHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
    }

    @Override
    public ResponseBuilder expires(Date date) {
        return setHeader(HttpHeaders.EXPIRES, date == null ? null : toHttpDate(date));
    }

    @Override
    public ResponseBuilder cookie(NewCookie... cookies) {
        return addHeader(HttpHeaders.SET_COOKIE, (Object[])cookies);
    }
    
    public ResponseBuilder header(String name, Object value) {
        if (HttpUtils.isDateRelatedHeader(name)) {
            Object theValue = value instanceof Date ? toHttpDate((Date)value) : value;  
            return setHeader(name, theValue);
        } else {
            return addHeader(name, value);
        }
    }

    
    @Override
    public ResponseBuilder variant(Variant variant) {
        type(variant == null ? null : variant.getMediaType());
        language(variant == null ? null : variant.getLanguage());
        setHeader(HttpHeaders.CONTENT_ENCODING, variant == null ? null : variant.getEncoding());
        
        return this;
    }


    @Override
    public ResponseBuilder variants(List<Variant> variants) {
        if (variants == null) {
            metadata.remove(HttpHeaders.VARY);
            return this;
        }
        String acceptVary = null;
        String acceptLangVary = null;
        String acceptEncVary = null;
        for (Variant v : variants) {
            MediaType mt = v.getMediaType();
            if (mt != null) {
                acceptVary = HttpHeaders.ACCEPT;
                addHeader(HttpHeaders.ACCEPT, mt);
            }
            Locale l = v.getLanguage();
            if (l != null) {
                acceptLangVary = HttpHeaders.ACCEPT_LANGUAGE;
                addHeader(HttpHeaders.ACCEPT_LANGUAGE, l);
            }
            String enc = v.getEncoding();
            if (enc != null) {
                acceptEncVary = HttpHeaders.ACCEPT_ENCODING;
                addHeader(HttpHeaders.ACCEPT_ENCODING, enc);
            }
        }
        handleVaryValue(acceptVary, acceptLangVary, acceptEncVary);
        return this;
    }
    
    private void handleVaryValue(String ...values) {
        List<Object> varyValues = metadata.get(HttpHeaders.VARY);
        for (String v : values) {
            if (v == null) {
                metadata.remove(v);
                if (varyValues != null) {
                    varyValues.remove(v);
                }
            } else {
                addHeader(HttpHeaders.VARY, v);
            }
        }
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
    
    private String toHttpDate(Date date) {
        SimpleDateFormat format = HttpUtils.getHttpDateFormat();
        return format.format(date);
    }
    
    private ResponseBuilder setHeader(String name, Object value) {
        if (value == null) {
            metadata.remove(name);
        } else {
            metadata.putSingle(name, value.toString());
        }
        return this;
    }
    
    private ResponseBuilder addHeader(String name, Object... values) {
        if (values != null && values.length >= 1 && values[0] != null) {
            for (Object value : values) {
                if (!valueExists(name, value)) {
                    metadata.add(name, value.toString());
                }
            }
        } else {
            metadata.remove(name);
        }    
        return this;
    }
    
    private boolean valueExists(String key, Object value) {
        List<Object> values = metadata.get(key);
        return values == null ? false : values.contains(value.toString());
    }
}
