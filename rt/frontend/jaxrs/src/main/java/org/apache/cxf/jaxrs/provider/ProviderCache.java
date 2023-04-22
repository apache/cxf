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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.jaxrs.model.ProviderInfo;

public class ProviderCache {
    private static final int MAX_PROVIDER_CACHE_SIZE =
        AccessController.doPrivileged(new PrivilegedAction<Integer>() {
            @Override
            public Integer run() {
                return SystemPropertyAction.getInteger("org.apache.cxf.jaxrs.max_provider_cache_size", 100);
            } }).intValue();

    private final Map<String, List<ProviderInfo<MessageBodyReader<?>>>>
        readerProviderCache = new ConcurrentHashMap<>();

    private final Map<String, List<ProviderInfo<MessageBodyWriter<?>>>>
        writerProviderCache = new ConcurrentHashMap<>();

    private boolean checkAllCandidates;
    public ProviderCache(boolean checkAllCandidates) {
        this.checkAllCandidates = checkAllCandidates;
    }

    public List<ProviderInfo<MessageBodyReader<?>>> getReaders(Class<?> type, MediaType mt) {
        if (readerProviderCache.isEmpty()) {
            return Collections.emptyList();
        }
        String key = getKey(type, mt);

        List<ProviderInfo<MessageBodyReader<?>>> list = readerProviderCache.get(key);
        return list != null ? list : Collections.emptyList();
    }
    public List<ProviderInfo<MessageBodyWriter<?>>> getWriters(Class<?> type, MediaType mt) {
        if (writerProviderCache.isEmpty()) {
            return Collections.emptyList();
        }

        String key = getKey(type, mt);

        List<ProviderInfo<MessageBodyWriter<?>>> list = writerProviderCache.get(key);
        return list != null ? list : Collections.emptyList();
    }

    public void putReaders(Class<?> type, MediaType mt, List<ProviderInfo<MessageBodyReader<?>>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        checkCacheSize(readerProviderCache);

        String key = getKey(type, mt);
        readerProviderCache.put(key, candidates);
    }

    public void putWriters(Class<?> type, MediaType mt, List<ProviderInfo<MessageBodyWriter<?>>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        checkCacheSize(writerProviderCache);

        String key = getKey(type, mt);
        writerProviderCache.put(key, candidates);
    }

    public void destroy() {
        this.readerProviderCache.clear();
        this.writerProviderCache.clear();
    }

    private String getKey(Class<?> type, MediaType mt) {
        return type.getName() + "-" + mt.toString();
    }

    private static void checkCacheSize(Map<?, ?> map) {
        final int size = map.size();
        if (size >= MAX_PROVIDER_CACHE_SIZE) {
            map.clear();
        }
    }

    public boolean isCheckAllCandidates() {
        return checkAllCandidates;
    }
}
