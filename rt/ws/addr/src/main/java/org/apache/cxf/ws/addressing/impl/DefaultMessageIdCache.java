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
package org.apache.cxf.ws.addressing.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.ws.addressing.MessageIdCache;

/**
 * An implementation that stores the IDs of received messages in memory. To bound the memory it uses, it
 * stores a digest of each message ID rather than the ID itself, and it holds at most a maximum number of IDs
 * (100000 by default), forgetting the oldest ones once it is full. A duplicate message is therefore only
 * detected while the ID of the original message is still in the cache. This implementation does not persist
 * the message IDs outside of this instance.
 */
public class DefaultMessageIdCache implements MessageIdCache {

    public static final int DEFAULT_MAX_SIZE = 100000;

    /**
     * The set of message ID digests, oldest first.
     */
    private final Map<String, Boolean> messageIdSet;

    public DefaultMessageIdCache() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * @param maxSize the maximum number of message IDs to hold
     */
    public DefaultMessageIdCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("The maximum size must be positive: " + maxSize);
        }
        this.messageIdSet = Collections.synchronizedMap(new BoundedMap(maxSize));
    }

    public boolean checkUniquenessAndCacheId(String messageId) {
        return this.messageIdSet.put(digest(messageId), Boolean.TRUE) == null;
    }

    protected Set<String> getMessageIdSet() {
        return this.messageIdSet.keySet();
    }

    private static String digest(String messageId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(messageId.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // every Java platform is required to support SHA-256
            throw new IllegalStateException(ex);
        }
    }

    /**
     * An insertion-ordered map that removes its oldest entry once it holds more than the maximum size.
     */
    private static final class BoundedMap extends LinkedHashMap<String, Boolean> {
        private static final long serialVersionUID = 1L;
        private final int maxSize;

        BoundedMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > maxSize;
        }
    }
}
