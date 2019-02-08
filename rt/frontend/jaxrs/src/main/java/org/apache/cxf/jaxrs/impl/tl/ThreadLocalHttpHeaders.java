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


package org.apache.cxf.jaxrs.impl.tl;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public class ThreadLocalHttpHeaders extends AbstractThreadLocalProxy<HttpHeaders>
    implements HttpHeaders {

    public List<MediaType> getAcceptableMediaTypes() {
        return get().getAcceptableMediaTypes();
    }

    public Map<String, Cookie> getCookies() {
        return get().getCookies();
    }

    public Locale getLanguage() {
        return get().getLanguage();
    }

    public MediaType getMediaType() {
        return get().getMediaType();
    }

    public MultivaluedMap<String, String> getRequestHeaders() {
        return get().getRequestHeaders();
    }

    public List<Locale> getAcceptableLanguages() {
        return get().getAcceptableLanguages();
    }

    public List<String> getRequestHeader(String name) {
        return get().getRequestHeader(name);
    }

    public Date getDate() {
        return get().getDate();
    }

    public String getHeaderString(String name) {
        return get().getHeaderString(name);
    }

    public int getLength() {
        return get().getLength();
    }

}
