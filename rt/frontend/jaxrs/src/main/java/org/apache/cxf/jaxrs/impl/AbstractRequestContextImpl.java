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

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public abstract class AbstractRequestContextImpl extends AbstractPropertiesImpl {

    protected HttpHeaders h;
    private boolean responseContext;
    public AbstractRequestContextImpl(Message message, boolean responseContext) {
        super(message);
        this.h = new HttpHeadersImpl(message);
        this.responseContext = responseContext;
    }

    public void abortWith(Response response) {
        checkContext();
        m.getExchange().put(Response.class, JAXRSUtils.copyResponseIfNeeded(response));
    }

    public List<Locale> getAcceptableLanguages() {
        return getHttpHeaders().getAcceptableLanguages();
    }

    public List<MediaType> getAcceptableMediaTypes() {
        return getHttpHeaders().getAcceptableMediaTypes();
    }

    public Map<String, Cookie> getCookies() {
        return getHttpHeaders().getCookies();
    }

    public Date getDate() {
        return getHttpHeaders().getDate();
    }

    public String getHeaderString(String name) {
        return getHttpHeaders().getHeaderString(name);
    }

    public Locale getLanguage() {
        return getHttpHeaders().getLanguage();
    }

    public int getLength() {
        return getHttpHeaders().getLength();
    }

    public MediaType getMediaType() {
        return getHttpHeaders().getMediaType();
    }

    public String getMethod() {
        return HttpUtils.getProtocolHeader(m, Message.HTTP_REQUEST_METHOD, null);
    }

    public void setMethod(String method) throws IllegalStateException {
        checkContext();
        m.put(Message.HTTP_REQUEST_METHOD, method);

    }

    protected HttpHeaders getHttpHeaders() {
        return h != null ? h : new HttpHeadersImpl(m);
    }

    protected void checkContext() {
        if (responseContext) {
            throw new IllegalStateException();
        }
    }
}
