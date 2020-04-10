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
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;

/**
 * Some common functionality
 */
public final class TokenStoreUtils {

    private TokenStoreUtils() {
        // complete
    }

    public static TokenStore getTokenStore(Message message) throws TokenStoreException {
        EndpointInfo info = message.getExchange().getEndpoint().getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore =
                (TokenStore)message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            if (tokenStore == null) {
                TokenStoreFactory tokenStoreFactory = TokenStoreFactory.newInstance();
                StringBuilder cacheKey = new StringBuilder(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
                String cacheIdentifier =
                    (String)message.getContextualProperty(SecurityConstants.CACHE_IDENTIFIER);
                if (cacheIdentifier != null) {
                    cacheKey.append('-').append(cacheIdentifier);
                }
                if (info.getName() != null) {
                    int hashcode = info.getName().toString().hashCode();
                    if (hashcode >= 0) {
                        cacheKey.append('-');
                    }
                    cacheKey.append(hashcode);
                }

                tokenStore = tokenStoreFactory.newTokenStore(cacheKey.toString(), message);
                info.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);
            }
            return tokenStore;
        }
    }
}
