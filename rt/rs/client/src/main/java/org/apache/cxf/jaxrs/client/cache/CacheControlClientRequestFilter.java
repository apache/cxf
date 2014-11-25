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

import javax.annotation.Priority;
import javax.cache.Cache;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@ClientCache
@Priority(Priorities.USER - 1)
public class CacheControlClientRequestFilter implements ClientRequestFilter {
    private Cache<Key, Entry> cache;

    public CacheControlClientRequestFilter(final Cache<Key, Entry> cache) {
        setCache(cache);
    }

    public CacheControlClientRequestFilter() {
        // no-op: use setCache then
    }

    @Override
    public void filter(final ClientRequestContext request) throws IOException {
        if (!"GET".equals(request.getMethod())) {
            request.setProperty("no_client_cache", "true");
            return;
        }
        final URI uri = request.getUri();
        final String accepts = request.getHeaderString(HttpHeaders.ACCEPT);
        final Key key = new Key(uri, accepts);
        Entry entry = cache.get(key);
        if (entry != null) {
            if (entry.isOutDated()) {
                cache.remove(key, entry);
            } else {
                Response.ResponseBuilder ok = Response.ok(new String(entry.getData()));
                if (entry.getHeaders() != null) {
                    for (Map.Entry<String, List<String>> h : entry.getHeaders().entrySet()) {
                        for (final Object instance : h.getValue()) {
                            ok = ok.header(h.getKey(), instance);
                        }
                    }
                }
                request.abortWith(ok.build());
            }
        }
    }

    public CacheControlClientRequestFilter setCache(final Cache<Key, Entry> c) {
        this.cache = c;
        return this;
    }
}
