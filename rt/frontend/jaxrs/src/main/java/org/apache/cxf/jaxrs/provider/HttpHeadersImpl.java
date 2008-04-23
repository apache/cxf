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

package org.apache.cxf.jaxrs.provider;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.JAXRSUtils;
import org.apache.cxf.jaxrs.MetadataMap;
import org.apache.cxf.message.Message;

public class HttpHeadersImpl implements HttpHeaders {

    private Message m;
    private MultivaluedMap<String, String> headers;
    
    @SuppressWarnings("unchecked")
    public HttpHeadersImpl(Message message) {
        this.m = message;
        this.headers = new MetadataMap<String, String>(
            (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS));
    }
    
    public List<MediaType> getAcceptableMediaTypes() {
        return JAXRSUtils.parseMediaTypes((String)m.get(Message.ACCEPT_CONTENT_TYPE));
    }

    public Map<String, Cookie> getCookies() {
        List<String> cs = headers.get("Cookie");
        Map<String, Cookie> cl = new HashMap<String, Cookie>(); 
        for (String c : cs) {
            Cookie cookie = Cookie.parse(c);
            cl.put(cookie.getName(), cookie);
        }
        return cl;
    }

    public String getLanguage() {
        String l = headers.getFirst("Content-Language");
        return l == null ? "UTF-8" : l;
    }

    public MediaType getMediaType() {
        return MediaType.parse((String)m.get(Message.CONTENT_TYPE));
    }

    public MultivaluedMap<String, String> getRequestHeaders() {
        // should we really worry about immutability given that the Message does not ?
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putAll(headers);
        return map;
    }

}
