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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

public class Entry implements Serializable {
    private static final long serialVersionUID = -3551501551331222546L;
    private Map<String, String> cacheHeaders = Collections.emptyMap();
    private Serializable data;
    private Map<String, List<String>> headers;
    private long expiresValue;
    private long initialTimestamp = now();

    public Entry(final Serializable data, final MultivaluedMap<String, String> headers,
                 final Map<String, String> cacheHeaders, final long expiresHeaderValue) {
        this.data = data;
        initHeaders(headers);
        this.cacheHeaders = cacheHeaders;
        this.expiresValue = expiresHeaderValue;
    }

    public Entry() {
        // no-op
    }

    public boolean isOutDated() {
        return now() - initialTimestamp > expiresValue * 1000L;
    }

    public Map<String, String> getCacheHeaders() {
        return cacheHeaders;
    }

    public void setCacheHeaders(final Map<String, String> cacheHeaders) {
        this.cacheHeaders = cacheHeaders;
    }

    public Serializable getData() {
        return data;
    }

    public void setData(final Serializable data) {
        this.data = data;
    }

    public MultivaluedMap<String, String> getHeaders() {
        final MultivaluedHashMap<String, String> toReturn = new MultivaluedHashMap<String, String>();
        toReturn.putAll(headers);
        return toReturn;
    }

    private void initHeaders(final  MultivaluedMap<String, String> mHeaders) {
        this.headers = new HashMap<String, List<String>>();
        headers.putAll(mHeaders);
    }

    public void setHeaders(final MultivaluedMap<String, String> headers) {
        initHeaders(headers);
    }

    public long getExpiresValue() {
        return expiresValue;
    }

    public void setExpiresValue(final long expiresValue) {
        this.expiresValue = expiresValue;
    }

    public long getInitialTimestamp() {
        return initialTimestamp;
    }

    public void setInitialTimestamp(final long initialTimestamp) {
        this.initialTimestamp = initialTimestamp;
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
