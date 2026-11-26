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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DefaultMessageIdCacheTest {

    @Test
    public void testDuplicateDetected() {
        DefaultMessageIdCache cache = new DefaultMessageIdCache();
        assertTrue(cache.checkUniquenessAndCacheId("urn:uuid:1"));
        assertTrue(cache.checkUniquenessAndCacheId("urn:uuid:2"));
        assertFalse(cache.checkUniquenessAndCacheId("urn:uuid:1"));
        assertFalse(cache.checkUniquenessAndCacheId("urn:uuid:2"));
        assertEquals(2, cache.getMessageIdSet().size());
    }

    @Test
    public void testMaxSize() {
        DefaultMessageIdCache cache = new DefaultMessageIdCache(3);
        for (int i = 1; i <= 5; i++) {
            assertTrue(cache.checkUniquenessAndCacheId("urn:uuid:" + i));
        }
        assertEquals(3, cache.getMessageIdSet().size());

        // the most recent IDs are still detected as duplicates
        assertFalse(cache.checkUniquenessAndCacheId("urn:uuid:5"));
        assertFalse(cache.checkUniquenessAndCacheId("urn:uuid:3"));
        // the oldest ones have been forgotten
        assertTrue(cache.checkUniquenessAndCacheId("urn:uuid:1"));
        assertEquals(3, cache.getMessageIdSet().size());
    }

    @Test
    public void testLongMessageIdNotStored() {
        // the cache keeps a fixed-size digest of each ID, however long the ID is
        String longId = "urn:uuid:" + "x".repeat(10 * 1024 * 1024);
        DefaultMessageIdCache cache = new DefaultMessageIdCache();
        assertTrue(cache.checkUniquenessAndCacheId(longId));
        assertFalse(cache.checkUniquenessAndCacheId(longId));
        assertTrue(cache.getMessageIdSet().iterator().next().length() < 100);
    }

    @Test
    public void testInvalidMaxSize() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultMessageIdCache(0));
    }

    @Test
    public void testTruncateMessageIdForDisplay() {
        // a duplicate message ID is logged and returned in the fault, so a huge one must be shortened
        assertEquals("urn:uuid:1", MAPAggregatorImpl.truncateMessageId("urn:uuid:1"));
        String longId = "urn:uuid:" + "x".repeat(10 * 1024 * 1024);
        String displayed = MAPAggregatorImpl.truncateMessageId(longId);
        assertEquals(256 + "...".length(), displayed.length());
        assertTrue(displayed.startsWith("urn:uuid:xxx") && displayed.endsWith("..."));
    }
}
