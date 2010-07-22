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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation that uses a simple set to store received message IDs.
 * Note that this implementation does not make any attempt to flush older
 * message IDs or to persist the message IDs outside of this instance. 
 */
public class DefaultMessageIdCache implements MessageIdCache {
    
    /**
     * The set of message IDs.
     */
    private final Map<String, Boolean> messageIdSet = 
        new ConcurrentHashMap<String, Boolean>();  
    
    public boolean checkUniquenessAndCacheId(String messageId) {
        return this.messageIdSet.put(messageId, Boolean.TRUE) == null;
    }
    
    protected Set<String> getMessageIdSet() {
        return this.messageIdSet.keySet();
    }
}
