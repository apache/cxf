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
package org.apache.cxf.ws.addressing;

/**
 * Interface abstracting various ID caches for enforcement of ID uniqueness.
 */
public interface MessageIdCache {
    
    /**
     * Check {@code messageId} for uniqueness against previously
     * encountered values and cache the ID.  Note that the retention
     * policy for previously encountered values is implementation specific.
     * 
     * @param messageId the message ID to check for uniqueness and cache for
     *                  future comparison
     *
     * @return true if and only if {@code messageId} is not already in the
     *         cache
     */
    boolean checkUniquenessAndCacheId(String messageId);
}
