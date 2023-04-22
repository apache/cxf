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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.transport.http.Headers;

@Priority(Priorities.USER - 1)
public class CacheControlClientReaderInterceptor implements ReaderInterceptor {
    private Cache<Key, Entry> cache;

    @Context
    private UriInfo uriInfo;
    private boolean cacheResponseInputStream;

    public CacheControlClientReaderInterceptor(final Cache<Key, Entry> cache) {
        this.cache = cache;
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
        Object cachedEntity = context.getProperty(CacheControlClientRequestFilter.CACHED_ENTITY_PROPERTY);
        if (cachedEntity != null) {
            if (cachedEntity instanceof BytesEntity) {
                // InputStream or byte[]
                BytesEntity bytesEntity = (BytesEntity)cachedEntity;
                byte[] bytes = bytesEntity.getEntity();
                cachedEntity = bytesEntity.isFromStream() ? new ByteArrayInputStream(bytes) : bytes;
                if (cacheResponseInputStream) {
                    InputStream is = bytesEntity.isFromStream() ? (InputStream)cachedEntity
                        : new ByteArrayInputStream((byte[])cachedEntity);
                    context.setInputStream(is);
                    return context.proceed();
                }
            }
            return cachedEntity;
        }

        if (Boolean.parseBoolean((String)context.getProperty(CacheControlClientRequestFilter.NO_CACHE_PROPERTY))) {
            // non GET HTTP method or other restriction applies
            return context.proceed();
        }
        final MultivaluedMap<String, String> responseHeaders = context.getHeaders();
        final String cacheControlHeader = responseHeaders.getFirst(HttpHeaders.CACHE_CONTROL);
        final CacheControl cacheControl = CacheControl.valueOf(cacheControlHeader);

        byte[] cachedBytes = null;
        final boolean validCacheControl = isCacheControlValid(context, cacheControl);
        if (validCacheControl && cacheResponseInputStream) {
            // if Cache-Control is set and the stream needs to be cached then do it
            cachedBytes = IOUtils.readBytesFromStream(context.getInputStream());
            context.setInputStream(new ByteArrayInputStream(cachedBytes));
        }
        // Read the stream and get the actual entity
        Object responseEntity = context.proceed();

        if (!validCacheControl) {
            return responseEntity;
        }

        Serializable ser = null;
        if (cachedBytes != null) {
            // store the cached bytes - they will be parsed again when a client cache will return them
            ser = new BytesEntity(cachedBytes, responseEntity instanceof InputStream);
        } else if (responseEntity instanceof Serializable) {
            // store the entity directly
            ser = (Serializable)responseEntity;
        } else if (responseEntity instanceof InputStream) {
            // read the stream, cache it, the cached bytes will be returned immediately
            // when a client cache will return them
            byte[] bytes = IOUtils.readBytesFromStream((InputStream)responseEntity);
            ser = new BytesEntity(bytes, true);
            responseEntity = new ByteArrayInputStream(bytes);
        }

        if (ser != null) {
            final Entry entry =
                new Entry(ser, responseHeaders,
                          computeCacheHeaders(responseHeaders), computeExpiry(cacheControl, responseHeaders));
            final URI uri = uriInfo.getRequestUri();
            final String accepts = (String)context.getProperty(CacheControlClientRequestFilter.CLIENT_ACCEPTS);
            cache.put(new Key(uri, accepts), entry);
        }
        return responseEntity;
    }

    private Map<String, String> computeCacheHeaders(final MultivaluedMap<String, String> responseHeaders) {
        final Map<String, String> cacheHeaders = new HashMap<>(2);

        final String etagHeader = responseHeaders.getFirst(HttpHeaders.ETAG);
        if (etagHeader != null) {
            cacheHeaders.put(HttpHeaders.IF_NONE_MATCH, etagHeader);
        }
        final String lastModifiedHeader = responseHeaders.getFirst(HttpHeaders.LAST_MODIFIED);
        if (lastModifiedHeader != null) {
            cacheHeaders.put(HttpHeaders.IF_MODIFIED_SINCE, lastModifiedHeader);
        }

        return cacheHeaders;
    }

    private long computeExpiry(CacheControl cacheControl, MultivaluedMap<String, String> responseHeaders) {
        // if a max-age property is set then it overrides Expires
        long expiry = cacheControl.getMaxAge();
        if (expiry == -1) {
            //TODO: Review if Expires can be supported as an alternative to Cache-Control
            String expiresHeader = responseHeaders.getFirst(HttpHeaders.EXPIRES);
            if (expiresHeader.length() > 1 && expiresHeader.startsWith("'") && expiresHeader.endsWith("'")) {
                expiresHeader = expiresHeader.substring(1, expiresHeader.length() - 1);
            }
            try {
                expiry = (Headers.getHttpDateFormat().parse(expiresHeader).getTime()
                    - System.currentTimeMillis()) / 1000L;
            } catch (final ParseException e) {
                // TODO: Revisit the possibility of supporting multiple formats
            }
        }
        return expiry;
    }

    public boolean isCacheInputStream() {
        return cacheResponseInputStream;
    }
    /**
     * Enforce the caching of the response stream.
     * This is not recommended if the client code expects Serializable data,
     * example, String or custom JAXB beans marked as Serializable,
     * which can be stored in the cache directly.
     * Use this property only if returning a cached entity does require a
     * repeated stream parsing.
     *
     * @param cacheInputStream
     */
    public void setCacheResponseInputStream(boolean cacheInputStream) {
        this.cacheResponseInputStream = cacheInputStream;
    }

    protected boolean isCacheControlValid(final ReaderInterceptorContext context,
                                          final CacheControl responseControl) {

        boolean valid =
            responseControl != null && !responseControl.isNoCache() && !responseControl.isNoStore();
        if (valid) {
            String clientHeader =
                (String)context.getProperty(CacheControlClientRequestFilter.CLIENT_CACHE_CONTROL);
            CacheControl clientControl = clientHeader == null ? null : CacheControl.valueOf(clientHeader);
            if (clientControl != null && clientControl.isPrivate() != responseControl.isPrivate()) {
                valid = false;
            }
        }
        return valid;
    }
}
