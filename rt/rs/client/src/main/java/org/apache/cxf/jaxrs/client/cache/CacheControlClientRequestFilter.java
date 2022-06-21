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
import java.util.List;
import java.util.Map;

import javax.cache.Cache;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.common.util.StringUtils;

@Priority(Priorities.USER - 1)
public class CacheControlClientRequestFilter implements ClientRequestFilter {
    static final String NO_CACHE_PROPERTY = "no_client_cache";
    static final String CACHED_ENTITY_PROPERTY = "client_cached_entity";
    static final String CLIENT_ACCEPTS = "client_accepts";
    static final String CLIENT_CACHE_CONTROL = "client_cache_control";
    private Cache<Key, Entry> cache;

    public CacheControlClientRequestFilter(final Cache<Key, Entry> cache) {
        this.cache = cache;
    }

    public CacheControlClientRequestFilter() {
        // no-op: use setCache then
    }

    @Override
    public void filter(final ClientRequestContext request) throws IOException {
        if (!HttpMethod.GET.equals(request.getMethod())) {
            //TODO: Review the possibility of supporting POST responses, example,
            //      POST create request may get a created entity representation returned
            request.setProperty(NO_CACHE_PROPERTY, "true");
            return;
        }
        final URI uri = request.getUri();
        final String accepts = request.getHeaderString(HttpHeaders.ACCEPT);
        final Key key = new Key(uri, accepts);
        Entry entry = cache.get(key);
        if (entry != null) {
            //TODO: do the extra validation against the conditional headers
            //      which may be contained in the current request
            if (entry.isOutDated()) {
                String ifNoneMatchHeader = entry.getCacheHeaders().get(HttpHeaders.IF_NONE_MATCH);
                String ifModifiedSinceHeader = entry.getCacheHeaders().get(HttpHeaders.IF_MODIFIED_SINCE);

                if (StringUtils.isEmpty(ifNoneMatchHeader) && StringUtils.isEmpty(ifModifiedSinceHeader)) {
                    cache.remove(key, entry);
                } else {
                    request.getHeaders().add(HttpHeaders.IF_NONE_MATCH, ifNoneMatchHeader);
                    request.getHeaders().add(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSinceHeader);
                    request.setProperty(CACHED_ENTITY_PROPERTY, entry.getData());
                }
            } else {
                Object cachedEntity = entry.getData();
                Response.ResponseBuilder ok = Response.ok(cachedEntity);
                if (entry.getHeaders() != null) {
                    for (Map.Entry<String, List<String>> h : entry.getHeaders().entrySet()) {
                        for (final Object instance : h.getValue()) {
                            ok = ok.header(h.getKey(), instance);
                        }
                    }
                }
                request.setProperty(CACHED_ENTITY_PROPERTY, cachedEntity);
                request.abortWith(ok.build());
            }
        }
        // Should the map of all request headers shared ?
        request.setProperty(CLIENT_ACCEPTS, accepts);
        request.setProperty(CLIENT_CACHE_CONTROL, request.getHeaderString(HttpHeaders.CACHE_CONTROL));
    }

    public CacheControlClientRequestFilter setCache(final Cache<Key, Entry> c) {
        this.cache = c;
        return this;
    }
}
