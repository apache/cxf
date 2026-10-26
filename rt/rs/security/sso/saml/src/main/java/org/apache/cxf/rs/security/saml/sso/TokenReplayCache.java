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

package org.apache.cxf.rs.security.saml.sso;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;

public interface TokenReplayCache<T> extends Closeable {

    boolean contains(T id);

    void putId(T id);

    void putId(T id, Instant expiry);

    /**
     * Add the given identifier to the cache if, and only if, it is not already contained in it, as a
     * single atomic operation, so that concurrent requests carrying the same identifier cannot all
     * pass a replay check before any of them has cached it. This default implementation only
     * serializes calls within a single JVM; an implementation backed by a distributed cache should
     * override it with the atomic put-if-absent operation of the underlying cache.
     * @param id The identifier to be added
     * @param expiry A custom expiry time for the identifier. Can be null in which case, the default
     *               expiry is used.
     * @return true if the identifier was added, false if it was already contained in the cache or is
     *         null or empty
     */
    default boolean putIdIfAbsent(T id, Instant expiry) {
        if (id == null || "".equals(id)) {
            return false;
        }
        synchronized (this) {
            if (contains(id)) {
                return false;
            }
            putId(id, expiry);
            return true;
        }
    }

    void close() throws IOException;
}