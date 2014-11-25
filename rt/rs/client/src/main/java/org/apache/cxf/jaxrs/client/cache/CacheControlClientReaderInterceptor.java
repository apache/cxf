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
package org.apache.cxf.jaxrs.client.cache;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Priority;
import javax.cache.Cache;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.apache.cxf.transport.http.Headers;

@ClientCache
@Priority(Priorities.USER - 1)
public class CacheControlClientReaderInterceptor implements ReaderInterceptor {
    private Cache<Key, Entry> cache;

    @Context
    private UriInfo uriInfo;

    public CacheControlClientReaderInterceptor(final Cache<Key, Entry> cache) {
        setCache(cache);
    }

    public CacheControlClientReaderInterceptor() {
        // no-op: use setCache then
    }

    public CacheControlClientReaderInterceptor setCache(final Cache<Key, Entry> c) {
        this.cache = c;
        return this;
    }

    @Override
    public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
        if (Boolean.parseBoolean((String)context.getProperty("no_client_cache"))) {
            return context.proceed();
        }
        final MultivaluedMap<String, String> headers = context.getHeaders(); 
        final String cacheControlHeader = headers.getFirst(HttpHeaders.CACHE_CONTROL);
        String expiresHeader = headers.getFirst(HttpHeaders.EXPIRES);
        
        long expiry = -1;
        if (cacheControlHeader != null) {
            final CacheControl value = CacheControl.valueOf(cacheControlHeader.toString());
            if (value.isNoCache()) {
                return context.proceed();
            }
            expiry = value.getMaxAge();
        } else if (expiresHeader != null) {
            if (expiresHeader.startsWith("'") && expiresHeader.endsWith("'")) {
                expiresHeader = expiresHeader.substring(1, expiresHeader.length() - 1);
            }
            try {
                expiry = (Headers.getHttpDateFormat().parse(expiresHeader).getTime() 
                    - System.currentTimeMillis()) / 1000;
            } catch (final ParseException e) {
                // try next
            }
            
        } else { // no cache
            return context.proceed();
        }

        final Object proceed = context.proceed();
        
        final Entry entry = new Entry(((String)proceed).getBytes(), context.getHeaders(), 
                                      computeCacheHeaders(context.getHeaders()), expiry);
        final URI uri = uriInfo.getRequestUri();
        final String accepts = headers.getFirst(HttpHeaders.ACCEPT);
        cache.put(new Key(uri, accepts), entry);

        return proceed;
    }

    private Map<String, String> computeCacheHeaders(final MultivaluedMap<String, String> httpHeaders) {
        final Map<String, String> cacheHeaders = new HashMap<String, String>(2);

        final String etagHeader = httpHeaders.getFirst(HttpHeaders.ETAG);
        if (etagHeader != null) {
            cacheHeaders.put("If-None-Match", etagHeader);
        }
        final String lastModifiedHeader = httpHeaders.getFirst(HttpHeaders.LAST_MODIFIED);
        if (lastModifiedHeader != null) {
            cacheHeaders.put("If-Modified-Since", lastModifiedHeader);
        }

        return cacheHeaders;
    }
}
