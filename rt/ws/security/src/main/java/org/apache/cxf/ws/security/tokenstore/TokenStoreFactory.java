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

package org.apache.cxf.ws.security.tokenstore;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.tokenstore.jcache.JCacheTokenStoreFactory;
import org.apache.cxf.ws.security.utils.JCacheUtils;
import org.apache.wss4j.common.cache.WSS4JCacheUtil;

/**
 * An abstract factory to return a TokenStore instance. It returns an EHCacheTokenStoreFactory
 * if EH-Cache is available. Otherwise it returns a MemoryTokenStoreFactory.
 */
public abstract class TokenStoreFactory {
    public static TokenStoreFactory newInstance() {
        if (WSS4JCacheUtil.isEhCacheInstalled()) {
            return new EHCacheTokenStoreFactory();
        } else if (JCacheUtils.isJCacheInstalled()) {
            return new JCacheTokenStoreFactory();
        } else {
            return new MemoryTokenStoreFactory();
        }
    }

    public abstract TokenStore newTokenStore(String key, Message message) throws TokenStoreException;

}
